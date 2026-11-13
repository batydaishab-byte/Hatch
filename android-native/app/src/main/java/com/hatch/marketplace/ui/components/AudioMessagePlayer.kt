package com.hatch.marketplace.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderSubtle
import com.hatch.marketplace.ui.theme.HatchTextMuted
import kotlinx.coroutines.delay
import java.io.File

private val WAVE_BAR_HEIGHTS = listOf(8, 14, 20, 12, 22, 16, 10, 18, 24, 14, 11, 20, 17, 9, 19, 15, 11, 13)

@Composable
fun AudioMessagePlayer(
    audioUrl: String,
    durationSeconds: Int,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember(audioUrl) { mutableStateOf(false) }
    var progress by remember(audioUrl) { mutableFloatStateOf(0f) }
    var currentSec by remember(audioUrl) { mutableLongStateOf(0L) }

    val player = remember(audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    progress = 0f
                    currentSec = 0L
                    player.seekTo(0L)
                    player.pause()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val dur = player.duration.takeIf { it > 0 } ?: (durationSeconds * 1000L).coerceAtLeast(1000L)
            val pos = player.currentPosition.coerceAtLeast(0L)
            progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            currentSec = pos / 1000L
            delay(180L)
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isMine) HatchBgDark.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isMine) HatchBgDark else HatchAmber500)
                .clickable {
                    if (player.isPlaying) player.pause() else player.play()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play Voice Note",
                tint = if (isMine) HatchAmber400 else HatchBgDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                WAVE_BAR_HEIGHTS.forEachIndexed { idx, h ->
                    val threshold = idx.toFloat() / WAVE_BAR_HEIGHTS.size.toFloat()
                    val active = progress > threshold
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    active && isMine -> HatchBgDark
                                    active -> HatchAmber400
                                    isMine -> HatchBgDark.copy(alpha = 0.35f)
                                    else -> HatchBorderSubtle
                                }
                            )
                    )
                }
            }

            val displaySec = if (isPlaying) currentSec.toInt() else durationSeconds
            val mins = displaySec / 60
            val secs = displaySec % 60
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = if (isMine) HatchBgDark else HatchAmber400, modifier = Modifier.size(11.dp))
                Text(
                    text = String.format(java.util.Locale.US, "%d:%02d", mins, secs),
                    color = if (isMine) HatchBgDark.copy(alpha = 0.85f) else HatchTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

class VoiceRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    fun startRecording(): Result<File> = runCatching {
        val dir = File(context.cacheDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(22_050)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        outputFile = file
        startedAtMs = System.currentTimeMillis()
        file
    }

    fun stopRecording(): Pair<File?, Int> {
        val elapsedSec = ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt().coerceAtLeast(1)
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        return outputFile to elapsedSec
    }

    fun cancelRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
