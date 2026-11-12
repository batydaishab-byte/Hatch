/**
 * HATCH Free Media Upload Signer (Cloudflare Workers Free Tier — 100,000 req/day, NO credit card)
 *
 * Security Guarantees:
 * 1. Verifies Firebase Auth ID Token signature (RS256) against Google's public JWKs.
 * 2. Verifies Admin identity AT UPLOAD SIGNING TIME (email_verified === true && email === ADMIN_EMAIL).
 * 3. Regular users can ONLY sign compressed WebP product images (<= 1.5 MB).
 * 4. Admins ONLY can sign sponsored ad images (<= 2 MB) & videos (<= 15 MB, <= 30s).
 * 5. Keeps CLOUDINARY_API_SECRET strictly inside Cloudflare Worker encrypted Secrets.
 */

export interface Env {
  FIREBASE_PROJECT_ID: string;
  ADMIN_EMAIL: string;
  CLOUDINARY_CLOUD_NAME: string;
  CLOUDINARY_API_KEY: string;
  CLOUDINARY_API_SECRET: string;
}

interface FirebaseJwtPayload {
  iss: string;
  aud: string;
  sub: string;
  exp: number;
  iat: number;
  email?: string;
  email_verified?: boolean;
}

const GOOGLE_JWKS_URL =
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';

const LIMITS = {
  PRODUCT_IMAGE: { maxBytes: 1_572_864, resourceType: 'image', formats: 'webp', folderPrefix: 'hatch/products' },
  PRODUCT_THUMBNAIL: { maxBytes: 153_600, resourceType: 'image', formats: 'webp', folderPrefix: 'hatch/thumbnails' },
  SPONSORED_AD_IMAGE: { maxBytes: 2_097_152, resourceType: 'image', formats: 'webp', folderPrefix: 'hatch/sponsored_ads', adminOnly: true },
  SPONSORED_AD_VIDEO: { maxBytes: 15_728_640, maxDurationMs: 30_000, resourceType: 'video', formats: 'mp4,webm', folderPrefix: 'hatch/sponsored_ads', adminOnly: true },
} as const;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);
    try {
      const token = request.headers.get('Authorization')?.replace(/^Bearer\s+/i, '');
      if (!token) return jsonResponse({ error: 'Missing Authorization Bearer token' }, 401);

      const claims = await verifyFirebaseIdToken(token, env.FIREBASE_PROJECT_ID);
      const isAdmin = claims.email_verified === true && claims.email === env.ADMIN_EMAIL;

      if (url.pathname === '/sign-upload' && request.method === 'POST') {
        const body = (await request.json()) as {
          action?: keyof typeof LIMITS;
          byteSize?: number;
          durationMs?: number;
        };

        const action = body.action;
        if (!action || !(action in LIMITS)) {
          return jsonResponse({ error: 'Invalid upload action' }, 400);
        }

        const rule = LIMITS[action];
        if ('adminOnly' in rule && rule.adminOnly && !isAdmin) {
          return jsonResponse({ error: 'Forbidden: Admin verification failed at upload gate' }, 403);
        }

        if (typeof body.byteSize !== 'number' || body.byteSize <= 0 || body.byteSize > rule.maxBytes) {
          return jsonResponse({ error: `File exceeds free-tier size limit (${rule.maxBytes} bytes)` }, 400);
        }

        if ('maxDurationMs' in rule) {
          if (typeof body.durationMs !== 'number' || body.durationMs <= 0 || body.durationMs > rule.maxDurationMs) {
            return jsonResponse({ error: 'Video exceeds 30-second limit' }, 400);
          }
        }

        const timestamp = Math.floor(Date.now() / 1000);
        const folder =
          action === 'PRODUCT_IMAGE' || action === 'PRODUCT_THUMBNAIL'
            ? `${rule.folderPrefix}/${claims.sub}`
            : rule.folderPrefix;

        const paramsToSign: Record<string, string> = {
          allowed_formats: rule.formats,
          folder,
          timestamp: String(timestamp),
        };

        const signature = await signCloudinaryParams(paramsToSign, env.CLOUDINARY_API_SECRET);

        return jsonResponse({
          signature,
          timestamp,
          apiKey: env.CLOUDINARY_API_KEY,
          cloudName: env.CLOUDINARY_CLOUD_NAME,
          folder,
          resourceType: rule.resourceType,
          allowedFormats: rule.formats,
        });
      }

      if (url.pathname === '/admin/media-usage' && request.method === 'GET') {
        if (!isAdmin) {
          return jsonResponse({ error: 'Forbidden: Admin access required' }, 403);
        }
        const basicAuth = btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`);
        const usageRes = await fetch(
          `https://api.cloudinary.com/v1_1/${env.CLOUDINARY_CLOUD_NAME}/usage`,
          { headers: { Authorization: `Basic ${basicAuth}` } }
        );
        if (!usageRes.ok) {
          return jsonResponse({ error: 'Failed to inspect Cloudinary usage' }, 502);
        }
        const usage = (await usageRes.json()) as {
          credits?: { usage?: number; limit?: number; used_percent?: number };
        };
        const creditsUsed = usage.credits?.usage ?? 0;
        const creditsLimit = usage.credits?.limit ?? 25;
        const usagePercent = usage.credits?.used_percent ?? (creditsUsed / creditsLimit) * 100;

        return jsonResponse({
          creditsUsed,
          creditsLimit,
          usagePercent,
          isNearLimit: usagePercent >= 80,
          videoUploadsEnabled: true,
        });
      }

      return jsonResponse({ error: 'Not found' }, 404);
    } catch (err) {
      return jsonResponse(
        { error: err instanceof Error ? err.message : 'Unauthorized' },
        401
      );
    }
  },
};

async function verifyFirebaseIdToken(token: string, projectId: string): Promise<FirebaseJwtPayload> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Malformed JWT');
  const [headerB64, payloadB64, sigB64] = parts;

  const header = JSON.parse(decodeBase64UrlText(headerB64)) as { alg?: string; kid?: string };
  if (header.alg !== 'RS256' || !header.kid) throw new Error('Invalid JWT header');

  const payload = JSON.parse(decodeBase64UrlText(payloadB64)) as FirebaseJwtPayload;
  const nowSec = Math.floor(Date.now() / 1000);
  if (payload.aud !== projectId) throw new Error('Invalid audience');
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new Error('Invalid issuer');
  if (!payload.sub || payload.exp <= nowSec || payload.iat > nowSec + 300) {
    throw new Error('Expired or invalid token');
  }

  const jwksRes = await fetch(GOOGLE_JWKS_URL);
  const jwks = (await jwksRes.json()) as { keys: Array<JsonWebKey & { kid: string }> };
  const jwk = jwks.keys.find((k) => k.kid === header.kid);
  if (!jwk) throw new Error('Unknown signing key');

  const key = await crypto.subtle.importKey(
    'jwk',
    jwk,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['verify']
  );

  const valid = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5',
    key,
    decodeBase64UrlBytes(sigB64),
    new TextEncoder().encode(`${headerB64}.${payloadB64}`)
  );
  if (!valid) throw new Error('Invalid JWT signature');
  return payload;
}

async function signCloudinaryParams(params: Record<string, string>, apiSecret: string): Promise<string> {
  const sorted = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k]}`)
    .join('&');
  const digest = await crypto.subtle.digest(
    'SHA-1',
    new TextEncoder().encode(`${sorted}${apiSecret}`)
  );
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

function decodeBase64UrlText(input: string): string {
  return new TextDecoder().decode(decodeBase64UrlBytes(input));
}

function decodeBase64UrlBytes(input: string): Uint8Array<ArrayBuffer> {
  const normalized = input.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  const bin = atob(padded);
  const buffer = new ArrayBuffer(bin.length);
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

function corsHeaders(): Record<string, string> {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization, Content-Type',
  };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...corsHeaders() },
  });
}
