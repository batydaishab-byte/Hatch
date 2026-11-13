package com.hatch.marketplace.ui.modals

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.media.MediaUploadPolicy
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CategoryId
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductCondition
import com.hatch.marketplace.data.model.SudaneseCities
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
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import java.io.File

@Composable
fun SellModal(
    defaultCity: String,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onPublish: (
        titleAr: String,
        titleEn: String,
        descriptionAr: String,
        descriptionEn: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        condition: String,
        city: String,
        areaAr: String,
        localUris: List<Uri>,
        remoteUrls: List<String>,
        paymentMethods: List<String>,
        negotiable: Boolean,
        deliveryAvailable: Boolean,
        urgentSale: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var titleAr by remember { mutableStateOf("") }
    var descriptionAr by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var originalPriceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CategoryId.PHONES.wireValue) }
    var condition by remember { mutableStateOf(ProductCondition.GOOD.wireValue) }
    var city by remember { mutableStateOf(SudaneseCities.ALL.firstOrNull { it == defaultCity } ?: SudaneseCities.ALL.first()) }
    var areaAr by remember { mutableStateOf("") }
    var negotiable by remember { mutableStateOf(true) }
    var deliveryAvailable by remember { mutableStateOf(false) }
    var urgentSale by remember { mutableStateOf(false) }
    val selectedPayments = remember { mutableStateListOf(PaymentMethod.BANKAK.wireValue, PaymentMethod.CASH.wireValue) }

    val localUris = remember { mutableStateListOf<Uri>() }
    val remoteUrls = remember { mutableStateListOf<String>() }
    var urlInput by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val room = MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING - (localUris.size + remoteUrls.size)
        if (room > 0) localUris.addAll(uris.take(room))
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                if (localUris.size + remoteUrls.size < MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING) {
                    localUris.add(uri)
                }
            }
        }
    }

    val launchCameraWithUri = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "hatch_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCameraWithUri()
    }

    val signerReady = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(HatchSurfaceDark)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                    }
                    Text(strings.postAdTitle, color = HatchAmber400, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photos Section
                    Text(
                        text = if (language == AppLanguage.AR) "صور المنتج (حد أقصى 5 صور — تُضغط تلقائياً إلى WebP)" else "Product Photos (max 5 — compressed to WebP)",
                        color = HatchTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!signerReady) {
                        Text(
                            text = if (language == AppLanguage.AR) "ملاحظة: رفع صور الهاتف مباشرة يتطلب تفعيل MEDIA_SIGNER_URL، أو يمكنك إضافة روابط HTTPS مباشرة للصور." else "Note: Device photo upload requires MEDIA_SIGNER_URL, or paste direct HTTPS image URLs.",
                            color = HatchAmber400,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.galleryPick, color = HatchAmber400, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (hasCam) launchCameraWithUri() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.cameraCapture, color = HatchEmerald400, fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("https://.../image.webp", color = HatchTextMuted, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            colors = hatchInputColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = urlInput.trim()
                                if (clean.startsWith("https://") && localUris.size + remoteUrls.size < 5) {
                                    remoteUrls.add(clean)
                                    urlInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HatchCardElevated, contentColor = HatchAmber400),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Black)
                        }
                    }

                    if (localUris.isNotEmpty() || remoteUrls.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            localUris.forEachIndexed { idx, uri ->
                                PhotoPreviewThumb(model = uri) { localUris.removeAt(idx) }
                            }
                            remoteUrls.forEachIndexed { idx, url ->
                                PhotoPreviewThumb(model = url) { remoteUrls.removeAt(idx) }
                            }
                        }
                    }

                    HatchField(
                        value = titleAr,
                        onValueChange = { titleAr = it },
                        label = if (language == AppLanguage.AR) "عنوان الإعلان *" else "Listing Title *"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HatchField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = if (language == AppLanguage.AR) "السعر (ج.س) *" else "Price (SDG) *",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        HatchField(
                            value = originalPriceText,
                            onValueChange = { originalPriceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = if (language == AppLanguage.AR) "السعر السابق (اختياري)" else "Original Price (Opt)",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(if (language == AppLanguage.AR) "القسم" else "Category", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryId.entries.forEach { cat ->
                            SelectablePill(strings.categoryLabel(cat.wireValue), category == cat.wireValue) { category = cat.wireValue }
                        }
                    }

                    Text(strings.conditionLabel, color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProductCondition.entries.forEach { cond ->
                            SelectablePill(strings.conditionLabel(cond.wireValue), condition == cond.wireValue) { condition = cond.wireValue }
                        }
                    }

                    Text(if (language == AppLanguage.AR) "المدينة" else "City", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SudaneseCities.ALL.forEach { c ->
                            SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                        }
                    }

                    HatchField(
                        value = areaAr,
                        onValueChange = { areaAr = it },
                        label = if (language == AppLanguage.AR) "الحي / المنطقة (مثلاً: الرياض / أركويت)" else "Neighborhood / Area"
                    )

                    Text(if (language == AppLanguage.AR) "طرق الدفع المقبولة *" else "Accepted Payment Methods *", color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { pm ->
                            val selected = selectedPayments.contains(pm.wireValue)
                            SelectablePill(strings.paymentLabel(pm.wireValue), selected) {
                                if (selected && selectedPayments.size > 1) selectedPayments.remove(pm.wireValue)
                                else if (!selected) selectedPayments.add(pm.wireValue)
                            }
                        }
                    }

                    HatchSwitchRow(strings.negotiableBadge, negotiable) { negotiable = it }
                    HatchSwitchRow(strings.deliveryBadge, deliveryAvailable) { deliveryAvailable = it }
                    HatchSwitchRow(strings.urgentBadge, urgentSale) { urgentSale = it }

                    HatchField(
                        value = descriptionAr,
                        onValueChange = { descriptionAr = it },
                        label = if (language == AppLanguage.AR) "الوصف الكامل وحالة السلعة (5 أحرف فأكثر) *" else "Full Description (min 5 chars) *",
                        singleLine = false,
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(HatchSurfaceDark)
                        .padding(16.dp)
                ) {
                    val priceVal = priceText.toDoubleOrNull() ?: 0.0
                    val canSubmit = !isSubmitting && titleAr.trim().length >= 2 &&
                        descriptionAr.trim().length >= 5 && priceVal > 0 &&
                        (localUris.isNotEmpty() || remoteUrls.isNotEmpty())
                    Button(
                        onClick = {
                            onPublish(
                                titleAr,
                                titleAr,
                                descriptionAr,
                                descriptionAr,
                                priceVal,
                                originalPriceText.toDoubleOrNull(),
                                category,
                                condition,
                                city,
                                areaAr,
                                localUris.toList(),
                                remoteUrls.toList(),
                                selectedPayments.toList(),
                                negotiable,
                                deliveryAvailable,
                                urgentSale
                            )
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = HatchBgDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(strings.postAdTitle, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditListingModal(
    product: Product,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSave: (
        productId: String,
        titleAr: String,
        descriptionAr: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        condition: String,
        city: String,
        areaAr: String,
        images: List<String>,
        paymentMethods: List<String>,
        negotiable: Boolean,
        deliveryAvailable: Boolean,
        urgentSale: Boolean
    ) -> Unit
) {
    var titleAr by remember(product.id) { mutableStateOf(product.titleAr) }
    var descriptionAr by remember(product.id) { mutableStateOf(product.descriptionAr) }
    var priceText by remember(product.id) { mutableStateOf(product.price.toLong().toString()) }
    var originalPriceText by remember(product.id) { mutableStateOf(product.originalPrice?.takeIf { it > 0 }?.toLong()?.toString().orEmpty()) }
    var category by remember(product.id) { mutableStateOf(product.category) }
    var condition by remember(product.id) { mutableStateOf(product.condition) }
    var city by remember(product.id) { mutableStateOf(product.city) }
    var areaAr by remember(product.id) { mutableStateOf(product.areaAr) }
    var negotiable by remember(product.id) { mutableStateOf(product.negotiable) }
    var deliveryAvailable by remember(product.id) { mutableStateOf(product.deliveryAvailable) }
    var urgentSale by remember(product.id) { mutableStateOf(product.urgentSale) }
    val selectedPayments = remember(product.id) {
        mutableStateListOf<String>().apply { addAll(product.paymentMethods.ifEmpty { listOf("cash") }) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().background(HatchSurfaceDark).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = null, tint = HatchTextPrimary) }
                    Text(strings.editAdTitle, color = HatchAmber400, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HatchField(titleAr, { titleAr = it }, if (language == AppLanguage.AR) "العنوان" else "Title")
                    HatchField(priceText, { priceText = it.filter { c -> c.isDigit() } }, if (language == AppLanguage.AR) "السعر (ج.س)" else "Price (SDG)", KeyboardType.Number)
                    HatchField(originalPriceText, { originalPriceText = it.filter { c -> c.isDigit() } }, if (language == AppLanguage.AR) "السعر السابق" else "Original Price", KeyboardType.Number)

                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryId.entries.forEach { c ->
                            SelectablePill(strings.categoryLabel(c.wireValue), category == c.wireValue) { category = c.wireValue }
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProductCondition.entries.forEach { cond ->
                            SelectablePill(strings.conditionLabel(cond.wireValue), condition == cond.wireValue) { condition = cond.wireValue }
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SudaneseCities.ALL.forEach { c ->
                            SelectablePill(c.substringBefore(" ("), city == c) { city = c }
                        }
                    }
                    HatchField(areaAr, { areaAr = it }, if (language == AppLanguage.AR) "الحي" else "Area")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { pm ->
                            val sel = selectedPayments.contains(pm.wireValue)
                            SelectablePill(strings.paymentLabel(pm.wireValue), sel) {
                                if (sel && selectedPayments.size > 1) selectedPayments.remove(pm.wireValue)
                                else if (!sel) selectedPayments.add(pm.wireValue)
                            }
                        }
                    }
                    HatchSwitchRow(strings.negotiableBadge, negotiable) { negotiable = it }
                    HatchSwitchRow(strings.deliveryBadge, deliveryAvailable) { deliveryAvailable = it }
                    HatchSwitchRow(strings.urgentBadge, urgentSale) { urgentSale = it }
                    HatchField(descriptionAr, { descriptionAr = it }, if (language == AppLanguage.AR) "الوصف" else "Description", singleLine = false, minLines = 3)
                }
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().background(HatchSurfaceDark).padding(16.dp)) {
                    val priceVal = priceText.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            onSave(
                                product.id, titleAr, descriptionAr, priceVal, originalPriceText.toDoubleOrNull(),
                                category, condition, city, areaAr, product.images, selectedPayments.toList(),
                                negotiable, deliveryAvailable, urgentSale
                            )
                        },
                        enabled = !isSubmitting && titleAr.trim().length >= 2 && descriptionAr.trim().length >= 5 && priceVal > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (language == AppLanguage.AR) "حفظ التعديلات" else "Save Changes", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MakeOfferDialog(
    product: Product,
    isSubmitting: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSubmitOffer: (Double, String) -> Unit
) {
    val suggested90 = (product.price * 0.9).toLong()
    val suggested80 = (product.price * 0.8).toLong()
    val suggested70 = (product.price * 0.7).toLong()
    var offerAmount by remember(product.id) { mutableStateOf(suggested90.toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(strings.makeOffer, color = HatchAmber400, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${if (language == AppLanguage.AR) product.titleAr else product.titleEn} • ${HatchFormatters.formatSdg(product.price, language)}",
                    color = HatchTextSecondary,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("90%" to suggested90, "80%" to suggested80, "70%" to suggested70).forEach { (pct, valAmt) ->
                        SelectablePill("$pct (${valAmt})", offerAmount == valAmt.toString()) {
                            offerAmount = valAmt.toString()
                        }
                    }
                }
                HatchField(
                    value = offerAmount,
                    onValueChange = { offerAmount = it.filter { c -> c.isDigit() } },
                    label = if (language == AppLanguage.AR) "المبلغ المقترح (ج.س)" else "Offer Amount (SDG)",
                    keyboardType = KeyboardType.Number
                )
                HatchField(
                    value = note,
                    onValueChange = { note = it },
                    label = if (language == AppLanguage.AR) "رسالة مرفقة (اختياري)" else "Optional Note"
                )
            }
        },
        confirmButton = {
            val amount = offerAmount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSubmitOffer(amount, note) },
                enabled = !isSubmitting && amount > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
            ) {
                Text(strings.makeOffer, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
fun SoldConfirmDialog(
    product: Product,
    isSubmitting: Boolean,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(strings.markSoldTitle, color = HatchEmerald400, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(product.titleAr, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(strings.markSoldConfirmBody, color = HatchTextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White)
            ) {
                Text(strings.markSoldTitle, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء / Cancel", color = HatchTextMuted) }
        }
    )
}

@Composable
private fun PhotoPreviewThumb(model: Any, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(68.dp)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(1.dp, HatchAmber500, RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(HatchRose500)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun hatchInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HatchSurfaceDark,
    unfocusedContainerColor = HatchSurfaceDark,
    focusedBorderColor = HatchAmber500,
    unfocusedBorderColor = HatchBorderDark,
    focusedTextColor = HatchTextPrimary,
    unfocusedTextColor = HatchTextPrimary
)

@Composable
fun HatchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = HatchTextMuted, fontSize = 12.sp) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = hatchInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SelectablePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) HatchAmber500 else HatchCardElevated)
            .border(1.dp, if (selected) HatchAmber500 else HatchBorderDark, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) HatchBgDark else HatchTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
        )
    }
}

@Composable
private fun HatchSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchBorderDark, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = HatchTextPrimary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = HatchBgDark, checkedTrackColor = HatchAmber500)
        )
    }
}
