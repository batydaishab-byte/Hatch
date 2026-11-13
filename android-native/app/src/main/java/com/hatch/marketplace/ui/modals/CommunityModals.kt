package com.hatch.marketplace.ui.modals

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.SudaneseCities
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.state.ReportRequest
import com.hatch.marketplace.ui.state.ReviewRequest
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary

private val AVATAR_PRESETS = listOf(
    "https://api.dicebear.com/9.x/avataaars/png?seed=SudanTrader1",
    "https://api.dicebear.com/9.x/avataaars/png?seed=SudanTrader2",
    "https://api.dicebear.com/9.x/avataaars/png?seed=SudanTrader3",
    "https://api.dicebear.com/9.x/avataaars/png?seed=SudanTrader4",
    "https://api.dicebear.com/9.x/avataaars/png?seed=SudanTrader5"
)

@Composable
fun CreateAccountAuthSheet(
    needsOnboarding: Boolean,
    defaultName: String,
    defaultEmail: String,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailSignUp: (email: String, pass: String, name: String, phone: String, city: String, avatar: String) -> Unit,
    onCompleteOnboarding: (name: String, phone: String, city: String, avatar: String) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(defaultEmail) }
    var password by remember { mutableStateOf("") }
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var phone by remember { mutableStateOf("+249") }
    var city by remember { mutableStateOf(SudaneseCities.ALL.first()) }
    var avatar by remember { mutableStateOf(AVATAR_PRESETS.first()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (needsOnboarding) strings.createAccountTitle else strings.signInTitle,
                        color = HatchAmber400,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (!needsOnboarding) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                        }
                    }
                }

                Text(strings.signInSubtitle, color = HatchTextSecondary, fontSize = 13.sp)

                if (!needsOnboarding) {
                    Button(
                        onClick = onGoogleSignInClick,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(strings.signInGoogle, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectablePill(strings.signInEmail, !isSignUpMode) { isSignUpMode = false }
                        SelectablePill(strings.signUpEmail, isSignUpMode) { isSignUpMode = true }
                    }

                    HatchField(email, { email = it }, if (language == AppLanguage.AR) "البريد الإلكتروني" else "Email Address", KeyboardType.Email)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (language == AppLanguage.AR) "كلمة المرور (6 أحرف فأكثر)" else "Password (min 6 chars)", color = HatchTextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = hatchInputColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (needsOnboarding || isSignUpMode) {
                    Text(if (language == AppLanguage.AR) "اختر صورتك الشخصية" else "Pick Profile Avatar", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AVATAR_PRESETS.forEach { preset ->
                            AsyncImage(
                                model = preset,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(if (avatar == preset) 2.5.dp else 1.dp, if (avatar == preset) HatchAmber500 else HatchBorderDark, CircleShape)
                                    .clickable { avatar = preset }
                            )
                        }
                    }

                    HatchField(name, { name = it }, if (language == AppLanguage.AR) "الاسم الكامل / اسم المتجر *" else "Display Name *")
                    HatchField(phone, { phone = it }, if (language == AppLanguage.AR) "رقم الهاتف السوداني (+249) *" else "Sudan Phone (+249) *", KeyboardType.Phone)

                    Text(if (language == AppLanguage.AR) "المدينة" else "City", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SudaneseCities.ALL.forEach { c ->
                            SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val normalizedPhone = HatchFormatters.normalizeSudanPhone(phone)
                        when {
                            needsOnboarding -> onCompleteOnboarding(name, normalizedPhone, city, avatar)
                            isSignUpMode -> onEmailSignUp(email, password, name, normalizedPhone, city, avatar)
                            else -> onEmailSignIn(email, password)
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = HatchBgDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        val btnLabel = when {
                            needsOnboarding -> strings.createAccountTitle
                            isSignUpMode -> strings.signUpEmail
                            else -> strings.signInEmail
                        }
                        Text(btnLabel, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    user: PublicUser,
    currentPhone: String,
    isSubmitting: Boolean,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, city: String, avatar: String) -> Unit
) {
    var name by remember(user.uid) { mutableStateOf(user.name) }
    var phone by remember(user.uid) { mutableStateOf(currentPhone.ifBlank { "+249" }) }
    var city by remember(user.uid) { mutableStateOf(user.city.ifBlank { SudaneseCities.ALL.first() }) }
    var avatar by remember(user.uid) { mutableStateOf(user.avatar.ifBlank { AVATAR_PRESETS.first() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(if (language == AppLanguage.AR) "تعديل الملف الشخصي" else "Edit Profile", color = HatchAmber400, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_PRESETS.forEach { preset ->
                        AsyncImage(
                            model = preset,
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(if (avatar == preset) 2.dp else 1.dp, if (avatar == preset) HatchAmber500 else HatchBorderDark, CircleShape)
                                .clickable { avatar = preset }
                        )
                    }
                }
                HatchField(name, { name = it }, if (language == AppLanguage.AR) "الاسم" else "Name")
                HatchField(phone, { phone = it }, if (language == AppLanguage.AR) "رقم الهاتف (+249)" else "Phone (+249)", KeyboardType.Phone)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SudaneseCities.ALL.forEach { c ->
                        SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, HatchFormatters.normalizeSudanPhone(phone), city, avatar) },
                enabled = !isSubmitting && name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) { Text(if (language == AppLanguage.AR) "حفظ" else "Save", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
fun UserRatingModal(
    request: ReviewRequest,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, tags: List<String>) -> Unit
) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val chosenTags = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text("${strings.rateSeller}: ${request.sellerName}", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { idx ->
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "$idx",
                            tint = if (idx <= stars) HatchAmber400 else HatchTextMuted,
                            modifier = Modifier.size(32.dp).clickable { stars = idx }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    strings.reviewTags.forEach { (arTag, enTag) ->
                        val label = if (language == AppLanguage.AR) arTag else enTag
                        val selected = chosenTags.contains(label)
                        SelectablePill(label, selected) {
                            if (selected) chosenTags.remove(label) else chosenTags.add(label)
                        }
                    }
                }
                HatchField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = if (language == AppLanguage.AR) "اكتب رأيك في التعامل *" else "Write your review *",
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(stars, comment, chosenTags.toList()) },
                enabled = !isSubmitting && comment.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) { Text(if (language == AppLanguage.AR) "إرسال التقييم" else "Submit", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
fun ReportViolationModal(
    request: ReportRequest,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, message: String, flagListing: Boolean) -> Unit
) {
    var selectedReason by remember { mutableStateOf(strings.reportReasons.first().first) }
    var message by remember { mutableStateOf("") }
    var flagListing by remember { mutableStateOf(request.productId != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = HatchRose500)
                Text(strings.reportAd, color = HatchRose500, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    strings.reportReasons.forEach { (key, label) ->
                        SelectablePill(label, selectedReason == key) { selectedReason = key }
                    }
                }
                Text(if (language == AppLanguage.AR) "جمل جاهزة:" else "Quick phrases:", color = HatchTextMuted, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    strings.reportQuickPhrases.forEach { phrase ->
                        Text(
                            text = "• $phrase",
                            color = HatchAmber400,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HatchCardElevated)
                                .clickable { message = phrase }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                HatchField(
                    value = message,
                    onValueChange = { message = it },
                    label = if (language == AppLanguage.AR) "تفاصيل البلاغ (5 أحرف فأكثر) *" else "Details (min 5 chars) *",
                    singleLine = false,
                    minLines = 3
                )
                if (request.productId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = flagListing,
                            onCheckedChange = { flagListing = it },
                            colors = CheckboxDefaults.colors(checkedColor = HatchRose500)
                        )
                        Text(
                            text = if (language == AppLanguage.AR) "تمييز الإعلان للمراجعة الفورية" else "Flag listing for urgent review",
                            color = HatchTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, message, flagListing) },
                enabled = !isSubmitting && message.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = HatchRose500, contentColor = Color.White)
            ) { Text(if (language == AppLanguage.AR) "إرسال البلاغ" else "Submit Report", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
fun NotificationsModal(
    notifications: List<NotificationItem>,
    readBroadcastIds: Set<String>,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(HatchSurfaceDark).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = HatchAmber400)
                        Text(strings.notificationsTitle, color = HatchTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onMarkAllRead) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.markAllRead, color = HatchEmerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = null, tint = HatchTextPrimary) }
                    }
                }
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (language == AppLanguage.AR) "لا توجد إشعارات حالياً" else "No notifications yet", color = HatchTextMuted)
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        notifications.forEach { n ->
                            val isRead = n.read || readBroadcastIds.contains(n.id)
                            val title = if (language == AppLanguage.AR) n.titleAr else n.titleEn.ifBlank { n.titleAr }
                            val body = if (language == AppLanguage.AR) n.bodyAr else n.bodyEn.ifBlank { n.bodyAr }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isRead) HatchSurfaceDark else HatchAmber500.copy(alpha = 0.08f))
                                    .border(1.dp, if (isRead) HatchBorderDark else HatchAmber500.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(title, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(body, color = HatchTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyGuideModal(language: AppLanguage, strings: HatchStrings, onDismiss: () -> Unit) {
    val rulesAr = listOf(
        "لا تحوّل أي عربون عبر بنكك أو فوري أو أوكاش قبل المعاينة المباشرة يداً بيد.",
        "اختر دائماً مكاناً عاماً ومضاءً للمعاينة (الأسواق المركزية، المجمعات، أو أمام مراكز معتمدة).",
        "افحص رقم IMEI والبطارية والشاحن الأصلي عند شراء الهواتف والإلكترونيات.",
        "تأكد من مطابقة إشعار التحويل البنكي داخل تطبيق بنكك الفعلي وليس عبر صور مرسلة.",
        "أبلغ فوراً عبر زر الإبلاغ في أي إعلان أو محادثة تطلب دفعاً مسبقاً."
    )
    val rulesEn = listOf(
        "Never transfer any Bankak, Fawry, or O-Cash deposit before inspecting the item in person.",
        "Always meet in busy public places during daytime for item inspection.",
        "Verify IMEI, battery health, and original accessories for electronics.",
        "Confirm incoming Bankak transfers inside your own banking app — not screenshots.",
        "Report any listing or chat asking for advance payment immediately."
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = HatchEmerald400)
                Text(strings.safetyTitle, color = HatchEmerald400, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (language == AppLanguage.AR) rulesAr else rulesEn).forEachIndexed { i, rule ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${i + 1}.", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text(rule, color = HatchTextPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White)) {
                Text(if (language == AppLanguage.AR) "فهمت" else "Got It", fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
fun NativeAppInfoDialog(language: AppLanguage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Android, contentDescription = null, tint = HatchEmerald400)
                Text("HATCH Android Native", color = HatchAmber400, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.AR) {
                        "أنت تستخدم تطبيق هتش الأصلي لنظام Android (Kotlin + Jetpack Compose) — الإصدار ${BuildConfig.VERSION_NAME}."
                    } else {
                        "You are running the native HATCH Android application (Kotlin + Jetpack Compose) — v${BuildConfig.VERSION_NAME}."
                    },
                    color = HatchTextPrimary,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (language == AppLanguage.AR) "مزامنة سحابية فورية + ضغط صور WebP + تخزين مؤقت بدون إنترنت" else "Cloud sync + WebP compression + offline cache",
                        color = HatchTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "تطبيق هتش للسوق السوداني الذكي:\nhttps://batydaishab-byte.github.io/Hatch/"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share HATCH"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (language == AppLanguage.AR) "مشاركة هتش" else "Share App", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.AR) "إغلاق" else "Close", color = HatchTextMuted) }
        }
    )
}
