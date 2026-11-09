import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const svgBuffer = fs.readFileSync(path.resolve('./public/icon.svg'));

async function generate() {
  console.log('Generating PWA and Android APK icons...');

  // Standard 192x192
  await sharp(svgBuffer)
    .resize(192, 192)
    .png()
    .toFile(path.resolve('./public/pwa-192x192.png'));

  // Standard 512x512
  await sharp(svgBuffer)
    .resize(512, 512)
    .png()
    .toFile(path.resolve('./public/pwa-512x512.png'));

  // Apple Touch Icon 180x180
  await sharp(svgBuffer)
    .resize(180, 180)
    .png()
    .toFile(path.resolve('./public/apple-touch-icon.png'));

  // Maskable 512x512 with safe zone padding (Android requirement: 10-15% margin)
  await sharp(svgBuffer)
    .resize(410, 410)
    .extend({
      top: 51,
      bottom: 51,
      left: 51,
      right: 51,
      background: { r: 10, g: 10, b: 10, alpha: 1 }
    })
    .png()
    .toFile(path.resolve('./public/pwa-maskable-512x512.png'));

  console.log('All icons generated successfully!');
}

generate().catch(console.error);
