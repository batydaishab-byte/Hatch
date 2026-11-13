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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchRed500
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary

@Composable
fun AdminConversationModal(
    conversation: Conversation,
    messages: List<ChatMessage>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onDeleteAd: (String) -> Unit,
    onSuspendSeller: (sellerId: String, sellerName: String) -> Unit
) {
    val isAr = language == AppLanguage.AR

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(HatchSurfaceDark)
                        .border(0.5.dp, HatchBorderDark)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = HatchAmber400,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isAr) "تدقيق المحادثة الأمني" else "Admin Chat Audit",
                                color = HatchAmber400,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${conversation.buyerName} ↔ ${conversation.sellerName}",
                                color = HatchTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                    }
                }

                // Product Context Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HatchCardElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (conversation.productImage.isNotBlank()) {
                        AsyncImage(
                            model = conversation.productImage,
                            contentDescription = conversation.productTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.productTitle,
                            color = HatchTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = HatchFormatters.formatSdg(conversation.productPrice, language),
                            color = HatchAmber400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Messages Audit Stream
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "لا توجد رسائل في هذه المحادثة" else "No messages in this conversation",
                                    color = HatchTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isBuyer = msg.senderId == conversation.buyerId
                            val senderLabel = if (isBuyer) {
                                "${conversation.buyerName} (${if (isAr) "مشتري" else "Buyer"})"
                            } else {
                                "${conversation.sellerName} (${if (isAr) "بائع" else "Seller"})"
                            }
                            val hasDepositWarning = MarketplaceLogic.hasDepositWarningKeywords(msg.text)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (hasDepositWarning) HatchRose500.copy(alpha = 0.14f)
                                        else HatchSurfaceDark
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (hasDepositWarning) HatchRose500 else HatchBorderDark,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = senderLabel,
                                        color = if (isBuyer) HatchEmerald400 else HatchAmber400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasDepositWarning) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(HatchRose500)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.WarningAmber,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه طلب عربون" else "Deposit Risk",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                if (msg.isOfferUpdate && msg.offerAmount != null) {
                                    Text(
                                        text = "${if (isAr) "عرض مالي:" else "Offer:"} ${HatchFormatters.formatSdg(msg.offerAmount, language)}",
                                        color = HatchAmber400,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                if (!msg.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = msg.imageTag,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }

                                if (!msg.audioUrl.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HatchCardElevated)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (isAr) "رسالة صوتية (${msg.audioDuration ?: 0}ث)" else "Voice note (${msg.audioDuration ?: 0}s)",
                                            color = HatchTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (msg.text.isNotBlank()) {
                                    Text(
                                        text = msg.text,
                                        color = HatchTextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Moderation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(HatchSurfaceDark)
                        .border(0.5.dp, HatchBorderDark)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDeleteAd(conversation.productId)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HatchRed500, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "حذف الإعلان" else "Delete Ad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Button(
                        onClick = {
                            onSuspendSeller(conversation.sellerId, conversation.sellerName)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "إيقاف البائع" else "Suspend Seller",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
