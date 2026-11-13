package com.hatch.marketplace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextDim
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary

@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }
    val imageUrl = product.thumbnailUrl?.takeIf { it.isNotBlank() } ?: product.images.firstOrNull().orEmpty()
    val hasDiscount = product.originalPrice != null && product.originalPrice > product.price
    val discountPct = if (hasDiscount && product.originalPrice!! > 0.0) {
        (((product.originalPrice - product.price) / product.originalPrice) * 100).toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HatchSurfaceDark)
            .border(
                width = 1.dp,
                color = if (product.featured) HatchAmber500.copy(alpha = 0.55f) else HatchBorderDark,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .background(HatchCardElevated)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, HatchBgDark.copy(alpha = 0.55f)), startY = 120f))
            )

            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (product.status == ProductStatus.SOLD.wireValue) {
                    BadgePill(text = if (language == AppLanguage.AR) "مباع" else "SOLD", bg = Color(0xFF334155), fg = Color.White)
                } else if (product.featured) {
                    BadgePill(text = "⭐ ${strings.featuredBadge}", bg = HatchAmber500, fg = HatchBgDark)
                }
                if (product.urgentSale) {
                    BadgePill(text = "⚡ ${strings.urgentBadge}", bg = HatchRose500, fg = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(HatchBgDark.copy(alpha = 0.7f))
                    .clickable { onToggleFavorite() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) HatchRose500 else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BadgePill(text = strings.conditionLabel(product.condition), bg = HatchBgDark.copy(alpha = 0.78f), fg = Color.White)
                if (product.paymentMethods.contains(PaymentMethod.BANKAK.wireValue)) {
                    BadgePill(text = strings.paymentLabel("bankak"), bg = HatchEmerald500.copy(alpha = 0.9f), fg = Color.White)
                }
            }
        }

        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = HatchTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = HatchFormatters.formatSdg(product.price, language), color = HatchAmber400, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (discountPct > 0) {
                    Text(text = "-$discountPct%", color = HatchEmerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (hasDiscount) {
                Text(
                    text = HatchFormatters.formatSdg(product.originalPrice!!, language),
                    color = HatchTextDim,
                    fontSize = 10.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(12.dp))
                    Text(text = product.city.substringBefore(" ("), color = HatchTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = HatchTextDim, modifier = Modifier.size(11.dp))
                    Text(text = product.viewsCount.toString(), color = HatchTextDim, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, HatchBorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (product.sellerAvatar.isNotBlank()) {
                        AsyncImage(
                            model = product.sellerAvatar,
                            contentDescription = product.sellerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(18.dp).clip(CircleShape)
                        )
                    }
                    Text(text = product.sellerName, color = HatchTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (product.sellerVerified) {
                        Icon(Icons.Rounded.Verified, contentDescription = strings.verifiedBadge, tint = HatchEmerald400, modifier = Modifier.size(12.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(11.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", product.sellerRating),
                        color = HatchTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BadgePill(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        color = fg,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
