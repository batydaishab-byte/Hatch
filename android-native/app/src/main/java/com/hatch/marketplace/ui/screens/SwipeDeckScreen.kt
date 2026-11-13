package com.hatch.marketplace.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.SudaneseCities
import com.hatch.marketplace.ui.components.BadgePill
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchBorderSubtle
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeckScreen(
    products: List<Product>,
    selectedCity: String,
    canUndo: Boolean,
    isRefreshing: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectCity: (String) -> Unit,
    onRefresh: () -> Unit,
    onSwipeLeft: (Product) -> Unit,
    onSwipeRight: (Product) -> Unit,
    onSwipeUpOffer: (Product) -> Unit,
    onUndo: () -> Unit,
    onOpenDetail: (Product) -> Unit,
    onResetDeck: () -> Unit,
    onGoToBrowse: () -> Unit
) {
    val quickCities = remember { listOf("all") + SudaneseCities.ALL.take(12) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCities.forEach { city ->
                        val active = selectedCity == city
                        val label = if (city == "all") strings.allCities else city.substringBefore(" (")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) HatchAmber500 else HatchSurfaceDark)
                                .border(1.dp, if (active) HatchAmber500 else HatchBorderDark, RoundedCornerShape(50))
                                .clickable { onSelectCity(city) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) HatchBgDark else HatchTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${products.size} ${strings.remainingCards}",
                    color = HatchAmber400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(24.dp))
                            .padding(28.dp)
                    ) {
                        Icon(Icons.Rounded.Storefront, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(54.dp))
                        Text(strings.swipeEmptyTitle, color = HatchTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(strings.swipeEmptySubtitle, color = HatchTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onResetDeck,
                                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.reloadDeck, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onGoToBrowse, shape = RoundedCornerShape(12.dp)) {
                                Text(strings.browseAllAds, color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                val topProduct = products.first()
                val nextProduct = products.getOrNull(1)

                var offsetX by remember(topProduct.id) { mutableFloatStateOf(0f) }
                var offsetY by remember(topProduct.id) { mutableFloatStateOf(0f) }
                var photoIndex by remember(topProduct.id) { mutableIntStateOf(0) }

                val animatedX by animateFloatAsState(offsetX, spring(), label = "cardX")
                val animatedY by animateFloatAsState(offsetY, spring(), label = "cardY")

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (nextProduct != null) {
                        SwipeCardContent(
                            product = nextProduct,
                            photoIndex = 0,
                            language = language,
                            strings = strings,
                            modifier = Modifier.fillMaxSize().scale(0.94f).offset(y = 14.dp)
                        )
                    }

                    val photos = topProduct.images.ifEmpty { listOfNotNull(topProduct.thumbnailUrl) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                            .rotate((animatedX / 35f).coerceIn(-16f, 16f))
                            .pointerInput(topProduct.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetY < -190f -> {
                                                offsetX = 0f; offsetY = 0f
                                                onSwipeUpOffer(topProduct)
                                            }
                                            offsetX > 210f -> {
                                                offsetX = 0f; offsetY = 0f
                                                onSwipeRight(topProduct)
                                            }
                                            offsetX < -210f -> {
                                                offsetX = 0f; offsetY = 0f
                                                onSwipeLeft(topProduct)
                                            }
                                            else -> {
                                                offsetX = 0f; offsetY = 0f
                                            }
                                        }
                                    },
                                    onDragCancel = { offsetX = 0f; offsetY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY = (offsetY + dragAmount.y).coerceAtMost(80f)
                                    }
                                )
                            }
                            .pointerInput(topProduct.id, photos.size) {
                                detectTapGestures { pos ->
                                    if (photos.size > 1 && pos.y < size.height * 0.68f) {
                                        if (pos.x < size.width / 2f) {
                                            photoIndex = (photoIndex - 1 + photos.size) % photos.size
                                        } else {
                                            photoIndex = (photoIndex + 1) % photos.size
                                        }
                                    } else {
                                        onOpenDetail(topProduct)
                                    }
                                }
                            }
                    ) {
                        SwipeCardContent(
                            product = topProduct,
                            photoIndex = photoIndex,
                            language = language,
                            strings = strings,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (offsetX > 65f) {
                            SwipeStamp(strings.swipeHintLike, HatchEmerald400, Modifier.align(Alignment.TopStart).padding(22.dp))
                        } else if (offsetX < -65f) {
                            SwipeStamp(strings.swipeHintPass, HatchRose500, Modifier.align(Alignment.TopEnd).padding(22.dp))
                        } else if (offsetY < -75f && abs(offsetY) > abs(offsetX)) {
                            SwipeStamp(strings.makeOffer, HatchAmber400, Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwipeActionCircle(Icons.Rounded.Undo, if (canUndo) HatchAmber400 else HatchTextMuted.copy(alpha = 0.4f), 44.dp, canUndo, onUndo)
                        SwipeActionCircle(Icons.Rounded.Close, HatchRose500, 56.dp, true) { onSwipeLeft(topProduct) }
                        SwipeActionCircle(Icons.Rounded.LocalOffer, HatchAmber400, 52.dp, true) { onSwipeUpOffer(topProduct) }
                        SwipeActionCircle(Icons.Rounded.Favorite, HatchEmerald400, 56.dp, true) { onSwipeRight(topProduct) }
                        SwipeActionCircle(Icons.Rounded.Info, HatchTextPrimary, 44.dp, true) { onOpenDetail(topProduct) }
                    }
                    Text(strings.swipeHintOffer, color = HatchTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SwipeCardContent(
    product: Product,
    photoIndex: Int,
    language: AppLanguage,
    strings: HatchStrings,
    modifier: Modifier = Modifier
) {
    val photos = product.images.ifEmpty { listOfNotNull(product.thumbnailUrl) }
    val activePhoto = photos.getOrNull(photoIndex.coerceIn(0, maxOf(0, photos.lastIndex))).orEmpty()
    val title = if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(HatchCardElevated)
            .border(1.dp, HatchBorderSubtle, RoundedCornerShape(26.dp))
    ) {
        AsyncImage(model = activePhoto, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

        if (photos.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                photos.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (idx == photoIndex) HatchAmber400 else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(top = 24.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BadgePill(strings.conditionLabel(product.condition), HatchBgDark.copy(alpha = 0.8f), Color.White)
            if (product.urgentSale) BadgePill("⚡ ${strings.urgentBadge}", HatchRose500, Color.White)
            if (product.paymentMethods.contains(PaymentMethod.BANKAK.wireValue)) {
                BadgePill(strings.paymentLabel("bankak"), HatchEmerald500, Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, HatchBgDark.copy(alpha = 0.94f), HatchBgDark)))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(HatchFormatters.formatSdg(product.price, language), color = HatchAmber400, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    if (product.negotiable) {
                        BadgePill(strings.negotiableBadge, HatchAmber500.copy(alpha = 0.2f), HatchAmber400)
                    }
                }
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(14.dp))
                        Text(product.city.substringBefore(" ("), color = HatchTextSecondary, fontSize = 12.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HatchSurfaceDark.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(product.sellerName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (product.sellerVerified) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(13.dp))
                        }
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(12.dp))
                        Text(String.format(java.util.Locale.US, "%.1f", product.sellerRating), color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeStamp(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .border(2.5.dp, color, RoundedCornerShape(10.dp))
            .background(HatchBgDark.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun SwipeActionCircle(
    icon: ImageVector,
    tint: Color,
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(HatchSurfaceDark)
            .border(1.5.dp, tint.copy(alpha = if (enabled) 0.5f else 0.15f), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}
