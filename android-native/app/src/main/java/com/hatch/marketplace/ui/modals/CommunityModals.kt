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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    onDismiss: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onCompleteOnboarding: (name: String, phone: String, city: String, avatar: String, avatarUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val initialAvatar = remember(defaultAvatar) {
        defaultAvatar.takeIf { it.startsWith("https://") } ?: AVATAR_PRESETS.first()
    }
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var phoneDigits by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(SudaneseCities.ALL.firstOrNull { it.contains("بورتسودان") } ?: SudaneseCities.ALL.first()) }
    var avatar by remember(initialAvatar) { mutableStateOf(initialAvatar) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val signerReady = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            validationError = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            selectedAvatarUri = pendingCamUri
            validationError = null
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

    Dialog(
        onDismissRequest = onDismiss,
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

                Text(
                    text = if (needsOnboarding) {
                        if (language == AppLanguage.AR) "أكمل بياناتك للبدء في البيع والشراء على هتش" else "Complete your seller profile to trade on HATCH"
                    } else {
                        strings.signInSubtitle
                    },
                    color = HatchTextSecondary,
                    fontSize = 13.sp
                )

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
                                    text = if (language == AppLanguage.AR) "اختر حساب Google الخاص بك من الجهاز للمتابعة" else "Choose your Google account on this device to continue",
                                    color = HatchTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onGoogleSignInClick,
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
                                    "بعد اختيار حساب Google، ستكمل الاسم ورقم الهاتف السوداني والمدينة والصورة مرة واحدة فقط بدون رسائل SMS."
                                } else {
                                    "After picking your Google account, you will complete your name, Sudan phone, city, and avatar once without SMS fees."
                                },
                                color = HatchTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.browseAsGuest, color = HatchTextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    if (!validationError.isNullOrBlank()) {
                        Text(
                            text = validationError!!,
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

                    // Avatar Picker + Live Preview
                    AvatarPickerSection(
                        avatar = avatar,
                        selectedAvatarUri = selectedAvatarUri,
                        defaultGooglePhoto = defaultAvatar,
                        signerReady = signerReady,
                        language = language,
                        onSelectPreset = { url ->
                            avatar = url
                            selectedAvatarUri = null
                        },
                        onPickGallery = { galleryLauncher.launch("image/*") },
                        onCaptureCamera = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCamera() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onClearLocalPhoto = { selectedAvatarUri = null }
                    )

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text(if (language == AppLanguage.AR) "الاسم الكامل / اسم المتجر *" else "Full Name *", color = HatchTextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = hatchInputColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sudan Phone with +249 Badge
                    SudanPhoneInputRow(
                        digits = phoneDigits,
                        language = language,
                        onDigitsChange = {
                            phoneDigits = it
                            validationError = null
                        }
                    )

                    // City Selector
                    Text(if (language == AppLanguage.AR) "المدينة / الولاية *" else "City / State *", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SudaneseCities.ALL.forEach { c ->
                            SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                        }
                    }

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
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (language == AppLanguage.AR) {
                                "شارة التوثيق الذهبية تظهر تلقائياً بعد إتمام 10 صفقات بيع ناجحة وموثقة."
                            } else {
                                "The verified badge unlocks automatically after 10 trusted completed deals."
                            },
                            color = HatchTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            if (cleanName.length < 2) {
                                validationError = if (language == AppLanguage.AR) "يرجى كتابة الاسم الكامل (حرفين فأكثر)" else "Please enter your full name"
                                return@Button
                            }
                            if (phoneDigits.length < 8) {
                                validationError = if (language == AppLanguage.AR) "يرجى إدخال رقم هاتف سوداني صحيح (8-10 أرقام)" else "Please enter a valid Sudan phone number"
                                return@Button
                            }
                            val normalizedPhone = HatchFormatters.normalizeSudanPhone(phoneDigits)
                            onCompleteOnboarding(cleanName, normalizedPhone, city, avatar, selectedAvatarUri)
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
            text = if (language == AppLanguage.AR) "صورة الملف الشخصي" else "Profile Photo",
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
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, city: String, avatar: String, avatarUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var name by remember(user.uid) { mutableStateOf(user.name) }
    var phoneDigits by remember(user.uid) { mutableStateOf(extractLocalSudanDigits(currentPhone)) }
    var city by remember(user.uid) { mutableStateOf(user.city.ifBlank { SudaneseCities.ALL.first() }) }
    var avatar by remember(user.uid) { mutableStateOf(user.avatar.ifBlank { AVATAR_PRESETS.first() }) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(if (language == AppLanguage.AR) "تعديل الملف الشخصي" else "Edit Profile", color = HatchAmber400, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarPickerSection(
                    avatar = avatar,
                    selectedAvatarUri = selectedAvatarUri,
                    defaultGooglePhoto = user.avatar,
                    signerReady = signerReady,
                    language = language,
                    onSelectPreset = { url ->
                        avatar = url
                        selectedAvatarUri = null
                    },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onCaptureCamera = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onClearLocalPhoto = { selectedAvatarUri = null }
                )
                HatchField(name, { name = it }, if (language == AppLanguage.AR) "الاسم" else "Name")
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
            Button(
                onClick = { onSave(name, HatchFormatters.normalizeSudanPhone(phoneDigits), city, avatar, selectedAvatarUri) },
                enabled = !isSubmitting && name.trim().length >= 2 && phoneDigits.length >= 8,
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
