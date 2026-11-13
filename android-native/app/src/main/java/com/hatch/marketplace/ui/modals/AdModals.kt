package com.hatch.marketplace.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.ui.components.openAdTarget
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import kotlinx.coroutines.delay

@Composable
fun AdMobInterstitialModal(
    ad: SponsoredAdItem,
    countdownSeconds: Int,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onClickAd: () -> Unit
) {
    val context = LocalContext.current
    var remaining by remember(ad.id) { mutableIntStateOf(countdownSeconds.coerceIn(1, 30)) }

    LaunchedEffect(ad.id) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
    }

    val title = if (language == AppLanguage.AR) ad.titleAr else ad.titleEn.ifBlank { ad.titleAr }
    val desc = if (language == AppLanguage.AR) ad.descriptionAr else ad.descriptionEn.ifBlank { ad.descriptionAr }
    val cta = if (language == AppLanguage.AR) ad.ctaTextAr.ifBlank { "استكشف الآن" } else ad.ctaTextEn.ifBlank { "Explore Now" }

    Dialog(onDismissRequest = { if (remaining == 0) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.sponsoredLabel,
                        color = HatchBgDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(HatchAmber400).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    if (remaining > 0) {
                        Text(
                            text = "${strings.adCountdownSkip} ($remaining)",
                            color = HatchTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(HatchSurfaceDark)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Skip", tint = HatchTextPrimary)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!ad.videoUrl.isNullOrBlank()) {
                        AdVideoBox(videoUrl = ad.videoUrl, modifier = Modifier.fillMaxWidth().height(250.dp))
                    } else if (ad.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ad.imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(20.dp))
                        )
                    }
                    Text(ad.advertiser, color = HatchAmber400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(title, color = HatchTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    if (desc.isNotBlank()) {
                        Text(desc, color = HatchTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }

                Button(
                    onClick = {
                        onClickAd()
                        openAdTarget(context, ad.targetUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(cta, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AdMobRewardedModal(
    ad: SponsoredAdItem,
    durationSeconds: Int,
    language: AppLanguage,
    onCompleted: () -> Unit,
    onForfeitDismiss: () -> Unit
) {
    val total = durationSeconds.coerceIn(5, 60)
    var remaining by remember(ad.id) { mutableIntStateOf(total) }
    var showForfeitWarning by remember { mutableStateOf(false) }

    LaunchedEffect(ad.id) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
    }

    val progress = ((total - remaining).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val title = if (language == AppLanguage.AR) ad.titleAr else ad.titleEn.ifBlank { ad.titleAr }

    Dialog(onDismissRequest = { if (remaining == 0) onCompleted() else showForfeitWarning = true }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = HatchEmerald400)
                            Text(
                                text = if (remaining > 0) {
                                    if (language == AppLanguage.AR) "المكافأة بعد $remaining ثانية" else "Reward in ${remaining}s"
                                } else {
                                    if (language == AppLanguage.AR) "اكتملت المكافأة! 🎉" else "Reward Unlocked! 🎉"
                                },
                                color = HatchEmerald400,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { if (remaining == 0) onCompleted() else showForfeitWarning = true }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = HatchEmerald500,
                        trackColor = HatchBorderDark,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!ad.videoUrl.isNullOrBlank()) {
                        AdVideoBox(videoUrl = ad.videoUrl, modifier = Modifier.fillMaxWidth().height(260.dp))
                    } else if (ad.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ad.imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(20.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchAmber500.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(64.dp))
                        }
                    }
                    Text(ad.advertiser, color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(title, color = HatchTextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = onCompleted,
                    enabled = remaining == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchEmerald500, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = if (remaining == 0) {
                            if (language == AppLanguage.AR) "استلام +1 رصيد تمييز 🎉" else "Claim +1 Boost Credit 🎉"
                        } else {
                            if (language == AppLanguage.AR) "انتظر انتهاء الإعلان..." else "Please wait..."
                        },
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (showForfeitWarning) {
        AlertDialog(
            onDismissRequest = { showForfeitWarning = false },
            containerColor = HatchSurfaceDark,
            title = { Text(if (language == AppLanguage.AR) "إلغاء المكافأة؟" else "Forfeit Reward?", color = HatchAmber400, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (language == AppLanguage.AR) "إذا أغلقت الإعلان الآن فلن تحصل على رصيد التمييز المجاني." else "Closing now will forfeit your +1 free boost credit.",
                    color = HatchTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showForfeitWarning = false; onForfeitDismiss() }) {
                    Text(if (language == AppLanguage.AR) "إغلاق على أي حال" else "Exit Anyway", color = HatchTextMuted)
                }
            },
            dismissButton = {
                Button(onClick = { showForfeitWarning = false }, colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark)) {
                    Text(if (language == AppLanguage.AR) "متابعة المشاهدة" else "Keep Watching", fontWeight = FontWeight.Black)
                }
            }
        )
    }
}

@Composable
fun AdVideoBox(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    )
}
