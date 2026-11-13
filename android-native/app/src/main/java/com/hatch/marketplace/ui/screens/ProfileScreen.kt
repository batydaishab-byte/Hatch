package com.hatch.marketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.ProfileSubTab
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.modals.SelectablePill
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchRed500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary

@Composable
fun ProfileScreen(
    isSignedIn: Boolean,
    publicUser: PublicUser?,
    privateUser: PrivateUser?,
    isCurrentlySuspended: Boolean,
    myProducts: List<Product>,
    favoriteProducts: List<Product>,
    activeSubTab: ProfileSubTab,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectSubTab: (ProfileSubTab) -> Unit,
    onOpenAuth: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onMarkSold: (Product) -> Unit,
    onWatchRewardedAd: () -> Unit,
    onBoostProductWithCredit: (String) -> Unit,
    onOpenSafetyGuide: () -> Unit,
    onOpenNativeInfo: () -> Unit
) {
    if (!isSignedIn || publicUser == null) {
        Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(HatchSurfaceDark)
                    .border(1.dp, HatchBorderDark, RoundedCornerShape(22.dp))
                    .padding(24.dp)
            ) {
                Text(strings.signInTitle, color = HatchAmber400, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(strings.signInSubtitle, color = HatchTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = onOpenAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(strings.signInGoogle, fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }

    val activeAds = myProducts.filter { it.status == ProductStatus.ACTIVE.wireValue || it.status == ProductStatus.FLAGGED.wireValue }
    val soldAds = myProducts.filter { it.status == ProductStatus.SOLD.wireValue }
    val rewardCredits = privateUser?.rewardCredits ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Suspension Notice Banner
        if (isCurrentlySuspended) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HatchRose500.copy(alpha = 0.14f))
                    .border(1.5.dp, HatchRose500, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = HatchRose500, modifier = Modifier.size(18.dp))
                    Text(strings.suspendedBannerTitle, color = HatchRose500, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                if (!publicUser.suspensionReason.isNullOrBlank()) {
                    Text(
                        text = "${if (language == AppLanguage.AR) "السبب:" else "Reason:"} ${publicUser.suspensionReason}",
                        color = HatchTextPrimary,
                        fontSize = 12.sp
                    )
                }
                if (!publicUser.suspendedUntil.isNullOrBlank()) {
                    Text(
                        text = "${if (language == AppLanguage.AR) "حتى تاريخ:" else "Until:"} ${publicUser.suspendedUntil.take(10)}",
                        color = HatchTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Profile Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HatchSurfaceDark)
                .border(1.dp, HatchBorderDark, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = publicUser.avatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80" },
                        contentDescription = publicUser.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, HatchAmber500, CircleShape)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(publicUser.name, color = HatchTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            if (publicUser.verifiedIdentity) {
                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(publicUser.city, color = HatchTextSecondary, fontSize = 11.sp)
                        if (!privateUser?.phone.isNullOrBlank()) {
                            Text(privateUser!!.phone, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row {
                    IconButton(onClick = onOpenEditProfile) { Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = HatchAmber400) }
                    IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout", tint = HatchRed500) }
                }
            }

            // Stats Strip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatChip("⭐ ${String.format(java.util.Locale.US, "%.1f", publicUser.rating)}", if (language == AppLanguage.AR) "التقييم" else "Rating")
                StatChip("${publicUser.dealsCompleted}", if (language == AppLanguage.AR) "صفقات ناجحة" else "Deals")
                StatChip("${activeAds.size}", if (language == AppLanguage.AR) "إعلانات نشطة" else "Active")
                StatChip("$rewardCredits", if (language == AppLanguage.AR) "رصيد تمييز" else "Credits")
            }

            // 10-Deal Auto-Verification Progress Bar
            val progress = (publicUser.dealsCompleted.toFloat() / MarketplaceLogic.AUTO_VERIFY_DEALS_THRESHOLD.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.verifiedProgressTitle, color = HatchTextMuted, fontSize = 11.sp)
                    Text("${publicUser.dealsCompleted} / 10", color = HatchEmerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = HatchEmerald500,
                    trackColor = HatchBorderDark,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                )
            }
        }

        // Rewarded Boost Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HatchAmber500.copy(alpha = 0.1f))
                .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(26.dp))
                Column {
                    Text("${strings.boostRewardTitle} ($rewardCredits)", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text(strings.boostRewardSubtitle, color = HatchTextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onWatchRewardedAd,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+1", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }

        // Profile Sub-Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectablePill(
                label = if (language == AppLanguage.AR) "إعلاناتي (${activeAds.size})" else "My Ads (${activeAds.size})",
                selected = activeSubTab == ProfileSubTab.MY_ADS
            ) { onSelectSubTab(ProfileSubTab.MY_ADS) }
            SelectablePill(
                label = if (language == AppLanguage.AR) "المباعة (${soldAds.size})" else "Sold (${soldAds.size})",
                selected = activeSubTab == ProfileSubTab.SOLD_ADS
            ) { onSelectSubTab(ProfileSubTab.SOLD_ADS) }
            SelectablePill(
                label = if (language == AppLanguage.AR) "المحفوظات (${favoriteProducts.size})" else "Saved (${favoriteProducts.size})",
                selected = activeSubTab == ProfileSubTab.SAVED
            ) { onSelectSubTab(ProfileSubTab.SAVED) }
            SelectablePill(
                label = if (language == AppLanguage.AR) "الأمان" else "Safety",
                selected = activeSubTab == ProfileSubTab.SAFETY
            ) { onSelectSubTab(ProfileSubTab.SAFETY) }
        }

        // Sub-Tab Content
        when (activeSubTab) {
            ProfileSubTab.MY_ADS -> {
                if (activeAds.isEmpty()) {
                    EmptyProfilePlaceholder(if (language == AppLanguage.AR) "لم تنشر أي إعلانات نشطة بعد" else "No active ads yet")
                } else {
                    activeAds.forEach { product ->
                        ProfileListingRow(
                            product = product,
                            language = language,
                            showActions = true,
                            canBoost = rewardCredits > 0 && !product.featured,
                            strings = strings,
                            onClick = { onOpenProduct(product) },
                            onEdit = { onEditProduct(product) },
                            onMarkSold = { onMarkSold(product) },
                            onBoost = { onBoostProductWithCredit(product.id) }
                        )
                    }
                }
            }
            ProfileSubTab.SOLD_ADS -> {
                if (soldAds.isEmpty()) {
                    EmptyProfilePlaceholder(if (language == AppLanguage.AR) "لا توجد صفقات مباعة بعد" else "No sold listings yet")
                } else {
                    soldAds.forEach { product ->
                        ProfileListingRow(product, language, false, false, strings, { onOpenProduct(product) }, {}, {}, {})
                    }
                }
            }
            ProfileSubTab.SAVED -> {
                if (favoriteProducts.isEmpty()) {
                    EmptyProfilePlaceholder(if (language == AppLanguage.AR) "لا توجد مفضلات محفوظة" else "No saved items")
                } else {
                    favoriteProducts.forEach { product ->
                        ProfileListingRow(product, language, false, false, strings, { onOpenProduct(product) }, {}, {}, {})
                    }
                }
            }
            ProfileSubTab.SAFETY -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HatchSurfaceDark)
                        .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = HatchEmerald400)
                        Text(strings.safetyTitle, color = HatchEmerald400, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Text(strings.safetyWarningShort, color = HatchTextPrimary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenSafetyGuide,
                            colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) { Text(if (language == AppLanguage.AR) "دليل التداول الآمن" else "Read Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = onOpenNativeInfo, modifier = Modifier.weight(1f)) {
                            Text(if (language == AppLanguage.AR) "حول التطبيق" else "App Info", color = HatchAmber400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(label, color = HatchTextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyProfilePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HatchSurfaceDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = HatchTextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileListingRow(
    product: Product,
    language: AppLanguage,
    showActions: Boolean,
    canBoost: Boolean,
    strings: HatchStrings,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMarkSold: () -> Unit,
    onBoost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = product.thumbnailUrl ?: product.images.firstOrNull().orEmpty(),
                contentDescription = product.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(10.dp)).background(HatchCardElevated)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(HatchFormatters.formatSdg(product.price, language), color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("${product.viewsCount} ${strings.viewsLabel} • ${product.status}", color = HatchTextMuted, fontSize = 10.sp)
            }
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.editAdTitle, color = HatchAmber400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onMarkSold,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.markSoldTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (canBoost) {
                    Button(
                        onClick = onBoost,
                        colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
