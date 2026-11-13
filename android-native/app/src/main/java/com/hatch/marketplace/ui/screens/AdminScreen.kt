package com.hatch.marketplace.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hatch.marketplace.data.media.CloudinaryQuotaStatus
import com.hatch.marketplace.data.media.MediaUploadPolicy
import com.hatch.marketplace.data.model.AdFormat
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AdRevenueStats
import com.hatch.marketplace.data.model.AdminSubTab
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.ReportTicket
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.UserStatus
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.modals.HatchField
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
import java.util.Locale

private data class SuspendTarget(val uid: String, val name: String)

@Composable
fun AdminScreen(
    activeSubTab: AdminSubTab,
    products: List<Product>,
    reports: List<ReportTicket>,
    conversations: List<Conversation>,
    users: List<PublicUser>,
    notifications: List<NotificationItem>,
    sponsoredAds: List<SponsoredAdItem>,
    adConfig: AdMobConfig,
    adStats: AdRevenueStats,
    cloudinaryQuota: CloudinaryQuotaStatus,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectSubTab: (AdminSubTab) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onOpenConversationAudit: (Conversation) -> Unit,
    onApproveFlaggedProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSuspendUser: (uid: String, days: Int, reason: String) -> Unit,
    onUnsuspendUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSendBroadcast: (type: String, titleAr: String, titleEn: String, bodyAr: String, bodyEn: String, relatedProductId: String?) -> Unit,
    onCreateSponsoredAd: (SponsoredAdItem) -> Unit,
    onToggleAdActive: (String, Boolean) -> Unit,
    onUpdateAdDuration: (String, Int) -> Unit,
    onDeleteAd: (String) -> Unit,
    onUpdateAdConfig: (AdMobConfig) -> Unit,
    onResetAdStats: () -> Unit,
    onTestInterstitial: () -> Unit,
    onTestRewarded: () -> Unit,
    onRefreshQuota: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val activeProducts = remember(products) { products.filter { it.status == ProductStatus.ACTIVE.wireValue } }
    val soldProducts = remember(products) { products.filter { it.status == ProductStatus.SOLD.wireValue } }
    val flaggedProducts = remember(products) { products.filter { it.status == ProductStatus.FLAGGED.wireValue } }
    val pendingReports = remember(reports) { reports.filter { it.status == "pending" } }
    val totalMarketVolume = remember(products) { products.sumOf { it.price } }

    var showBroadcastComposer by remember { mutableStateOf(false) }
    var suspendTarget by remember { mutableStateOf<SuspendTarget?>(null) }
    var deleteUserConfirm by remember { mutableStateOf<PublicUser?>(null) }
    var showCreateAdDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Admin Header + Broadcast Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(24.dp))
                Column {
                    Text(strings.adminTitle, color = HatchAmber400, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = if (isAr) "الإشراف الكامل، الإيرادات، وحماية المستخدمين" else "Moderation, Monetization & Safety",
                        color = HatchTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            Button(
                onClick = { showBroadcastComposer = !showBroadcastComposer },
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAr) "إشعار جماعي" else "Broadcast", fontSize = 11.sp, fontWeight = FontWeight.Black)
                Icon(if (showBroadcastComposer) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        // 4 KPI Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiStatCard(
                label = if (isAr) "الإعلانات النشطة" else "Total Active",
                value = activeProducts.size.toString(),
                accent = HatchEmerald400,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                label = if (isAr) "أرشيف المباع" else "Sold Archive",
                value = soldProducts.size.toString(),
                accent = HatchAmber400,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectSubTab(AdminSubTab.SOLD) }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiStatCard(
                label = if (isAr) "بلاغات قيد المراجعة" else "Pending Reports",
                value = pendingReports.size.toString(),
                accent = HatchRose500,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectSubTab(AdminSubTab.REPORTS) }
            )
            KpiStatCard(
                label = if (isAr) "حجم السوق (ج.س)" else "Market Volume",
                value = HatchFormatters.formatSdg(totalMarketVolume, language),
                accent = HatchTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable Broadcast Notifications Composer
        AnimatedVisibility(visible = showBroadcastComposer) {
            BroadcastComposerCard(
                activeProducts = activeProducts,
                strings = strings,
                language = language,
                isSubmitting = isSubmitting,
                onSendBroadcast = { type, tAr, tEn, bAr, bEn, relId ->
                    onSendBroadcast(type, tAr, tEn, bAr, bEn, relId)
                    showBroadcastComposer = false
                }
            )
        }

        // 7 Sub-tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabItems = listOf(
                AdminSubTab.REPORTS to (if (isAr) "البلاغات (${pendingReports.size})" else "Reports (${pendingReports.size})"),
                AdminSubTab.BROADCAST to (if (isAr) "الإشعارات" else "Broadcasts"),
                AdminSubTab.SOLD to (if (isAr) "المباعة (${soldProducts.size})" else "Sold (${soldProducts.size})"),
                AdminSubTab.CONVERSATIONS to (if (isAr) "المحادثات (${conversations.size})" else "Chats (${conversations.size})"),
                AdminSubTab.FLAGGED to (if (isAr) "المعلّقة (${flaggedProducts.size})" else "Flagged (${flaggedProducts.size})"),
                AdminSubTab.MONETIZATION to (if (isAr) "الأرباح والإعلانات" else "Monetization"),
                AdminSubTab.USERS to (if (isAr) "المستخدمون (${users.size})" else "Users (${users.size})")
            )
            tabItems.forEach { (tab, label) ->
                SelectablePill(label = label, selected = activeSubTab == tab) {
                    onSelectSubTab(tab)
                }
            }
        }

        when (activeSubTab) {
            AdminSubTab.REPORTS -> {
                if (reports.isEmpty()) {
                    EmptyAdminSection(if (isAr) "لا توجد بلاغات مسجلة حالياً" else "No reports found")
                } else {
                    reports.forEach { report ->
                        ReportAdminCard(
                            report = report,
                            isAr = isAr,
                            onResolve = { onUpdateReportStatus(report.id, "resolved") },
                            onDismiss = { onUpdateReportStatus(report.id, "dismissed") },
                            onAuditChat = {
                                val linkedConv = conversations.firstOrNull { conv ->
                                    (report.reportedProductId != null && conv.productId == report.reportedProductId) ||
                                        (report.reportedUserId != null && conv.participantIds.contains(report.reportedUserId))
                                }
                                if (linkedConv != null) onOpenConversationAudit(linkedConv)
                            },
                            onDeleteAd = report.reportedProductId?.let { pid -> { onDeleteProduct(pid) } },
                            onSuspendTarget = report.reportedUserId?.let { uid ->
                                { suspendTarget = SuspendTarget(uid, report.reportedUserName ?: uid) }
                            },
                            onDeleteUser = report.reportedUserId?.let { uid ->
                                { deleteUserConfirm = users.firstOrNull { it.uid == uid } ?: PublicUser(uid = uid, name = report.reportedUserName ?: uid) }
                            }
                        )
                    }
                }
            }

            AdminSubTab.BROADCAST -> {
                if (notifications.isEmpty()) {
                    EmptyAdminSection(if (isAr) "لا توجد إشعارات جماعية بعد" else "No broadcast notifications yet")
                } else {
                    notifications.forEach { n ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isAr) n.titleAr else n.titleEn.ifBlank { n.titleAr }, color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(n.type.uppercase(Locale.ROOT), color = HatchTextMuted, fontSize = 10.sp)
                            }
                            Text(if (isAr) n.bodyAr else n.bodyEn.ifBlank { n.bodyAr }, color = HatchTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            AdminSubTab.SOLD -> {
                if (soldProducts.isEmpty()) {
                    EmptyAdminSection(if (isAr) "أرشيف الصفقات المباعة فارغ" else "Sold archive is empty")
                } else {
                    soldProducts.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = p.thumbnailUrl ?: p.images.firstOrNull(),
                                contentDescription = p.titleAr,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.titleAr, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${p.sellerName} • ${HatchFormatters.formatSdg(p.price, language)}", color = HatchEmerald400, fontSize = 11.sp)
                            }
                            Text(if (isAr) "مباع" else "SOLD", color = HatchEmerald400, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            AdminSubTab.CONVERSATIONS -> {
                if (conversations.isEmpty()) {
                    EmptyAdminSection(if (isAr) "لا توجد محادثات للمراجعة" else "No conversations to audit")
                } else {
                    conversations.forEach { conv ->
                        val hasDepositRisk = MarketplaceLogic.hasDepositWarningKeywords(conv.lastMessage)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, if (hasDepositRisk) HatchRose500 else HatchBorderDark, RoundedCornerShape(14.dp))
                                .clickable { onOpenConversationAudit(conv) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${conv.buyerName} ↔ ${conv.sellerName}", color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (hasDepositRisk) {
                                    Text(
                                        text = if (isAr) "تحذير عربون" else "Deposit Alert",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(HatchRose500).padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(conv.productTitle, color = HatchAmber400, fontSize = 11.sp)
                            Text(conv.lastMessage, color = HatchTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            AdminSubTab.FLAGGED -> {
                if (flaggedProducts.isEmpty()) {
                    EmptyAdminSection(if (isAr) "لا توجد إعلانات معلّقة حالياً" else "No flagged listings")
                } else {
                    flaggedProducts.forEach { p ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchRose500.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(p.titleAr, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${p.sellerName} • ${HatchFormatters.formatSdg(p.price, language)}", color = HatchTextMuted, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveFlaggedProduct(p.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isAr) "إعادة تفعيل" else "Approve / Restore", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = { onDeleteProduct(p.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HatchRed500, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isAr) "حذف نهائي" else "Delete", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            AdminSubTab.MONETIZATION -> {
                MonetizationAdminTab(
                    sponsoredAds = sponsoredAds,
                    adConfig = adConfig,
                    adStats = adStats,
                    cloudinaryQuota = cloudinaryQuota,
                    language = language,
                    strings = strings,
                    onRefreshQuota = onRefreshQuota,
                    onOpenAddAd = { showCreateAdDialog = true },
                    onToggleAdActive = onToggleAdActive,
                    onUpdateAdDuration = onUpdateAdDuration,
                    onDeleteAd = onDeleteAd,
                    onTestInterstitial = onTestInterstitial,
                    onTestRewarded = onTestRewarded,
                    onUpdateConfig = onUpdateAdConfig,
                    onResetStats = onResetAdStats
                )
            }

            AdminSubTab.USERS -> {
                UsersAdminTab(
                    users = users,
                    isAr = isAr,
                    onSuspendClick = { u -> suspendTarget = SuspendTarget(u.uid, u.name) },
                    onUnsuspendClick = onUnsuspendUser,
                    onDeleteClick = { u -> deleteUserConfirm = u }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Suspend User Dialog (1, 3, 7, 14, 30, Custom days + 5 preset reasons)
    suspendTarget?.let { target ->
        SuspendUserDialog(
            target = target,
            strings = strings,
            isAr = isAr,
            onDismiss = { suspendTarget = null },
            onConfirm = { days, reason ->
                onSuspendUser(target.uid, days, reason)
                suspendTarget = null
            }
        )
    }

    // Delete User Confirm Dialog
    deleteUserConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteUserConfirm = null },
            containerColor = HatchSurfaceDark,
            title = { Text(if (isAr) "حذف حساب مستخدم" else "Delete User", color = HatchRose500, fontWeight = FontWeight.Black) },
            text = { Text(if (isAr) "هل تريد حذف بيانات ${target.name} من هتش؟" else "Remove ${target.name}'s profile from HATCH?", color = HatchTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(target.uid)
                        deleteUserConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HatchRed500, contentColor = Color.White)
                ) { Text(if (isAr) "حذف" else "Delete", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { deleteUserConfirm = null }) { Text(if (isAr) "إلغاء" else "Cancel", color = HatchTextMuted) }
            }
        )
    }

    // Add Sponsored Ad Dialog
    if (showCreateAdDialog) {
        CreateSponsoredAdDialog(
            strings = strings,
            isAr = isAr,
            onDismiss = { showCreateAdDialog = false },
            onCreate = { ad ->
                onCreateSponsoredAd(ad)
                showCreateAdDialog = false
            }
        )
    }
}

@Composable
private fun KpiStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = HatchTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyAdminSection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = HatchTextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun BroadcastComposerCard(
    activeProducts: List<Product>,
    strings: HatchStrings,
    language: AppLanguage,
    isSubmitting: Boolean,
    onSendBroadcast: (type: String, titleAr: String, titleEn: String, bodyAr: String, bodyEn: String, relatedProductId: String?) -> Unit
) {
    val isAr = language == AppLanguage.AR
    var type by remember { mutableStateOf("offer") }
    var titleAr by remember { mutableStateOf("") }
    var bodyAr by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchAmber500.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(if (isAr) "إرسال إشعار جماعي لكل المستخدمين" else "Send Broadcast Notification", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 14.sp)

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("offer", "alert", "update", "system").forEach { t ->
                SelectablePill(t.uppercase(Locale.ROOT), type == t) { type = t }
            }
        }

        // Templates
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            strings.broadcastTemplates.forEach { tpl ->
                SelectablePill(if (isAr) tpl.labelAr else tpl.labelEn, false) {
                    type = tpl.type
                    titleAr = if (isAr) tpl.titleAr else tpl.titleEn
                    bodyAr = if (isAr) tpl.bodyAr else tpl.bodyEn
                }
            }
        }

        HatchField(titleAr, { titleAr = it }, if (isAr) "عنوان الإشعار *" else "Notification Title *")
        HatchField(bodyAr, { bodyAr = it }, if (isAr) "نص الرسالة *" else "Message Body *", singleLine = false, minLines = 2)

        if (activeProducts.isNotEmpty()) {
            Text(if (isAr) "ربط بإعلان (اختياري):" else "Link Product (optional):", color = HatchTextMuted, fontSize = 11.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectablePill(if (isAr) "بدون ربط" else "None", selectedProductId == null) { selectedProductId = null }
                activeProducts.take(8).forEach { prod ->
                    SelectablePill(prod.titleAr.take(18), selectedProductId == prod.id) { selectedProductId = prod.id }
                }
            }
        }

        // Live preview card
        if (titleAr.isNotBlank() || bodyAr.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HatchCardElevated)
                    .padding(10.dp)
            ) {
                Text(if (isAr) "معاينة حية:" else "Live Preview:", color = HatchAmber400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(titleAr, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(bodyAr, color = HatchTextSecondary, fontSize = 11.sp)
            }
        }

        Button(
            onClick = { onSendBroadcast(type, titleAr, titleAr, bodyAr, bodyAr, selectedProductId) },
            enabled = !isSubmitting && titleAr.isNotBlank() && bodyAr.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isAr) "إرسال الإشعار الآن 📣" else "Send Broadcast Now 📣", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReportAdminCard(
    report: ReportTicket,
    isAr: Boolean,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onAuditChat: () -> Unit,
    onDeleteAd: (() -> Unit)?,
    onSuspendTarget: (() -> Unit)?,
    onDeleteUser: (() -> Unit)?
) {
    val isPending = report.status == "pending"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, if (isPending) HatchRose500.copy(alpha = 0.5f) else HatchBorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = HatchRose500, modifier = Modifier.size(16.dp))
                Text(report.reason, color = HatchRose500, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Text(report.status.uppercase(Locale.ROOT), color = if (isPending) HatchAmber400 else HatchEmerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text("${if (isAr) "المُبلّغ:" else "Reporter:"} ${report.reporterName}", color = HatchTextMuted, fontSize = 11.sp)
        report.reportedUserName?.let {
            Text("${if (isAr) "المُبلّغ عنه:" else "Reported User:"} $it", color = HatchTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        report.reportedProductTitle?.let {
            Text("${if (isAr) "الإعلان:" else "Listing:"} $it", color = HatchAmber400, fontSize = 12.sp)
        }
        Text(report.message, color = HatchTextSecondary, fontSize = 12.sp)

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isPending) {
                Button(onClick = onResolve, colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White)) {
                    Text(if (isAr) "اعتماد الحل" else "Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onDismiss) {
                    Text(if (isAr) "رفض البلاغ" else "Dismiss", color = HatchTextMuted, fontSize = 11.sp)
                }
            }
            OutlinedButton(onClick = onAuditChat) {
                Text(if (isAr) "فحص المحادثة" else "Audit Chat", color = HatchAmber400, fontSize = 11.sp)
            }
            if (onDeleteAd != null) {
                OutlinedButton(onClick = onDeleteAd) {
                    Text(if (isAr) "حذف الإعلان" else "Delete Ad", color = HatchRose500, fontSize = 11.sp)
                }
            }
            if (onSuspendTarget != null) {
                OutlinedButton(onClick = onSuspendTarget) {
                    Text(if (isAr) "إيقاف" else "Suspend", color = HatchAmber400, fontSize = 11.sp)
                }
            }
            if (onDeleteUser != null) {
                OutlinedButton(onClick = onDeleteUser) {
                    Text(if (isAr) "حذف المستخدم" else "Delete User", color = HatchRed500, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MonetizationAdminTab(
    sponsoredAds: List<SponsoredAdItem>,
    adConfig: AdMobConfig,
    adStats: AdRevenueStats,
    cloudinaryQuota: CloudinaryQuotaStatus,
    language: AppLanguage,
    strings: HatchStrings,
    onRefreshQuota: () -> Unit,
    onOpenAddAd: () -> Unit,
    onToggleAdActive: (String, Boolean) -> Unit,
    onUpdateAdDuration: (String, Int) -> Unit,
    onDeleteAd: (String) -> Unit,
    onTestInterstitial: () -> Unit,
    onTestRewarded: () -> Unit,
    onUpdateConfig: (AdMobConfig) -> Unit,
    onResetStats: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val nearQuotaLimit = cloudinaryQuota.isNearLimit || cloudinaryQuota.usagePercent >= MediaUploadPolicy.QUOTA_WARNING_THRESHOLD_PERCENT

    var appId by remember(adConfig) { mutableStateOf(adConfig.admobAppId) }
    var bannerUnit by remember(adConfig) { mutableStateOf(adConfig.bannerUnitId) }
    var interUnit by remember(adConfig) { mutableStateOf(adConfig.interstitialUnitId) }
    var rewardUnit by remember(adConfig) { mutableStateOf(adConfig.rewardedUnitId) }
    var ecpm by remember(adConfig) { mutableStateOf(adConfig.ecpmRate.toString()) }
    var cpc by remember(adConfig) { mutableStateOf(adConfig.cpcRate.toString()) }
    var sdgRate by remember(adConfig) { mutableStateOf(adConfig.sdgRate.toString()) }
    var isEnabled by remember(adConfig) { mutableStateOf(adConfig.isEnabled) }
    var isTestMode by remember(adConfig) { mutableStateOf(adConfig.isTestMode) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Cloudinary Quota Warning Banner (Admin Only)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (nearQuotaLimit) HatchRose500.copy(alpha = 0.14f) else HatchSurfaceDark)
                .border(1.dp, if (nearQuotaLimit) HatchRose500 else HatchBorderDark, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (nearQuotaLimit) Icons.Rounded.WarningAmber else Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = if (nearQuotaLimit) HatchRose500 else HatchEmerald400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isAr) "استهلاك Cloudinary المجاني" else "Cloudinary Free Tier Quota",
                        color = if (nearQuotaLimit) HatchRose500 else HatchTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onRefreshQuota, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Quota", tint = HatchAmber400, modifier = Modifier.size(16.dp))
                }
            }
            LinearProgressIndicator(
                progress = { (cloudinaryQuota.usagePercent.toFloat() / 100f).coerceIn(0f, 1f) },
                color = if (nearQuotaLimit) HatchRose500 else HatchEmerald500,
                trackColor = HatchBorderDark,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
            )
            Text(
                text = String.format(
                    Locale.US,
                    "%.2f / %.1f credits (%.1f%%)",
                    cloudinaryQuota.creditsUsed,
                    cloudinaryQuota.creditsLimit,
                    cloudinaryQuota.usagePercent
                ),
                color = HatchTextSecondary,
                fontSize = 11.sp
            )
            if (nearQuotaLimit) {
                Text(
                    text = if (isAr) "تنبيه: اقترب استهلاك Cloudinary من الحد المجاني (80%+). قلل رفع الصور كبيرة الحجم." else "Warning: Cloudinary free quota reached 80%+.",
                    color = HatchRose500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = if (cloudinaryQuota.videoUploadsEnabled) HatchEmerald400 else HatchAmber400, modifier = Modifier.size(14.dp))
                Text(
                    text = if (cloudinaryQuota.videoUploadsEnabled) {
                        if (isAr) "رفع الفيديو للمشرفين مفعّل عبر خادم التوقيع" else "Admin video upload enabled via signer"
                    } else {
                        if (isAr) "رفع الفيديو مقفل أمنياً (لا يوجد خادم توقيع مجاني بعد — بدون أسرار في التطبيق)" else "Video upload locked (fail-closed until free signer URL configured)"
                    },
                    color = HatchTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Live Revenue Stats Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HatchSurfaceDark)
                .border(1.dp, HatchAmber500.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = HatchEmerald400)
                Text(if (isAr) "ملخص الأرباح والأداء" else "Revenue & Impressions", color = HatchEmerald400, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${if (isAr) "مشاهدات:" else "Impr:"} ${adStats.totalImpressions}", color = HatchTextPrimary, fontSize = 12.sp)
                Text("${if (isAr) "نقرات:" else "Clicks:"} ${adStats.totalClicks}", color = HatchTextPrimary, fontSize = 12.sp)
                Text(String.format(Locale.US, "CTR: %.2f%%", adStats.ctrPercent), color = HatchAmber400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format(Locale.US, "$%.2f USD", adStats.estimatedUsd), color = HatchEmerald400, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(HatchFormatters.formatSdg(adStats.estimatedSdg, language), color = HatchAmber400, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }

        // Action Buttons (Add Campaign + Test Sandbox)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenAddAd,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAr) "حملة جديدة" else "New Campaign", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = onTestInterstitial) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                Text(if (isAr) "بيني" else "Interstitial", color = HatchAmber400, fontSize = 11.sp)
            }
            OutlinedButton(onClick = onTestRewarded) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                Text(if (isAr) "مكافأة" else "Rewarded", color = HatchEmerald400, fontSize = 11.sp)
            }
        }

        // Active Campaigns List
        sponsoredAds.forEach { ad ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HatchSurfaceDark)
                    .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = ad.imageUrl,
                    contentDescription = ad.titleAr,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isAr) ad.titleAr else ad.titleEn.ifBlank { ad.titleAr }, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    Text("${ad.format} • ${ad.impressionsCount} imp • ${ad.clicksCount} clicks", color = HatchTextMuted, fontSize = 10.sp)
                }
                Switch(
                    checked = ad.isActive,
                    onCheckedChange = { onToggleAdActive(ad.id, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = HatchBgDark, checkedTrackColor = HatchEmerald500)
                )
                IconButton(onClick = { onDeleteAd(ad.id) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = HatchRose500, modifier = Modifier.size(18.dp))
                }
            }
        }

        // AdMob Config Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HatchSurfaceDark)
                .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (isAr) "إعدادات AdMob والأسعار" else "AdMob Config & Rates", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "تفعيل نظام الإعلانات" else "Enable Ads", color = HatchTextPrimary, fontSize = 12.sp)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "وضع الاختبار (Test Mode)" else "Test Mode", color = HatchTextPrimary, fontSize = 12.sp)
                Switch(checked = isTestMode, onCheckedChange = { isTestMode = it })
            }
            HatchField(appId, { appId = it }, "AdMob App ID")
            HatchField(bannerUnit, { bannerUnit = it }, "Banner Unit ID")
            HatchField(interUnit, { interUnit = it }, "Interstitial Unit ID")
            HatchField(rewardUnit, { rewardUnit = it }, "Rewarded Unit ID")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HatchField(ecpm, { ecpm = it }, "eCPM ($)", KeyboardType.Decimal, modifier = Modifier.weight(1f))
                HatchField(cpc, { cpc = it }, "CPC ($)", KeyboardType.Decimal, modifier = Modifier.weight(1f))
                HatchField(sdgRate, { sdgRate = it }, "SDG/$", KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onUpdateConfig(
                            adConfig.copy(
                                isEnabled = isEnabled,
                                isTestMode = isTestMode,
                                admobAppId = appId,
                                bannerUnitId = bannerUnit,
                                interstitialUnitId = interUnit,
                                rewardedUnitId = rewardUnit,
                                ecpmRate = ecpm.toDoubleOrNull() ?: adConfig.ecpmRate,
                                cpcRate = cpc.toDoubleOrNull() ?: adConfig.cpcRate,
                                sdgRate = sdgRate.toDoubleOrNull() ?: adConfig.sdgRate
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isAr) "حفظ الإعدادات" else "Save Config", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                OutlinedButton(onClick = onResetStats) {
                    Text(if (isAr) "تصفير العدادات" else "Reset Stats", color = HatchRose500, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun UsersAdminTab(
    users: List<PublicUser>,
    isAr: Boolean,
    onSuspendClick: (PublicUser) -> Unit,
    onUnsuspendClick: (String) -> Unit,
    onDeleteClick: (PublicUser) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") }

    val filtered = remember(users, search, filterMode) {
        users.filter { u ->
            val q = search.trim().lowercase(Locale.ROOT)
            val matchesQ = q.isBlank() || u.name.lowercase(Locale.ROOT).contains(q) || u.city.lowercase(Locale.ROOT).contains(q)
            val suspended = MarketplaceLogic.isUserSuspended(u.status, u.suspendedUntil)
            val matchesFilter = when (filterMode) {
                "active" -> !suspended
                "suspended" -> suspended
                "verified" -> u.verifiedIdentity
                else -> true
            }
            matchesQ && matchesFilter
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HatchField(search, { search = it }, if (isAr) "بحث بالاسم أو المدينة..." else "Search users...")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "all" to (if (isAr) "الكل" else "All"),
                "active" to (if (isAr) "نشط" else "Active"),
                "suspended" to (if (isAr) "موقوف" else "Suspended"),
                "verified" to (if (isAr) "موثق" else "Verified")
            ).forEach { (key, label) ->
                SelectablePill(label, filterMode == key) { filterMode = key }
            }
        }

        filtered.forEach { u ->
            val suspended = MarketplaceLogic.isUserSuspended(u.status, u.suspendedUntil)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HatchSurfaceDark)
                    .border(1.dp, if (suspended) HatchRose500.copy(alpha = 0.5f) else HatchBorderDark, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(
                        model = u.avatar,
                        contentDescription = u.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(HatchCardElevated)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.name, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${u.city} • ${u.dealsCompleted} ${if (isAr) "صفقة" else "deals"} • ${u.reportsCount} ${if (isAr) "بلاغ" else "reports"}", color = HatchTextMuted, fontSize = 11.sp)
                    }
                    Text(
                        text = if (suspended) (if (isAr) "موقوف" else "Suspended") else (if (isAr) "نشط" else "Active"),
                        color = if (suspended) HatchRose500 else HatchEmerald400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (suspended) {
                        Button(
                            onClick = { onUnsuspendClick(u.uid) },
                            colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "فك الإيقاف" else "Unsuspend", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        OutlinedButton(onClick = { onSuspendClick(u) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Block, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "إيقاف مؤقت" else "Suspend", color = HatchAmber400, fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(onClick = { onDeleteClick(u) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = HatchRose500, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuspendUserDialog(
    target: SuspendTarget,
    strings: HatchStrings,
    isAr: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (days: Int, reason: String) -> Unit
) {
    var selectedDays by remember { mutableIntStateOf(7) }
    var customDaysText by remember { mutableStateOf("7") }
    var reason by remember { mutableStateOf(strings.suspensionPresetReasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Text(
                text = if (isAr) "إيقاف حساب: ${target.name}" else "Suspend: ${target.name}",
                color = HatchAmber400,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isAr) "مدة الإيقاف (أيام):" else "Duration (days):", color = HatchTextMuted, fontSize = 11.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 7, 14, 30).forEach { d ->
                        SelectablePill("$d ${if (isAr) "يوم" else "d"}", selectedDays == d) {
                            selectedDays = d
                            customDaysText = d.toString()
                        }
                    }
                }
                HatchField(
                    value = customDaysText,
                    onValueChange = {
                        customDaysText = it.filter { c -> c.isDigit() }
                        selectedDays = customDaysText.toIntOrNull() ?: 7
                    },
                    label = if (isAr) "مدة مخصصة بالأيام" else "Custom Days",
                    keyboardType = KeyboardType.Number
                )
                Text(if (isAr) "سبب الإيقاف الجاهز:" else "Preset Reasons:", color = HatchTextMuted, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    strings.suspensionPresetReasons.forEach { r ->
                        Text(
                            text = "• $r",
                            color = if (reason == r) HatchAmber400 else HatchTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HatchCardElevated)
                                .clickable { reason = r }
                                .padding(8.dp)
                        )
                    }
                }
                HatchField(reason, { reason = it }, if (isAr) "السبب المرسل للمستخدم" else "Reason")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDays.coerceIn(1, 3650), reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HatchRose500, contentColor = Color.White)
            ) { Text(if (isAr) "تأكيد الإيقاف" else "Confirm Suspend", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
private fun CreateSponsoredAdDialog(
    strings: HatchStrings,
    isAr: Boolean,
    onDismiss: () -> Unit,
    onCreate: (SponsoredAdItem) -> Unit
) {
    var advertiser by remember { mutableStateOf("") }
    var titleAr by remember { mutableStateOf("") }
    var descAr by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("https://") }
    var format by remember { mutableStateOf(AdFormat.BANNER.wireValue) }
    var duration by remember { mutableStateOf("8") }

    val videoPermResult = MediaUploadPolicy.canUserPerformUpload(MediaUploadPolicy.UploadAction.SPONSORED_AD_VIDEO, isAdmin = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(if (isAr) "إنشاء حملة إعلانية ممولة" else "New Sponsored Ad", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 15.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isAr) "قوالب إعلانات سودانية جاهزة:" else "Sudan Ad Presets:", color = HatchTextMuted, fontSize = 11.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    strings.sudanAdPresets.forEach { preset ->
                        SelectablePill(preset.advertiser, false) {
                            advertiser = preset.advertiser
                            titleAr = if (isAr) preset.titleAr else preset.titleEn
                            descAr = if (isAr) preset.descAr else preset.descEn
                            imageUrl = preset.imageUrl
                        }
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdFormat.entries.forEach { f ->
                        SelectablePill(f.wireValue.uppercase(Locale.ROOT), format == f.wireValue) { format = f.wireValue }
                    }
                }
                HatchField(advertiser, { advertiser = it }, if (isAr) "اسم المعلن *" else "Advertiser *")
                HatchField(titleAr, { titleAr = it }, if (isAr) "عنوان الإعلان *" else "Ad Title *")
                HatchField(descAr, { descAr = it }, if (isAr) "الوصف" else "Description", singleLine = false, minLines = 2)
                HatchField(imageUrl, { imageUrl = it }, if (isAr) "رابط الصورة (HTTPS)" else "Image URL (HTTPS)")
                HatchField(targetUrl, { targetUrl = it }, if (isAr) "رابط التوجيه" else "Target URL")
                HatchField(duration, { duration = it.filter { c -> c.isDigit() } }, if (isAr) "المدة بالثواني" else "Duration (sec)", KeyboardType.Number)

                Text(
                    text = if (videoPermResult.isSuccess) "Video Upload Ready" else "🔒 ${videoPermResult.exceptionOrNull()?.message}",
                    color = if (videoPermResult.isSuccess) HatchEmerald400 else HatchAmber400,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        SponsoredAdItem(
                            advertiser = advertiser.trim(),
                            titleAr = titleAr.trim(),
                            titleEn = titleAr.trim(),
                            descriptionAr = descAr.trim(),
                            descriptionEn = descAr.trim(),
                            imageUrl = imageUrl.trim(),
                            targetUrl = targetUrl.trim(),
                            format = format,
                            durationSeconds = (duration.toIntOrNull() ?: 8).coerceIn(3, 120),
                            isActive = true
                        )
                    )
                },
                enabled = advertiser.isNotBlank() && titleAr.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) { Text(if (isAr) "إطلاق الإعلان" else "Launch Ad", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}
