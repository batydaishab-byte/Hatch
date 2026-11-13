package com.hatch.marketplace.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.R
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.MainTab
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.data.model.UiToast
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
import com.hatch.marketplace.ui.theme.HatchRed500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import com.hatch.marketplace.ui.theme.HatchYellow200
import kotlinx.coroutines.delay

@Composable
fun HatchHeader(
    strings: HatchStrings,
    language: AppLanguage,
    currentUser: PublicUser?,
    unreadNotifications: Int,
    onLogoClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSafetyGuide: () -> Unit,
    onOpenNativeInfo: () -> Unit,
    onOpenAuthOrProfile: () -> Unit
) {
    Surface(
        color = HatchBgDark.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(width = 0.5.dp, color = HatchBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable { onLogoClick() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hatch_brand_logo),
                    contentDescription = "HATCH Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, HatchAmber500, RoundedCornerShape(10.dp))
                )
                Column {
                    Text(
                        text = strings.appName,
                        color = HatchAmber400,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = strings.appTagline,
                        color = HatchTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onOpenSafetyGuide,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HatchEmerald500.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = strings.safetyTitle,
                        tint = HatchEmerald400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenNativeInfo,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HatchSurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "App Info",
                        tint = HatchAmber300,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HatchSurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = strings.notificationsTitle,
                            tint = HatchTextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(HatchRose500),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadNotifications.coerceAtMost(9).toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HatchSurfaceDark)
                        .border(1.dp, HatchBorderSubtle, RoundedCornerShape(50))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "EN" else "عربي",
                        color = HatchAmber400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(HatchCardElevated)
                        .border(1.dp, if (currentUser != null) HatchAmber500 else HatchBorderSubtle, CircleShape)
                        .clickable { onOpenAuthOrProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser != null && currentUser.avatar.isNotBlank()) {
                        AsyncImage(
                            model = currentUser.avatar,
                            contentDescription = currentUser.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PersonOutline,
                            contentDescription = "Account",
                            tint = HatchAmber400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HatchBottomNav(
    strings: HatchStrings,
    activeTab: MainTab,
    unreadChatsCount: Int,
    favoritesCount: Int,
    isAdmin: Boolean,
    adminAlertsCount: Int,
    onSelectTab: (MainTab) -> Unit,
    onSellClick: () -> Unit
) {
    Surface(
        color = HatchBgDark.copy(alpha = 0.98f),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = HatchBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItemButton(
                label = strings.navSwipe,
                icon = Icons.Rounded.Style,
                selected = activeTab == MainTab.SWIPE,
                badgeCount = 0,
                onClick = { onSelectTab(MainTab.SWIPE) },
                modifier = Modifier.weight(1f)
            )
            NavItemButton(
                label = strings.navBrowse,
                icon = Icons.Rounded.Storefront,
                selected = activeTab == MainTab.BROWSE,
                badgeCount = 0,
                onClick = { onSelectTab(MainTab.BROWSE) },
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSellClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(HatchAmber500, HatchYellow200))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.navSell,
                            tint = HatchBgDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = strings.navSell,
                        color = HatchAmber400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            NavItemButton(
                label = strings.navChat,
                icon = Icons.Rounded.ChatBubbleOutline,
                selected = activeTab == MainTab.CHAT,
                badgeCount = unreadChatsCount,
                onClick = { onSelectTab(MainTab.CHAT) },
                modifier = Modifier.weight(1f)
            )
            NavItemButton(
                label = strings.navFavorites,
                icon = Icons.Rounded.FavoriteBorder,
                selected = activeTab == MainTab.FAVORITES,
                badgeCount = favoritesCount,
                onClick = { onSelectTab(MainTab.FAVORITES) },
                modifier = Modifier.weight(1f)
            )
            NavItemButton(
                label = strings.navProfile,
                icon = Icons.Rounded.PersonOutline,
                selected = activeTab == MainTab.PROFILE,
                badgeCount = 0,
                onClick = { onSelectTab(MainTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                NavItemButton(
                    label = strings.navAdmin,
                    icon = Icons.Rounded.AdminPanelSettings,
                    selected = activeTab == MainTab.ADMIN,
                    badgeCount = adminAlertsCount,
                    onClick = { onSelectTab(MainTab.ADMIN) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItemButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) HatchAmber400 else HatchTextMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(HatchRose500)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(99).toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OfflineIndicatorBanner(isOnline: Boolean, message: String) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HatchRed500.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text = message, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ToastHostOverlay(
    toasts: List<UiToast>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        toasts.forEach { toast ->
            val (bg, iconColor, icon) = when (toast.type) {
                ToastType.SUCCESS -> Triple(Color(0xFF064E3B), HatchEmerald400, Icons.Rounded.CheckCircle)
                ToastType.WARNING -> Triple(Color(0xFF451A03), HatchAmber400, Icons.Rounded.WarningAmber)
                ToastType.ERROR -> Triple(Color(0xFF450A0A), HatchRose500, Icons.Rounded.ErrorOutline)
                ToastType.INFO -> Triple(HatchCardElevated, HatchAmber400, Icons.Rounded.Info)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { onDismiss(toast.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                Text(text = toast.message, color = HatchTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = HatchTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AdMobBannerCard(
    ads: List<SponsoredAdItem>,
    config: AdMobConfig,
    language: AppLanguage,
    strings: HatchStrings,
    onImpression: (String) -> Unit,
    onClickAd: (SponsoredAdItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val eligible = remember(ads) {
        ads.filter { it.isActive && (it.format == "banner" || it.format == "all") }
    }
    if (!config.isEnabled || eligible.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    val currentAd = eligible[index % eligible.size]

    LaunchedEffect(eligible.size, config.bannerRotationSeconds) {
        if (eligible.size > 1) {
            while (true) {
                delay(config.bannerRotationSeconds.coerceIn(3, 60) * 1000L)
                index = (index + 1) % eligible.size
            }
        }
    }

    LaunchedEffect(currentAd.id) { onImpression(currentAd.id) }

    val context = LocalContext.current
    val title = if (language == AppLanguage.AR) currentAd.titleAr else currentAd.titleEn.ifBlank { currentAd.titleAr }
    val cta = if (language == AppLanguage.AR) currentAd.ctaTextAr.ifBlank { "افتح" } else currentAd.ctaTextEn.ifBlank { "Open" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchAmber500.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable {
                onClickAd(currentAd)
                openAdTarget(context, currentAd.targetUrl)
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (currentAd.imageUrl.isNotBlank()) {
            AsyncImage(
                model = currentAd.imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.sponsoredLabel,
                    color = HatchBgDark,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(HatchAmber400)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
                Text(text = currentAd.advertiser, color = HatchTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = title, color = HatchTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(HatchAmber500.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(text = cta, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun AdMobNativeCard(
    ad: SponsoredAdItem,
    language: AppLanguage,
    strings: HatchStrings,
    onImpression: (String) -> Unit,
    onClickAd: (SponsoredAdItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(ad.id) { onImpression(ad.id) }

    val title = if (language == AppLanguage.AR) ad.titleAr else ad.titleEn.ifBlank { ad.titleAr }
    val desc = if (language == AppLanguage.AR) ad.descriptionAr else ad.descriptionEn.ifBlank { ad.descriptionAr }
    val cta = if (language == AppLanguage.AR) ad.ctaTextAr.ifBlank { "استكشف العرض" } else ad.ctaTextEn.ifBlank { "Learn More" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable {
                onClickAd(ad)
                openAdTarget(context, ad.targetUrl)
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(128.dp).background(HatchCardElevated)) {
            if (ad.imageUrl.isNotBlank()) {
                AsyncImage(model = ad.imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Text(
                text = strings.sponsoredLabel,
                color = HatchBgDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HatchAmber400)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = ad.advertiser, color = HatchAmber400, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = title, color = HatchTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (desc.isNotBlank()) {
                Text(text = desc, color = HatchTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = {
                    onClickAd(ad)
                    openAdTarget(context, ad.targetUrl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = cta, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

fun openAdTarget(context: android.content.Context, targetUrl: String) {
    val clean = targetUrl.trim()
    if (clean.startsWith("https://") || clean.startsWith("http://")) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean))) }
    }
}
