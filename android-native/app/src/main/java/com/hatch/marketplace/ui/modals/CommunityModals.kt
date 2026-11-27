package com.hatch.marketplace.ui.modals

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.GuestPromptReason
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.SudaneseCities
import com.hatch.marketplace.data.model.displayLogoOrAvatar
import com.hatch.marketplace.data.model.resolvedAccountType
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
import java.io.File

private val AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80",
    "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=256&q=80",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=256&q=80",
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80"
)

private fun extractLocalSudanDigits(raw: String): String {
    var clean = raw.filter { it.isDigit() }
    if (clean.startsWith("249")) clean = clean.drop(3)
    if (clean.startsWith("0")) clean = clean.drop(1)
    return clean.take(10)
}

@Composable
fun CreateAccountAuthSheet(
    needsOnboarding: Boolean,
    defaultName: String,
    defaultAvatar: String,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    authErrorMessage: String? = null,
    onClearAuthError: () -> Unit = {},
    onDismiss: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onCompleteOnboarding: (
        name: String,
        phone: String,
        city: String,
        avatar: String,
        avatarUri: Uri?,
        accountType: String,
        businessActivity: String,
        businessLogo: String
    ) -> Unit
) {
    val context = LocalContext.current
    val initialAvatar = remember(defaultAvatar) {
        defaultAvatar.takeIf { it.startsWith("https://") } ?: AVATAR_PRESETS.first()
    }
    var selectedAccountType by remember(needsOnboarding) { mutableStateOf<AccountType?>(null) }
    var onboardingStep by remember(needsOnboarding) { mutableIntStateOf(1) }
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var businessActivity by remember { mutableStateOf("") }
    var customLogoUrl by remember { mutableStateOf("") }
    var phoneDigits by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(SudaneseCities.ALL.firstOrNull { it.contains("بورتسودان") } ?: SudaneseCities.ALL.first()) }
    var avatar by remember(initialAvatar) { mutableStateOf(initialAvatar) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(needsOnboarding) {
        validationError = null
        onboardingStep = 1
        selectedAccountType = null
    }

    val signerReady = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            validationError = null
            onClearAuthError()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            selectedAvatarUri = pendingCamUri
            validationError = null
            onClearAuthError()
        }
    }

    val launchCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCamUri = uri
        cameraLauncher.launch(uri)
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else validationError = if (language == AppLanguage.AR) "يرجى منح إذن الكاميرا لالتقاط صورة" else "Camera permission is required"
    }

    val displayedError = authErrorMessage?.takeIf { it.isNotBlank() }
        ?: validationError?.takeIf { it.isNotBlank() }

    val activeType = selectedAccountType ?: AccountType.PERSONAL

    Dialog(
        onDismissRequest = {
            if (!needsOnboarding) {
                onClearAuthError()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = when {
                        !needsOnboarding -> strings.signInTitle
                        onboardingStep == 1 -> strings.accountTypeChooserTitle
                        activeType == AccountType.STORE -> if (language == AppLanguage.AR) "بيانات حساب المتجر" else "Store Account Details"
                        activeType == AccountType.COMPANY -> if (language == AppLanguage.AR) "بيانات حساب الشركة" else "Company Account Details"
                        else -> if (language == AppLanguage.AR) "بيانات الحساب الشخصي" else "Personal Account Details"
                    }
                    Text(
                        text = headerText,
                        color = HatchAmber400,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    if (!needsOnboarding) {
                        IconButton(onClick = {
                            onClearAuthError()
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                        }
                    }
                }

                Text(
                    text = when {
                        !needsOnboarding -> strings.signInSubtitle
                        onboardingStep == 1 -> strings.accountTypeChooserSubtitle
                        activeType == AccountType.STORE -> if (language == AppLanguage.AR) "أدخل بيانات المتجر لعرض منتجاتك للعملاء باسم وهوية متجرك" else "Enter your store details to display products under your brand"
                        activeType == AccountType.COMPANY -> if (language == AppLanguage.AR) "أدخل بيانات الشركة ومجال العمل لإنشاء صفحتك التجارية" else "Enter your company profile and business field"
                        else -> if (language == AppLanguage.AR) "أكمل بياناتك للبدء في البيع والشراء على هتش" else "Complete your details to buy and sell on HATCH"
                    },
                    color = HatchTextSecondary,
                    fontSize = 13.sp
                )

                if (!displayedError.isNullOrBlank()) {
                    Text(
                        text = displayedError,
                        color = HatchRose500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HatchRose500.copy(alpha = 0.14f))
                            .border(1.dp, HatchRose500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    )
                }

                if (!needsOnboarding) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(22.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(HatchAmber500.copy(alpha = 0.14f))
                                    .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(
                                    text = if (language == AppLanguage.AR) "دخول سريع وآمن عبر Google" else "Fast & Secure Google Sign-In",
                                    color = HatchTextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (language == AppLanguage.AR) "سجّل الدخول بحساب Google الموحد لحسابك في هتش" else "Sign in with your Google account for your HATCH profile",
                                    color = HatchTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                validationError = null
                                onClearAuthError()
                                onGoogleSignInClick()
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF111111)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color(0xFF111111), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.signInGoogle, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HatchBgDark)
                                .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (language == AppLanguage.AR) {
                                    "عند دخولك لأول مرة بحساب Google، ستكمل بيانات ملفك في هتش مرة واحدة فقط؛ وفي كل دخول لاحق يُفتح ملفك الحالي تلقائياً."
                                } else {
                                    "On your first Google sign-in, you will complete your HATCH profile once; future sign-ins automatically open your existing profile."
                                },
                                color = HatchTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onClearAuthError()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.browseAsGuest, color = HatchTextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (onboardingStep == 1) {
                    // Step 1: Choose Account Type
                    Spacer(modifier = Modifier.height(4.dp))

                    AccountTypeOptionCard(
                        icon = Icons.Rounded.Person,
                        emoji = "👤",
                        title = strings.accountPersonalTitle,
                        description = strings.accountPersonalDesc,
                        selected = selectedAccountType == AccountType.PERSONAL,
                        onClick = {
                            selectedAccountType = AccountType.PERSONAL
                            validationError = null
                            onClearAuthError()
                            onboardingStep = 2
                        }
                    )

                    AccountTypeOptionCard(
                        icon = Icons.Rounded.Storefront,
                        emoji = "🏪",
                        title = strings.accountStoreTitle,
                        description = strings.accountStoreDesc,
                        selected = selectedAccountType == AccountType.STORE,
                        onClick = {
                            selectedAccountType = AccountType.STORE
                            if (name == defaultName) name = ""
                            validationError = null
                            onClearAuthError()
                            onboardingStep = 2
                        }
                    )

                    AccountTypeOptionCard(
                        icon = Icons.Rounded.Business,
                        emoji = "🏢",
                        title = strings.accountCompanyTitle,
                        description = strings.accountCompanyDesc,
                        selected = selectedAccountType == AccountType.COMPANY,
                        onClick = {
                            selectedAccountType = AccountType.COMPANY
                            if (name == defaultName) name = ""
                            validationError = null
                            onClearAuthError()
                            onboardingStep = 2
                        }
                    )
                } else {
                    // Step 2: Dynamic fields per AccountType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, HatchAmber500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                validationError = null
                                onboardingStep = 1
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(18.dp))
                            Text(
                                text = strings.accountTypeLabel(activeType.wireValue),
                                color = HatchAmber400,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (language == AppLanguage.AR) "تغيير النوع" else "Change Type",
                            color = HatchTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val photoSectionTitle = when (activeType) {
                        AccountType.STORE -> if (language == AppLanguage.AR) "شعار المتجر" else "Store Logo"
                        AccountType.COMPANY -> if (language == AppLanguage.AR) "شعار الشركة" else "Company Logo"
                        AccountType.PERSONAL -> if (language == AppLanguage.AR) "الصورة الشخصية (اختياري)" else "Profile Photo (Optional)"
                    }

                    val effectivePreviewUrl = customLogoUrl.trim().takeIf { activeType.isCommercial() && it.startsWith("https://") } ?: avatar

                    AvatarPickerSection(
                        sectionTitle = photoSectionTitle,
                        avatar = effectivePreviewUrl,
                        selectedAvatarUri = selectedAvatarUri,
                        defaultGooglePhoto = defaultAvatar,
                        signerReady = signerReady,
                        language = language,
                        onSelectPreset = { url ->
                            avatar = url
                            customLogoUrl = ""
                            selectedAvatarUri = null
                            onClearAuthError()
                        },
                        onPickGallery = { galleryLauncher.launch("image/*") },
                        onCaptureCamera = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCamera() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onClearLocalPhoto = { selectedAvatarUri = null }
                    )

                    if (activeType.isCommercial()) {
                        OutlinedTextField(
                            value = customLogoUrl,
                            onValueChange = {
                                customLogoUrl = it
                                selectedAvatarUri = null
                                validationError = null
                                onClearAuthError()
                            },
                            label = {
                                Text(
                                    text = if (language == AppLanguage.AR) "رابط الشعار HTTPS (اختياري)" else "Logo HTTPS URL (Optional)",
                                    color = HatchTextMuted,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            colors = hatchInputColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val nameFieldLabel = when (activeType) {
                        AccountType.STORE -> if (language == AppLanguage.AR) "اسم المتجر *" else "Store Name *"
                        AccountType.COMPANY -> if (language == AppLanguage.AR) "اسم الشركة *" else "Company Name *"
                        AccountType.PERSONAL -> if (language == AppLanguage.AR) "الاسم *" else "Full Name *"
                    }
                    val nameFieldIcon = when (activeType) {
                        AccountType.STORE -> Icons.Rounded.Storefront
                        AccountType.COMPANY -> Icons.Rounded.Business
                        AccountType.PERSONAL -> Icons.Rounded.Person
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                            onClearAuthError()
                        },
                        label = { Text(nameFieldLabel, color = HatchTextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(nameFieldIcon, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = hatchInputColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activeType.isCommercial()) {
                        val activityLabel = if (activeType == AccountType.STORE) {
                            if (language == AppLanguage.AR) "نوع النشاط (مثلاً: هواتف، أزياء، إلكترونيات) *" else "Store Activity *"
                        } else {
                            if (language == AppLanguage.AR) "مجال العمل (مثلاً: طاقة شمسية، استيراد، مقاولات) *" else "Business Field *"
                        }
                        OutlinedTextField(
                            value = businessActivity,
                            onValueChange = {
                                businessActivity = it
                                validationError = null
                                onClearAuthError()
                            },
                            label = { Text(activityLabel, color = HatchTextMuted, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Work, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            colors = hatchInputColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // City Selector
                    Text(if (language == AppLanguage.AR) "المدينة / الولاية *" else "City / State *", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SudaneseCities.ALL.forEach { c ->
                            SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                        }
                    }

                    // Sudan Phone with +249 Badge
                    SudanPhoneInputRow(
                        digits = phoneDigits,
                        language = language,
                        onDigitsChange = {
                            phoneDigits = it
                            validationError = null
                            onClearAuthError()
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(18.dp))
                        Text(
                            text = strings.goldBadgeAdminDesc,
                            color = HatchTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            if (cleanName.length < 2) {
                                validationError = when (activeType) {
                                    AccountType.STORE -> if (language == AppLanguage.AR) "يرجى كتابة اسم المتجر (حرفين فأكثر)" else "Please enter store name"
                                    AccountType.COMPANY -> if (language == AppLanguage.AR) "يرجى كتابة اسم الشركة (حرفين فأكثر)" else "Please enter company name"
                                    AccountType.PERSONAL -> if (language == AppLanguage.AR) "يرجى كتابة الاسم (حرفين فأكثر)" else "Please enter your name"
                                }
                                return@Button
                            }
                            if (activeType.isCommercial() && businessActivity.trim().length < 2) {
                                validationError = if (activeType == AccountType.STORE) {
                                    if (language == AppLanguage.AR) "يرجى تحديد نوع نشاط المتجر" else "Please enter store activity"
                                } else {
                                    if (language == AppLanguage.AR) "يرجى تحديد مجال عمل الشركة" else "Please enter business field"
                                }
                                return@Button
                            }
                            if (phoneDigits.length < 8) {
                                validationError = if (language == AppLanguage.AR) "يرجى إدخال رقم هاتف سوداني صحيح (8-10 أرقام)" else "Please enter a valid Sudan phone number"
                                return@Button
                            }
                            val normalizedPhone = HatchFormatters.normalizeSudanPhone(phoneDigits)
                            val resolvedUrl = customLogoUrl.trim().takeIf { activeType.isCommercial() && it.startsWith("https://") } ?: avatar
                            val logoParam = if (activeType.isCommercial()) resolvedUrl else ""
                            onCompleteOnboarding(
                                cleanName,
                                normalizedPhone,
                                city,
                                resolvedUrl,
                                selectedAvatarUri,
                                activeType.wireValue,
                                if (activeType.isCommercial()) businessActivity.trim() else "",
                                logoParam
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = HatchBgDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(strings.saveAndContinue, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTypeOptionCard(
    icon: ImageVector,
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) HatchAmber500.copy(alpha = 0.14f) else HatchSurfaceDark)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) HatchAmber500 else HatchBorderDark,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(HatchCardElevated)
                .border(1.dp, if (selected) HatchAmber500 else HatchBorderDark, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "$emoji $title",
                color = HatchTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = description,
                color = HatchTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SudanPhoneInputRow(
    digits: String,
    language: AppLanguage,
    onDigitsChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (language == AppLanguage.AR) "رقم الهاتف السوداني *" else "Sudan Phone Number *",
            color = HatchTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HatchSurfaceDark)
                        .border(1.dp, HatchBorderDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🇸🇩", fontSize = 16.sp)
                    Text(
                        text = "+249",
                        color = HatchAmber400,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }

                OutlinedTextField(
                    value = digits,
                    onValueChange = { input ->
                        val cleaned = extractLocalSudanDigits(input)
                        onDigitsChange(cleaned)
                    },
                    placeholder = { Text("912345678", color = HatchTextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(17.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = hatchInputColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = if (language == AppLanguage.AR) "أدخل رقم الهاتف بدون +249 (مثال: 912345678) للتواصل المباشر عند المعاينة" else "Enter digits after +249 (e.g. 912345678) for deal contact",
            color = HatchTextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AvatarPickerSection(
    sectionTitle: String,
    avatar: String,
    selectedAvatarUri: Uri?,
    defaultGooglePhoto: String,
    signerReady: Boolean,
    language: AppLanguage,
    onSelectPreset: (String) -> Unit,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onClearLocalPhoto: () -> Unit
) {
    val activePreviewModel: Any = selectedAvatarUri ?: avatar
    val presetChoices = remember(defaultGooglePhoto) {
        if (defaultGooglePhoto.startsWith("https://") && !AVATAR_PRESETS.contains(defaultGooglePhoto)) {
            listOf(defaultGooglePhoto) + AVATAR_PRESETS
        } else {
            AVATAR_PRESETS
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = sectionTitle,
            color = HatchTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.size(62.dp)) {
                AsyncImage(
                    model = activePreviewModel,
                    contentDescription = "Selected Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.5.dp, HatchAmber500, CircleShape)
                )
                if (selectedAvatarUri != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(HatchRose500)
                            .clickable { onClearLocalPhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetChoices.forEach { preset ->
                    val isSelected = selectedAvatarUri == null && avatar == preset
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) HatchAmber400 else HatchBorderDark,
                                shape = CircleShape
                            )
                            .clickable { onSelectPreset(preset) }
                    ) {
                        AsyncImage(
                            model = preset,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HatchAmber500.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCaptureCamera,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(if (language == AppLanguage.AR) "التقاط صورة" else "Camera", color = HatchEmerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPickGallery,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(if (language == AppLanguage.AR) "من المعرض" else "Gallery", color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (selectedAvatarUri != null && !signerReady) {
            Text(
                text = if (language == AppLanguage.AR) {
                    "تنبيه: رفع صورة من الجهاز يتطلب تفعيل MEDIA_SIGNER_URL السحابي؛ يمكنك اختيار إحدى الصور الجاهزة أو صورة Google."
                } else {
                    "Note: Device photo upload requires MEDIA_SIGNER_URL; select a preset or Google photo to save now."
                },
                color = HatchAmber400,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    user: PublicUser,
    currentPhone: String,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        city: String,
        avatar: String,
        avatarUri: Uri?,
        accountType: String,
        businessActivity: String,
        businessLogo: String
    ) -> Unit
) {
    val context = LocalContext.current
    val initialType = remember(user.uid, user.accountType) { user.resolvedAccountType() }
    var accountType by remember(user.uid) { mutableStateOf(initialType) }
    var name by remember(user.uid) { mutableStateOf(user.name) }
    var businessActivity by remember(user.uid) { mutableStateOf(user.businessActivity.orEmpty()) }
    var customLogoUrl by remember(user.uid) { mutableStateOf(user.businessLogo.orEmpty()) }
    var phoneDigits by remember(user.uid) { mutableStateOf(extractLocalSudanDigits(currentPhone)) }
    var city by remember(user.uid) { mutableStateOf(user.city.ifBlank { SudaneseCities.ALL.first() }) }
    var avatar by remember(user.uid) { mutableStateOf(user.displayLogoOrAvatar().ifBlank { AVATAR_PRESETS.first() }) }
    var selectedAvatarUri by remember(user.uid) { mutableStateOf<Uri?>(null) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }

    val signerReady = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedAvatarUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) selectedAvatarUri = pendingCamUri
    }

    val launchCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "avatar_edit_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCamUri = uri
        cameraLauncher.launch(uri)
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    val effectivePreviewUrl = customLogoUrl.trim().takeIf { accountType.isCommercial() && it.startsWith("https://") } ?: avatar

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Text(
                text = if (initialType == AccountType.PERSONAL && accountType.isCommercial()) {
                    strings.upgradeAccountTitle
                } else {
                    if (language == AppLanguage.AR) "تعديل الحساب والملف الشخصي" else "Edit Profile & Account"
                },
                color = HatchAmber400,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HatchCardElevated)
                        .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (initialType == AccountType.PERSONAL) {
                                strings.upgradeAccountTitle
                            } else {
                                if (language == AppLanguage.AR) "نوع الحساب" else "Account Type"
                            },
                            color = HatchAmber400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountType.entries.forEach { t ->
                            SelectablePill(strings.accountTypeLabel(t.wireValue), accountType == t) {
                                accountType = t
                            }
                        }
                    }
                    if (initialType == AccountType.PERSONAL && accountType.isCommercial()) {
                        Text(
                            text = if (language == AppLanguage.AR) {
                                "تتم الترقية على نفس حسابك وتبقى صفقاتك ومنتجاتك والتوثيق كما هي."
                            } else {
                                "Upgrading keeps your exact UID, deals, listings, and verification intact."
                            },
                            color = HatchEmerald400,
                            fontSize = 10.sp
                        )
                    }
                }

                AvatarPickerSection(
                    sectionTitle = when (accountType) {
                        AccountType.STORE -> if (language == AppLanguage.AR) "شعار المتجر" else "Store Logo"
                        AccountType.COMPANY -> if (language == AppLanguage.AR) "شعار الشركة" else "Company Logo"
                        AccountType.PERSONAL -> if (language == AppLanguage.AR) "صورة الملف الشخصي" else "Profile Photo"
                    },
                    avatar = effectivePreviewUrl,
                    selectedAvatarUri = selectedAvatarUri,
                    defaultGooglePhoto = user.avatar,
                    signerReady = signerReady,
                    language = language,
                    onSelectPreset = { url ->
                        avatar = url
                        customLogoUrl = ""
                        selectedAvatarUri = null
                    },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onCaptureCamera = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onClearLocalPhoto = { selectedAvatarUri = null }
                )

                if (accountType.isCommercial()) {
                    HatchField(
                        value = customLogoUrl,
                        onValueChange = {
                            customLogoUrl = it
                            selectedAvatarUri = null
                        },
                        label = if (language == AppLanguage.AR) "رابط الشعار HTTPS (اختياري)" else "Logo HTTPS URL (Optional)"
                    )
                }

                HatchField(
                    value = name,
                    onValueChange = { name = it },
                    label = when (accountType) {
                        AccountType.STORE -> if (language == AppLanguage.AR) "اسم المتجر *" else "Store Name *"
                        AccountType.COMPANY -> if (language == AppLanguage.AR) "اسم الشركة *" else "Company Name *"
                        AccountType.PERSONAL -> if (language == AppLanguage.AR) "الاسم *" else "Name *"
                    }
                )

                if (accountType.isCommercial()) {
                    HatchField(
                        value = businessActivity,
                        onValueChange = { businessActivity = it },
                        label = if (accountType == AccountType.STORE) {
                            if (language == AppLanguage.AR) "نوع النشاط *" else "Store Activity *"
                        } else {
                            if (language == AppLanguage.AR) "مجال العمل *" else "Business Field *"
                        }
                    )
                }

                SudanPhoneInputRow(
                    digits = phoneDigits,
                    language = language,
                    onDigitsChange = { phoneDigits = it }
                )

                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SudaneseCities.ALL.forEach { c ->
                        SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                    }
                }
            }
        },
        confirmButton = {
            val activityValid = !accountType.isCommercial() || businessActivity.trim().length >= 2
            Button(
                onClick = {
                    val resolvedUrl = customLogoUrl.trim().takeIf { accountType.isCommercial() && it.startsWith("https://") } ?: avatar
                    onSave(
                        name.trim(),
                        HatchFormatters.normalizeSudanPhone(phoneDigits),
                        city,
                        resolvedUrl,
                        selectedAvatarUri,
                        accountType.wireValue,
                        if (accountType.isCommercial()) businessActivity.trim() else "",
                        if (accountType.isCommercial()) resolvedUrl else ""
                    )
                },
                enabled = !isSubmitting && name.trim().length >= 2 && phoneDigits.length >= 8 && activityValid,
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
    var stars by remember(request.sellerId, request.productId, request.initialRating, request.isAlreadyReviewed) {
        mutableIntStateOf(request.initialRating.coerceIn(0, 5))
    }
    var comment by remember(request.sellerId, request.productId, request.existingComment) {
        mutableStateOf(request.existingComment)
    }
    val chosenTags = remember(request.sellerId, request.productId, request.existingTags) {
        mutableStateListOf<String>().apply { addAll(request.existingTags) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text("${strings.rateSeller}: ${request.sellerName}", color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { idx ->
                        val selected = idx <= stars
                        Icon(
                            imageVector = if (selected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "$idx",
                            tint = if (selected) HatchAmber400 else HatchTextMuted,
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (!request.isAlreadyReviewed) Modifier.clickable { stars = idx }
                                    else Modifier
                                )
                        )
                    }
                }
                if (request.isAlreadyReviewed) {
                    Text(
                        text = if (language == AppLanguage.AR) {
                            "تقييمك السابق: $stars/5 — تم اعتماد التقييم مسبقاً"
                        } else {
                            "Your previous rating: $stars/5 — already submitted"
                        },
                        color = HatchTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (comment.isNotBlank()) {
                        Text(
                            text = comment,
                            color = HatchTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                } else {
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
            }
        },
        confirmButton = {
            if (!request.isAlreadyReviewed) {
                Button(
                    onClick = { onSubmit(stars, comment, chosenTags.toList()) },
                    enabled = !isSubmitting && stars in 1..5 && comment.trim().length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
                ) { Text(if (language == AppLanguage.AR) "إرسال التقييم" else "Submit", fontWeight = FontWeight.Black) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (request.isAlreadyReviewed) {
                        if (language == AppLanguage.AR) "إغلاق" else "Close"
                    } else {
                        if (language == AppLanguage.AR) "إلغاء" else "Cancel"
                    },
                    color = HatchTextMuted
                )
            }
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
    onSubmit: (reason: String, message: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(strings.reportReasons.first().first) }
    var message by remember { mutableStateOf("") }

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
                Text(
                    text = if (language == AppLanguage.AR) {
                        "سيتم إرسال البلاغ للمشرفين لمراجعته واتخاذ الإجراء المناسب."
                    } else {
                        "Your report will be reviewed by HATCH moderators."
                    },
                    color = HatchTextMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, message) },
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
fun VerificationRequestDialog(
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSubmit: (note: String, documentUrl: String?) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var docUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = HatchAmber400)
                Text(strings.requestGoldVerification, color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.goldBadgeAdminDesc,
                    color = HatchTextSecondary,
                    fontSize = 12.sp
                )
                HatchField(
                    value = note,
                    onValueChange = { note = it },
                    label = strings.verificationNoteLabel,
                    singleLine = false,
                    minLines = 3
                )
                HatchField(
                    value = docUrl,
                    onValueChange = { docUrl = it },
                    label = strings.verificationDocUrlLabel
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(note.trim(), docUrl.trim().takeIf { it.isNotBlank() }) },
                enabled = !isSubmitting && note.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = HatchBgDark, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(strings.submitVerificationBtn, fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel", color = HatchTextMuted)
            }
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
                Text(
                    text = if (language == AppLanguage.AR) "تطبيق هتش الأصلي (Android)" else "HATCH Android Native",
                    color = HatchAmber400,
                    fontWeight = FontWeight.Black
                )
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
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            if (language == AppLanguage.AR) "مشاركة هتش" else "Share HATCH"
                        )
                    )
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

@Composable
fun GuestAuthPromptDialog(
    reason: GuestPromptReason,
    language: AppLanguage,
    strings: HatchStrings,
    onContinueWithGoogle: () -> Unit,
    onDismiss: () -> Unit
) {
    val promptBody = strings.guestPromptMessage(reason)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            color = HatchSurfaceDark,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, HatchAmber500.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HatchAmber500.copy(alpha = 0.14f))
                        .border(1.dp, HatchAmber500.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = HatchAmber400,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = strings.guestDialogTitle,
                    color = HatchAmber400,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = promptBody,
                    color = HatchTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onContinueWithGoogle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HatchAmber500,
                        contentColor = HatchBgDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.signInGoogle,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.guestDialogKeepBrowsing,
                        color = HatchTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
