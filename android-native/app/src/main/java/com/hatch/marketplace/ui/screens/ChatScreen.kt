package com.hatch.marketplace.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.OfferStatus
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.ui.components.AudioMessagePlayer
import com.hatch.marketplace.ui.components.BadgePill
import com.hatch.marketplace.ui.components.VoiceRecorderHelper
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.modals.HatchField
import com.hatch.marketplace.ui.modals.SelectablePill
import com.hatch.marketplace.ui.modals.hatchInputColors
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
fun ChatScreen(
    conversations: List<Conversation>,
    activeConversationId: String?,
    messages: List<ChatMessage>,
    products: List<Product>,
    currentUserId: String,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectConversation: (Conversation?) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendInspectionPhoto: (Uri?, String?, String, String) -> Unit,
    onSendVoiceNote: (String, Int) -> Unit,
    onRespondOffer: (OfferStatus, Double?) -> Unit,
    onRequestMarkSold: (Product) -> Unit,
    onShowToast: (String) -> Unit
) {
    val activeConv = conversations.firstOrNull { it.id == activeConversationId }

    if (activeConv == null) {
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(56.dp))
                    Text(strings.chatEmptyTitle, color = HatchTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(strings.chatEmptySubtitle, color = HatchTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val isBuyer = conv.buyerId == currentUserId
                    val partnerName = if (isBuyer) conv.sellerName else conv.buyerName
                    val partnerAvatar = if (isBuyer) conv.sellerAvatar else conv.buyerAvatar
                    val unread = if (isBuyer) conv.unreadByBuyer else conv.unreadBySeller

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(HatchSurfaceDark)
                            .border(1.dp, if (unread > 0) HatchAmber500.copy(alpha = 0.55f) else HatchBorderDark, RoundedCornerShape(16.dp))
                            .clickable { onSelectConversation(conv) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(48.dp)) {
                            AsyncImage(
                                model = conv.productImage.ifBlank { partnerAvatar },
                                contentDescription = conv.productTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(HatchCardElevated)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(partnerName.ifBlank { "مستخدم هتش" }, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (conv.offerAmount != null && conv.offerStatus != OfferStatus.NONE.wireValue) {
                                    BadgePill(
                                        text = "${conv.offerAmount.toLong()} SDG",
                                        bg = HatchAmber500.copy(alpha = 0.18f),
                                        fg = HatchAmber400
                                    )
                                }
                            }
                            Text(conv.productTitle, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = conv.lastMessage.ifBlank { strings.chatEmptySubtitle },
                                color = HatchTextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (unread > 0) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape).background(HatchRose500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(unread.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Active Chat Thread
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isSeller = activeConv.sellerId == currentUserId
    val partnerName = if (isSeller) activeConv.buyerName else activeConv.sellerName
    val linkedProduct = products.firstOrNull { it.id == activeConv.productId }

    var inputText by remember { mutableStateOf("") }
    var showCounterDialog by remember { mutableStateOf(false) }
    var counterAmountText by remember { mutableStateOf("") }
    var showInspectionSheet by remember { mutableStateOf(false) }
    var lightboxImageUrl by remember { mutableStateOf<String?>(null) }

    // Voice Recording state
    val recorder = remember { VoiceRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { recorder.cancelRecording() }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordSeconds++
            }
        }
    }

    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(messages.size, imeOpen) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recorder.startRecording()
                .onSuccess { isRecording = true }
                .onFailure { onShowToast(it.localizedMessage ?: "Mic error") }
        } else {
            onShowToast(if (language == AppLanguage.AR) "يرجى منح إذن الميكروفون للتسجيل الصوتي" else "Microphone permission required")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Thread Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HatchSurfaceDark)
                .border(0.5.dp, HatchBorderDark)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { onSelectConversation(null) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = HatchAmber400)
            }
            if (activeConv.productImage.isNotBlank()) {
                AsyncImage(
                    model = activeConv.productImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(partnerName, color = HatchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(
                    text = "${activeConv.productTitle} • ${HatchFormatters.formatSdg(activeConv.productPrice, language)}",
                    color = HatchAmber400,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Active Offer Negotiation Bar
        if (activeConv.offerStatus != OfferStatus.NONE.wireValue && activeConv.offerAmount != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HatchCardElevated)
                    .border(0.5.dp, HatchAmber500.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val activePrice = activeConv.counterAmount ?: activeConv.offerAmount
                val statusLabel = when (activeConv.offerStatus) {
                    OfferStatus.ACCEPTED.wireValue -> strings.offerAccepted
                    OfferStatus.DECLINED.wireValue -> strings.offerDeclined
                    OfferStatus.COUNTERED.wireValue -> strings.offerCountered
                    else -> strings.offerPending
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$statusLabel: ${HatchFormatters.formatSdg(activePrice, language)}",
                        color = if (activeConv.offerStatus == OfferStatus.ACCEPTED.wireValue) HatchEmerald400 else HatchAmber400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (isSeller && activeConv.offerStatus == OfferStatus.PENDING.wireValue) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onRespondOffer(OfferStatus.ACCEPTED, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.acceptOffer, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { showCounterDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.counterOffer, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { onRespondOffer(OfferStatus.DECLINED, null) },
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(strings.declineOffer, color = HatchRose500, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                } else if (isSeller && activeConv.offerStatus == OfferStatus.ACCEPTED.wireValue && linkedProduct != null && linkedProduct.status == ProductStatus.ACTIVE.wireValue) {
                    Button(
                        onClick = { onRequestMarkSold(linkedProduct) },
                        colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.markSoldChatCta, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else if (!isSeller && activeConv.offerStatus == OfferStatus.COUNTERED.wireValue) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onRespondOffer(OfferStatus.ACCEPTED, activeConv.counterAmount) },
                            colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.acceptOffer, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { onRespondOffer(OfferStatus.DECLINED, null) },
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(strings.declineOffer, color = HatchRose500, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Sudan Safety Micro-Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HatchEmerald500.copy(alpha = 0.09f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(14.dp))
            Text(strings.chatDepositBanner, color = HatchEmerald400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 285.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    msg.isOfferUpdate -> HatchCardElevated
                                    isMine -> HatchAmber500
                                    else -> HatchSurfaceDark
                                }
                            )
                            .then(
                                if (msg.isOfferUpdate) Modifier.border(1.dp, HatchAmber500, RoundedCornerShape(16.dp))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (!msg.imageUrl.isNullOrBlank()) {
                            if (!msg.imageTag.isNullOrBlank()) {
                                BadgePill(msg.imageTag, HatchBgDark.copy(alpha = 0.7f), HatchAmber400)
                            }
                            AsyncImage(
                                model = msg.imageUrl,
                                contentDescription = msg.imageTag,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { lightboxImageUrl = msg.imageUrl }
                            )
                        }

                        if (!msg.audioUrl.isNullOrBlank()) {
                            AudioMessagePlayer(
                                audioUrl = msg.audioUrl,
                                durationSeconds = msg.audioDuration ?: 10,
                                isMine = isMine
                            )
                        }

                        if (msg.text.isNotBlank()) {
                            Text(
                                text = msg.text,
                                color = if (isMine && !msg.isOfferUpdate) HatchBgDark else HatchTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (msg.isOfferUpdate) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isMine) {
                            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                when (msg.status) {
                                    "read" -> Icon(Icons.Rounded.DoneAll, contentDescription = "Read", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                    "delivered" -> Icon(Icons.Rounded.DoneAll, contentDescription = "Delivered", tint = HatchBgDark.copy(alpha = 0.65f), modifier = Modifier.size(14.dp))
                                    else -> Icon(Icons.Rounded.Check, contentDescription = "Sent", tint = HatchBgDark.copy(alpha = 0.65f), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Replies Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            strings.quickReplies.forEach { reply ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HatchSurfaceDark)
                        .border(1.dp, HatchBorderDark, RoundedCornerShape(50))
                        .clickable { onSendMessage(reply) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(reply, color = HatchTextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Voice Recording Banner OR Composer Bar
        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HatchRose500.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.voiceRecordingLabel} (${recordSeconds}s)",
                    color = HatchRose500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        recorder.cancelRecording()
                        isRecording = false
                    }) { Text("إلغاء", color = HatchTextMuted, fontSize = 11.sp) }
                    Button(
                        onClick = {
                            val (file, sec) = recorder.stopRecording()
                            isRecording = false
                            if (file != null && BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")) {
                                onSendVoiceNote(Uri.fromFile(file).toString(), sec)
                            } else {
                                file?.delete()
                                onShowToast(
                                    if (language == AppLanguage.AR)
                                        "تم تسجيل ${sec}ث — يتطلب رفع الصوت سحابياً تفعيل MEDIA_SIGNER_URL المجاني"
                                    else
                                        "Recorded ${sec}s — Cloud audio upload requires MEDIA_SIGNER_URL"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HatchRose500, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == AppLanguage.AR) "إرسال الصوت" else "Send Voice", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HatchSurfaceDark)
                    .border(0.5.dp, HatchBorderDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = { showInspectionSheet = true }) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = strings.sendInspectionPhoto, tint = HatchEmerald400)
                }
                IconButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        recorder.startRecording().onSuccess { isRecording = true }
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Icon(Icons.Rounded.Mic, contentDescription = strings.voiceHoldOrTapHint, tint = HatchAmber400)
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(strings.chatInputPlaceholder, color = HatchTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }),
                    colors = hatchInputColors(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(HatchAmber500)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = HatchBgDark, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Counter-Offer Dialog
    if (showCounterDialog) {
        AlertDialog(
            onDismissRequest = { showCounterDialog = false },
            containerColor = HatchSurfaceDark,
            title = { Text(strings.counterOffer, color = HatchAmber400, fontWeight = FontWeight.Black) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HatchField(
                        value = counterAmountText,
                        onValueChange = { counterAmountText = it.filter { c -> c.isDigit() } },
                        label = if (language == AppLanguage.AR) "السعر المضاد (ج.س)" else "Counter Price (SDG)",
                        keyboardType = KeyboardType.Number
                    )
                }
            },
            confirmButton = {
                val amt = counterAmountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        onRespondOffer(OfferStatus.COUNTERED, amt)
                        showCounterDialog = false
                    },
                    enabled = amt > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)
                ) { Text(strings.counterOffer, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showCounterDialog = false }) { Text("إلغاء", color = HatchTextMuted) } }
        )
    }

    // Inspection Photo Picker Dialog (Camera + Gallery + 5 Inspection Tags)
    if (showInspectionSheet) {
        InspectionPhotoPickerDialog(
            language = language,
            strings = strings,
            onDismiss = { showInspectionSheet = false },
            onSubmitPhoto = { uri, url, tag, caption ->
                showInspectionSheet = false
                onSendInspectionPhoto(uri, url, tag, caption)
            },
            onShowToast = onShowToast
        )
    }

    // Fullscreen Pinch-to-Zoom & Double-Tap Lightbox + Save to Gallery
    lightboxImageUrl?.let { imgUrl ->
        FullscreenImageLightbox(
            imageUrl = imgUrl,
            language = language,
            onDismiss = { lightboxImageUrl = null },
            onSavedToast = { msg -> onShowToast(msg) }
        )
    }
}

@Composable
private fun InspectionPhotoPickerDialog(
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSubmitPhoto: (Uri?, String?, String, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(strings.inspectionTags.first().ar) }
    var caption by remember { mutableStateOf("") }
    var directUrl by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) selectedUri = pendingCamUri
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "insp_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCamUri = uri
            cameraLauncher.launch(uri)
        } else {
            onShowToast(if (language == AppLanguage.AR) "يرجى منح إذن الكاميرا" else "Camera permission required")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HatchSurfaceDark,
        title = { Text(strings.sendInspectionPhoto, color = HatchEmerald400, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (language == AppLanguage.AR) "اختر تصنيف الفحص:" else "Select Inspection Tag:", color = HatchTextSecondary, fontSize = 11.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    strings.inspectionTags.forEach { tag ->
                        val label = if (language == AppLanguage.AR) tag.ar else tag.en
                        SelectablePill(label, selectedTag == label) { selectedTag = label }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.galleryPick, color = HatchAmber400, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                                val file = File(dir, "insp_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                pendingCamUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                camPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.cameraCapture, color = HatchEmerald400, fontSize = 11.sp)
                    }
                }
                if (selectedUri != null) {
                    AsyncImage(model = selectedUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp)))
                }
                HatchField(directUrl, { directUrl = it }, if (language == AppLanguage.AR) "أو رابط صورة HTTPS مباشر" else "Or direct HTTPS image URL")
                HatchField(caption, { caption = it }, if (language == AppLanguage.AR) "ملاحظة فحص (اختياري)" else "Inspection Note")
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitPhoto(selectedUri, directUrl.takeIf { it.startsWith("https://") }, selectedTag, caption) },
                enabled = selectedUri != null || directUrl.startsWith("https://"),
                colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White)
            ) { Text(if (language == AppLanguage.AR) "إرسال الصورة" else "Send Photo", fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = HatchTextMuted) } }
    )
}

@Composable
private fun FullscreenImageLightbox(
    imageUrl: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSavedToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Inspection Lightbox",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                offset = if (nextScale > 1f) offset + pan else Offset.Zero
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f; offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            })
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.clip(CircleShape).background(HatchBgDark.copy(alpha = 0.7f))
                    ) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { scale = 1f; offset = Offset.Zero },
                            modifier = Modifier.clip(CircleShape).background(HatchBgDark.copy(alpha = 0.7f))
                        ) { Icon(Icons.Rounded.Refresh, contentDescription = "Reset Zoom", tint = HatchAmber400) }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    val saved = saveImageUrlToGallery(context, imageUrl)
                                    onSavedToast(
                                        if (saved) {
                                            if (language == AppLanguage.AR) "تم حفظ الصورة في المعرض" else "Saved to Gallery"
                                        } else {
                                            if (language == AppLanguage.AR) "تعذر حفظ الصورة" else "Save failed"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.clip(CircleShape).background(HatchBgDark.copy(alpha = 0.7f))
                        ) { Icon(Icons.Rounded.Download, contentDescription = "Save", tint = HatchEmerald400) }
                    }
                }
            }
        }
    }
}

private suspend fun saveImageUrlToGallery(context: Context, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = URL(imageUrl).openStream().use { it.readBytes() }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "HATCH_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HATCH")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching false
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@runCatching false
        true
    }.getOrDefault(false)
}
