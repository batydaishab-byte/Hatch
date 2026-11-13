package com.hatch.marketplace.ui.modals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.UserReview
import com.hatch.marketplace.ui.components.AdMobBannerCard
import com.hatch.marketplace.ui.components.BadgePill
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.state.ReportRequest
import com.hatch.marketplace.ui.state.ReviewRequest
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
import com.hatch.marketplace.ui.theme.HatchTextDim
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProductDetailSheet(
    product: Product,
    isFavorite: Boolean,
    isOwner: Boolean,
    sellerReviews: List<UserReview>,
    sponsoredAds: List<SponsoredAdItem>,
    adConfig: AdMobConfig,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMakeOffer: () -> Unit,
    onStartChat: () -> Unit,
    onEditListing: () -> Unit,
    onMarkSold: () -> Unit,
    onOpenReview: (ReviewRequest) -> Unit,
    onOpenReport: (ReportRequest) -> Unit,
    onAdImpression: (String) -> Unit,
    onAdClick: (SponsoredAdItem) -> Unit,
    onCopiedToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos = product.images.ifEmpty { listOfNotNull(product.thumbnailUrl) }
    val pagerState = rememberPagerState(pageCount = { maxOf(1, photos.size) })

    val title = if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }
    val desc = if (language == AppLanguage.AR) product.descriptionAr else product.descriptionEn.ifBlank { product.descriptionAr }
    val area = if (language == AppLanguage.AR) product.areaAr else product.areaEn.ifBlank { product.areaAr }
    val hasDiscount = product.originalPrice != null && product.originalPrice > product.price

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = HatchBgDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(HatchSurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                    }
                    Text(
                        text = strings.categoryLabel(product.category),
                        color = HatchAmber400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val shareText = "$title — ${HatchFormatters.formatSdg(product.price, language)}\n${product.city}\nhttps://batydaishab-byte.github.io/Hatch/?product=${product.id}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, strings.shareAd))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = strings.shareAd, tint = HatchTextPrimary)
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) HatchRose500 else HatchTextPrimary
                            )
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Photo Pager
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(HatchCardElevated)
                    ) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            AsyncImage(
                                model = photos.getOrNull(page).orEmpty(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgePill(strings.conditionLabel(product.condition), HatchBgDark.copy(alpha = 0.8f), Color.White)
                            if (product.urgentSale) BadgePill("⚡ ${strings.urgentBadge}", HatchRose500, Color.White)
                            if (product.status == ProductStatus.SOLD.wireValue) {
                                BadgePill(if (language == AppLanguage.AR) "تم البيع" else "SOLD", Color(0xFF334155), Color.White)
                            }
                        }
                    }

                    // Thumbnails
                    if (photos.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photos.forEachIndexed { idx, imgUrl ->
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (pagerState.currentPage == idx) 2.dp else 1.dp,
                                            color = if (pagerState.currentPage == idx) HatchAmber500 else HatchBorderDark,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(title, color = HatchTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = HatchFormatters.formatSdg(product.price, language),
                                color = HatchAmber400,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (hasDiscount) {
                                Text(
                                    text = HatchFormatters.formatSdg(product.originalPrice!!, language),
                                    color = HatchTextDim,
                                    fontSize = 13.sp,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                            if (product.negotiable) {
                                BadgePill(strings.negotiableBadge, HatchAmber500.copy(alpha = 0.2f), HatchAmber400)
                            }
                        }

                        // Location, Delivery & Views
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (area.isNotBlank()) "${product.city.substringBefore(" (")} • $area" else product.city,
                                    color = HatchTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (product.deliveryAvailable) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(14.dp))
                                        Text(strings.deliveryBadge, color = HatchEmerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(14.dp))
                                    Text("${product.viewsCount} ${strings.viewsLabel}", color = HatchTextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        // Payment Methods
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            product.paymentMethods.forEach { method ->
                                val isBankak = method == "bankak"
                                BadgePill(
                                    text = strings.paymentLabel(method),
                                    bg = if (isBankak) HatchEmerald500.copy(alpha = 0.2f) else HatchCardElevated,
                                    fg = if (isBankak) HatchEmerald400 else HatchTextSecondary
                                )
                            }
                        }

                        // Description Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.AR) "الوصف والتفاصيل" else "Description",
                                color = HatchAmber400,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(desc, color = HatchTextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                        }

                        // Seller Info Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (product.sellerAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = product.sellerAvatar,
                                            contentDescription = product.sellerName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(product.sellerName, color = HatchTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            if (product.sellerVerified) {
                                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(
                                            text = "⭐ ${String.format(java.util.Locale.US, "%.1f", product.sellerRating)} • ${product.sellerDealsCount} ${if (language == AppLanguage.AR) "صفقة مكتملة" else "deals"}",
                                            color = HatchTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (!isOwner) {
                                    OutlinedButton(
                                        onClick = {
                                            onOpenReview(
                                                ReviewRequest(
                                                    sellerId = product.sellerId,
                                                    sellerName = product.sellerName,
                                                    sellerAvatar = product.sellerAvatar,
                                                    productId = product.id,
                                                    productTitle = title
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Star, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.rateSeller, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (sellerReviews.isNotEmpty()) {
                                sellerReviews.take(2).forEach { rev ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(HatchCardElevated)
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(rev.reviewerName, color = HatchTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("⭐ ${rev.rating}/5", color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(rev.comment, color = HatchTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Inline AdMob Banner
                        AdMobBannerCard(
                            ads = sponsoredAds,
                            config = adConfig,
                            language = language,
                            strings = strings,
                            onImpression = onAdImpression,
                            onClickAd = onAdClick
                        )

                        // Sudan Safety Warning + Report + Copy Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(HatchEmerald500.copy(alpha = 0.1f))
                                .border(1.dp, HatchEmerald500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(20.dp))
                            Text(strings.safetyWarningShort, color = HatchTextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("HATCH Link", "https://batydaishab-byte.github.io/Hatch/?product=${product.id}"))
                                        onCopiedToast(if (language == AppLanguage.AR) "تم نسخ رابط الإعلان" else "Listing link copied")
                                    }
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(14.dp))
                                Text(if (language == AppLanguage.AR) "نسخ الرابط" else "Copy Link", color = HatchTextMuted, fontSize = 11.sp)
                            }
                            if (!isOwner) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onOpenReport(
                                                ReportRequest(
                                                    userId = product.sellerId,
                                                    userName = product.sellerName,
                                                    productId = product.id,
                                                    productTitle = title
                                                )
                                            )
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = HatchRose500, modifier = Modifier.size(14.dp))
                                    Text(strings.reportAd, color = HatchRose500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Bottom Sticky Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(HatchSurfaceDark)
                        .border(0.5.dp, HatchBorderDark)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isOwner) {
                        OutlinedButton(
                            onClick = onEditListing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.editAdTitle, color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (product.status == ProductStatus.ACTIVE.wireValue) {
                            Button(
                                onClick = onMarkSold,
                                colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.markSoldTitle, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    } else if (product.status == ProductStatus.ACTIVE.wireValue) {
                        OutlinedButton(
                            onClick = onMakeOffer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.makeOffer, color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = onStartChat,
                            colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.chatSeller, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
