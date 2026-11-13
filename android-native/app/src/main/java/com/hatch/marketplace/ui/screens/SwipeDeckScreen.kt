package com.hatch.marketplace.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber300
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchBorderSubtle
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchEmerald950
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class SwipeQuickCity(
    val wireValue: String,
    val emojiLabel: String,
    val tokens: List<String>
) {
    fun matches(selectedCity: String): Boolean = tokens.any { selectedCity.contains(it) }
}

private val SWIPE_QUICK_CITIES = listOf(
    SwipeQuickCity("بورتسودان (ولاية البحر الأحمر)", "🌊 بورتسودان", listOf("بورتسودان")),
    SwipeQuickCity("الخرطوم (ولاية الخرطوم)", "🏙️ الخرطوم", listOf("الخرطوم")),
    SwipeQuickCity("أم درمان (ولاية الخرطوم)", "🕌 أم درمان", listOf("أم درمان")),
    SwipeQuickCity("ودمدني (ولاية الجزيرة)", "🌾 ودمدني", listOf("ودمدني", "ود مدني")),
    SwipeQuickCity("عطبرة (ولاية نهر النيل)", "🚂 عطبرة", listOf("عطبرة"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeckScreen(
    products: List<Product>,
    favoriteIds: Set<String>,
    selectedCity: String,
    canUndo: Boolean,
    isRefreshing: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectCity: (String) -> Unit,
    onRefresh: () -> Unit,
    onBrowseNext: (Product) -> Unit,
    onBrowsePrevious: () -> Unit,
    onSwipeUpOffer: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenDetail: (Product) -> Unit,
    onResetDeck: () -> Unit,
    onGoToBrowse: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Original 6 City Filter Pills Bar + Left Counter
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
                    val allActive = selectedCity == "all"
                    QuickCityPill(
                        label = strings.allCities,
                        active = allActive,
                        onClick = { onSelectCity("all") }
                    )
                    SWIPE_QUICK_CITIES.forEach { qc ->
                        val active = qc.matches(selectedCity)
                        QuickCityPill(
                            label = qc.emojiLabel,
                            active = active,
                            onClick = { onSelectCity(if (active) "all" else qc.wireValue) }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${products.size} ${if (language == AppLanguage.AR) "عرض" else "left"}",
                    color = HatchTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(HatchAmber500.copy(alpha = 0.12f))
                                .border(1.dp, HatchAmber500.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(30.dp))
                        }
                        Text(strings.swipeEmptyTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(strings.swipeEmptySubtitle, color = HatchTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onResetDeck,
                                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.reloadDeck, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { if (selectedCity != "all") onSelectCity("all") else onGoToBrowse() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (selectedCity != "all") strings.allCities else strings.browseAllAds,
                                    color = HatchAmber400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                val topProduct = products.first()
                val nextProduct = products.getOrNull(1)
                val isTopFavorited = favoriteIds.contains(topProduct.id)

                val scope = rememberCoroutineScope()
                val offsetX = remember(topProduct.id) { Animatable(0f) }
                val offsetY = remember(topProduct.id) { Animatable(0f) }
                var photoIndex by remember(topProduct.id) { mutableIntStateOf(0) }
                var isExiting by remember(topProduct.id) { mutableStateOf(false) }

                fun triggerNextCard() {
                    if (isExiting) return
                    isExiting = true
                    scope.launch {
                        offsetX.animateTo(950f, tween(170))
                        onBrowseNext(topProduct)
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val maxCardHeight = minOf(maxHeight, 580.dp)
                    val targetRatio = 3f / 4.4f
                    val cardWidth = minOf(maxWidth, maxCardHeight * targetRatio)
                    val cardHeight = cardWidth / targetRatio

                    if (nextProduct != null) {
                        SwipeCardContent(
                            product = nextProduct,
                            photoIndex = 0,
                            isFavorite = favoriteIds.contains(nextProduct.id),
                            isInteractive = false,
                            language = language,
                            strings = strings,
                            onPrevPhoto = {},
                            onNextPhoto = {},
                            onToggleFavorite = {},
                            onOpenDetail = {},
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .scale(0.94f)
                                .offset(y = 12.dp)
                                .alpha(0.65f)
                        )
                    }

                    val photos = topProduct.images.ifEmpty { listOfNotNull(topProduct.thumbnailUrl) }
                    val dragX = offsetX.value
                    val dragY = offsetY.value
                    val dragAlpha = (1f - (abs(dragX) - 220f).coerceAtLeast(0f) / 380f).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                            .rotate((dragX / 28f).coerceIn(-18f, 18f))
                            .alpha(dragAlpha)
                            .pointerInput(topProduct.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (isExiting) return@detectDragGestures
                                        val curX = offsetX.value
                                        val curY = offsetY.value
                                        scope.launch {
                                            when {
                                                curY < -170f && abs(curX) < 160f -> {
                                                    launch { offsetX.animateTo(0f, spring()) }
                                                    launch { offsetY.animateTo(0f, spring()) }
                                                    onSwipeUpOffer(topProduct)
                                                }
                                                curY > 155f && abs(curX) < 140f -> {
                                                    launch { offsetX.animateTo(0f, spring()) }
                                                    launch { offsetY.animateTo(0f, spring()) }
                                                    onRefresh()
                                                }
                                                curX > 195f -> {
                                                    triggerNextCard()
                                                }
                                                curX < -195f -> {
                                                    if (canUndo) {
                                                        launch { offsetX.animateTo(0f, spring()) }
                                                        launch { offsetY.animateTo(0f, spring()) }
                                                        onBrowsePrevious()
                                                    } else {
                                                        launch { offsetX.animateTo(0f, spring()) }
                                                        launch { offsetY.animateTo(0f, spring()) }
                                                    }
                                                }
                                                else -> {
                                                    launch { offsetX.animateTo(0f, spring()) }
                                                    launch { offsetY.animateTo(0f, spring()) }
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            launch { offsetX.animateTo(0f, spring()) }
                                            launch { offsetY.animateTo(0f, spring()) }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (isExiting) return@detectDragGestures
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-280f, 190f))
                                        }
                                    }
                                )
                            }
                    ) {
                        SwipeCardContent(
                            product = topProduct,
                            photoIndex = photoIndex,
                            isFavorite = isTopFavorited,
                            isInteractive = true,
                            language = language,
                            strings = strings,
                            onPrevPhoto = {
                                if (photos.size > 1) {
                                    photoIndex = (photoIndex - 1 + photos.size) % photos.size
                                }
                            },
                            onNextPhoto = {
                                if (photos.size > 1) {
                                    photoIndex = (photoIndex + 1) % photos.size
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(topProduct.id) },
                            onOpenDetail = { onOpenDetail(topProduct) },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Browse Next Stamp (Swipe Right)
                        if (dragX > 45f) {
                            val stampAlpha = ((dragX - 45f) / 140f).coerceIn(0f, 1f)
                            SwipeStamp(
                                text = if (language == AppLanguage.AR) "التالي ➡️" else "NEXT ➡️",
                                color = HatchAmber400,
                                icon = null,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 64.dp, start = 20.dp)
                                    .rotate(-12f)
                                    .alpha(stampAlpha)
                            )
                        }

                        // Browse Previous Stamp (Swipe Left)
                        if (dragX < -45f) {
                            val stampAlpha = ((-dragX - 45f) / 140f).coerceIn(0f, 1f)
                            SwipeStamp(
                                text = if (language == AppLanguage.AR) "السابق ⬅️" else "PREVIOUS ⬅️",
                                color = HatchEmerald400,
                                icon = Icons.Rounded.Replay,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 64.dp, end = 20.dp)
                                    .rotate(12f)
                                    .alpha(stampAlpha)
                            )
                        }

                        // Super Offer Stamp (Swipe Up)
                        if (dragY < -55f && abs(dragY) > abs(dragX)) {
                            val stampAlpha = ((-dragY - 55f) / 120f).coerceIn(0f, 1f)
                            SwipeStamp(
                                text = if (language == AppLanguage.AR) "عرض فوري ⚡" else "SUPER OFFER ⚡",
                                color = HatchAmber300,
                                icon = Icons.Rounded.Bolt,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 148.dp)
                                    .alpha(stampAlpha)
                            )
                        }

                        // Pull-down Refresh Stamp (Drag Down)
                        if (dragY > 55f && abs(dragY) > abs(dragX)) {
                            val stampAlpha = ((dragY - 55f) / 100f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 56.dp)
                                    .size(48.dp)
                                    .alpha(stampAlpha)
                                    .clip(CircleShape)
                                    .background(HatchBgDark.copy(alpha = 0.92f))
                                    .border(2.dp, HatchAmber400, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = HatchAmber400,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(dragY * 2f)
                                )
                            }
                        }
                    }
                }

                // Original 4 Control Buttons + Hint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Previous (52.dp)
                        SwipeControlCircle(
                            icon = Icons.Rounded.Replay,
                            iconTint = if (canUndo) HatchEmerald400 else HatchTextMuted.copy(alpha = 0.35f),
                            containerColor = HatchSurfaceDark,
                            borderColor = if (canUndo) HatchEmerald400.copy(alpha = 0.7f) else HatchBorderDark,
                            size = 52.dp,
                            iconSize = 22.dp,
                            enabled = canUndo,
                            onClick = onBrowsePrevious
                        )

                        // 2. Instant Offer (48.dp)
                        SwipeControlCircle(
                            icon = Icons.Rounded.Bolt,
                            iconTint = HatchBgDark,
                            containerColor = HatchAmber500,
                            borderColor = HatchAmber400,
                            size = 48.dp,
                            iconSize = 22.dp,
                            enabled = true,
                            onClick = { onSwipeUpOffer(topProduct) }
                        )

                        // 3. Favorite Heart (56.dp - toggles favorite directly without discarding)
                        val roseActive = Color(0xFFE11D48)
                        SwipeControlCircle(
                            icon = if (isTopFavorited) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            iconTint = if (isTopFavorited) Color.White else HatchRose500,
                            containerColor = if (isTopFavorited) roseActive else HatchSurfaceDark,
                            borderColor = if (isTopFavorited) Color(0xFFFDA4AF) else HatchRose500.copy(alpha = 0.45f),
                            size = 56.dp,
                            iconSize = 24.dp,
                            enabled = true,
                            onClick = { onToggleFavorite(topProduct.id) }
                        )

                        // 4. Next Card (52.dp)
                        SwipeControlCircle(
                            icon = if (language == AppLanguage.AR) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                            iconTint = HatchAmber400,
                            containerColor = HatchSurfaceDark,
                            borderColor = HatchAmber500.copy(alpha = 0.55f),
                            size = 52.dp,
                            iconSize = 26.dp,
                            enabled = !isExiting,
                            onClick = { triggerNextCard() }
                        )
                    }

                    Text(
                        text = if (language == AppLanguage.AR) {
                            "اسحب لليمين ➡️ للتالي • اسحب لليسار ⬅️ للسابق • اسحب للأسفل 🔄 للتحديث"
                        } else {
                            "Swipe right ➡️ for Next • Swipe left ⬅️ for Previous • Pull down 🔄 to refresh"
                        },
                        color = HatchTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCityPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) HatchAmber500 else HatchCardElevated)
            .border(1.dp, if (active) HatchAmber400 else HatchBorderSubtle, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (active) HatchBgDark else HatchTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwipeCardContent(
    product: Product,
    photoIndex: Int,
    isFavorite: Boolean,
    isInteractive: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onPrevPhoto: () -> Unit,
    onNextPhoto: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photos = product.images.ifEmpty { listOfNotNull(product.thumbnailUrl) }
    val activePhoto = photos.getOrNull(photoIndex.coerceIn(0, maxOf(0, photos.lastIndex))).orEmpty()
    val title = if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }
    val area = if (language == AppLanguage.AR) product.areaAr else product.areaEn.ifBlank { product.areaAr }
    val cityClean = product.city.substringBefore(" (")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(HatchCardElevated)
            .border(1.dp, HatchBorderSubtle.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
    ) {
        // Product Image
        AsyncImage(
            model = activePhoto,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Left / Right Transparent Photo Tap Zones (upper 65% of card, below top buttons)
        if (isInteractive) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.66f)
                        .padding(top = 74.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val leftInteraction = remember { MutableInteractionSource() }
                    val rightInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = leftInteraction, indication = null) {
                                if (photos.size > 1) onPrevPhoto() else onOpenDetail()
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = rightInteraction, indication = null) {
                                if (photos.size > 1) onNextPhoto() else onOpenDetail()
                            }
                    )
                }
            }
        }

        // Top Overlay Gradient: Photo Bars + Badges + Info/Heart
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            HatchBgDark.copy(alpha = 0.82f),
                            HatchBgDark.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (photos.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    photos.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (idx == photoIndex) HatchAmber400 else Color.White.copy(alpha = 0.32f))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (product.urgentSale) {
                        SwipeBadgePill(
                            text = strings.urgentBadge,
                            icon = Icons.Rounded.LocalFireDepartment,
                            bg = HatchRose500.copy(alpha = 0.92f),
                            fg = Color.White
                        )
                    }
                    if (product.paymentMethods.contains(PaymentMethod.BANKAK.wireValue)) {
                        SwipeBadgePill(
                            text = if (language == AppLanguage.AR) "بنكك فوري" else "Bankak Ready",
                            icon = Icons.Rounded.CreditCard,
                            bg = HatchEmerald500.copy(alpha = 0.92f),
                            fg = Color.White
                        )
                    }
                    SwipeBadgePill(
                        text = strings.conditionLabel(product.condition),
                        icon = null,
                        bg = HatchSurfaceDark.copy(alpha = 0.85f),
                        fg = HatchAmber300,
                        borderColor = HatchAmber500.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info button (36.dp)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HatchSurfaceDark.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(enabled = isInteractive) { onOpenDetail() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Details", tint = HatchAmber400, modifier = Modifier.size(18.dp))
                    }

                    // Top-Right Heart button (44.dp)
                    val roseActive = Color(0xFFE11D48)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isFavorite) roseActive else HatchBgDark.copy(alpha = 0.82f))
                            .border(
                                width = if (isFavorite) 2.dp else 1.dp,
                                color = if (isFavorite) Color(0xFFFDA4AF) else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable(enabled = isInteractive) { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Info Gradient + Clickable Details
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            HatchBgDark.copy(alpha = 0.90f),
                            HatchBgDark
                        )
                    )
                )
                .clickable(enabled = isInteractive) { onOpenDetail() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = HatchFormatters.formatSdg(product.price, language),
                        color = HatchAmber400,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Text(
                            text = HatchFormatters.formatSdg(product.originalPrice, language),
                            color = HatchTextMuted,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1
                        )
                    }
                }

                if (product.negotiable) {
                    Text(
                        text = strings.negotiableBadge,
                        color = HatchEmerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HatchEmerald950.copy(alpha = 0.78f))
                            .border(1.dp, HatchEmerald500.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            HorizontalDivider(color = HatchBorderDark.copy(alpha = 0.85f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (area.isNotBlank()) "$cityClean • $area" else cityClean,
                        color = HatchTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HatchSurfaceDark.copy(alpha = 0.75f))
                        .border(1.dp, HatchBorderDark, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    if (product.sellerAvatar.isNotBlank()) {
                        AsyncImage(
                            model = product.sellerAvatar,
                            contentDescription = product.sellerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .border(1.dp, HatchAmber400.copy(alpha = 0.55f), CircleShape)
                        )
                    }
                    Text(
                        text = product.sellerName,
                        color = HatchTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "★ ${String.format(Locale.US, "%.1f", product.sellerRating.takeIf { it > 0 } ?: 4.9)}",
                        color = HatchAmber400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.sellerVerified) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = strings.verifiedBadge,
                            tint = HatchEmerald400,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBadgePill(
    text: String,
    icon: ImageVector?,
    bg: Color,
    fg: Color,
    borderColor: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        }
        Text(text = text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SwipeStamp(
    text: String,
    color: Color,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .border(3.dp, color, RoundedCornerShape(14.dp))
            .background(HatchBgDark.copy(alpha = 0.90f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun SwipeControlCircle(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    borderColor: Color,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
