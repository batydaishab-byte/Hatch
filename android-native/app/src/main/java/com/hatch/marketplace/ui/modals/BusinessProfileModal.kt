package com.hatch.marketplace.ui.modals

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CommercialDirectoryEntry
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.UserReview
import com.hatch.marketplace.data.model.displayLogoOrAvatar
import com.hatch.marketplace.ui.components.BadgePill
import com.hatch.marketplace.ui.components.ProductCard
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchEmerald500
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary
import java.util.Locale

@Composable
fun BusinessProfileModal(
    sellerId: String,
    sellerUser: PublicUser?,
    sellerProducts: List<Product>,
    favoriteIds: Set<String>,
    sellerReviews: List<UserReview>,
    isOwnProfile: Boolean,
    isSignedIn: Boolean = true,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onStartChatWithProduct: (Product) -> Unit,
    onEditOwnProfile: () -> Unit,
    onGuestContactPrompt: () -> Unit = {}
) {
    val context = LocalContext.current
    val firstProd = sellerProducts.firstOrNull()
    val displayName = sellerUser?.name?.takeIf { it.isNotBlank() }
        ?: firstProd?.sellerName?.takeIf { it.isNotBlank() }
        ?: if (language == AppLanguage.AR) "متجر هتش" else "HATCH Store"
    val displayLogo = sellerUser?.displayLogoOrAvatar()?.takeIf { it.isNotBlank() }
        ?: firstProd?.sellerAvatar.orEmpty()
    val rawType = sellerUser?.accountType ?: firstProd?.sellerAccountType
    val resolvedType = AccountType.fromWire(rawType)
    val activity = sellerUser?.businessActivity?.takeIf { it.isNotBlank() }
        ?: firstProd?.sellerBusinessActivity?.takeIf { it.isNotBlank() }
        ?: ""
    val city = sellerUser?.city?.takeIf { it.isNotBlank() }
        ?: firstProd?.city.orEmpty()
    val verified = sellerUser?.verifiedIdentity ?: firstProd?.sellerVerified ?: false
    val dealsCount = sellerUser?.dealsCompleted ?: firstProd?.sellerDealsCount ?: 0
    val avgRating = if (sellerReviews.isNotEmpty()) {
        sellerReviews.map { it.rating }.average()
    } else {
        sellerUser?.rating ?: firstProd?.sellerRating ?: 5.0
    }
    val typeLabel = strings.accountTypeLabel(resolvedType.wireValue)
    val typeIcon = when (resolvedType) {
        AccountType.COMPANY -> Icons.Rounded.Business
        else -> Icons.Rounded.Storefront
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(HatchSurfaceDark)
                        .border(0.5.dp, HatchBorderDark)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(typeIcon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(18.dp))
                        Text(
                            text = displayName,
                            color = HatchTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        val shareBody = buildString {
                            append(displayName)
                            append(" ($typeLabel)")
                            if (activity.isNotBlank()) append(" — $activity")
                            if (city.isNotBlank()) append("\n$city")
                            append("\nhttps://batydaishab-byte.github.io/Hatch/")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                        }
                        context.startActivity(Intent.createChooser(intent, strings.shareAd))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = strings.shareAd, tint = HatchAmber400)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(HatchSurfaceDark)
                                .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(HatchCardElevated)
                                        .border(2.dp, HatchAmber500, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (displayLogo.isNotBlank()) {
                                        AsyncImage(
                                            model = displayLogo,
                                            contentDescription = displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(typeIcon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(30.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = displayName,
                                            color = HatchTextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (verified) {
                                            Icon(Icons.Rounded.Verified, contentDescription = strings.verifiedBadge, tint = HatchEmerald400, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        BadgePill(
                                            text = typeLabel,
                                            bg = HatchAmber500.copy(alpha = 0.2f),
                                            fg = HatchAmber400
                                        )
                                        if (city.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(13.dp))
                                                Text(city.substringBefore(" ("), color = HatchTextSecondary, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }

                                    if (activity.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Icon(Icons.Rounded.Work, contentDescription = null, tint = HatchEmerald400, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = activity,
                                                color = HatchEmerald400,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Stats Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(HatchCardElevated)
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StoreStatItem(
                                    value = "⭐ ${String.format(Locale.US, "%.1f", avgRating)}",
                                    label = if (language == AppLanguage.AR) "التقييم" else "Rating"
                                )
                                StoreStatItem(
                                    value = "$dealsCount",
                                    label = if (language == AppLanguage.AR) "صفقات ناجحة" else "Deals"
                                )
                                StoreStatItem(
                                    value = "${sellerProducts.size}",
                                    label = if (language == AppLanguage.AR) "منتجات نشطة" else "Listings"
                                )
                            }

                            if (isOwnProfile) {
                                OutlinedButton(
                                    onClick = onEditOwnProfile,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.AR) "تعديل بيانات الصفحة التجارية" else "Edit Commercial Page",
                                        color = HatchAmber400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else if (!isSignedIn) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(HatchCardElevated)
                                        .border(1.dp, HatchAmber500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .clickable { onGuestContactPrompt() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.guestStorefrontReadOnlyHint,
                                        color = HatchAmber400,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else if (firstProd != null) {
                                Button(
                                    onClick = { onStartChatWithProduct(firstProd) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.AR) "مراسلة والتفاوض الآن" else "Message & Negotiate",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "${strings.storefrontProductsTitle} (${sellerProducts.size})",
                                    color = HatchTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (sellerProducts.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(HatchSurfaceDark)
                                    .border(1.dp, HatchBorderDark, RoundedCornerShape(16.dp))
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == AppLanguage.AR) "لا توجد منتجات نشطة معروضة حالياً" else "No active products listed yet",
                                    color = HatchTextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(sellerProducts, key = { "store_prod_${sellerId}_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                isFavorite = favoriteIds.contains(product.id),
                                language = language,
                                strings = strings,
                                onClick = { onOpenProduct(product) },
                                onToggleFavorite = { onToggleFavorite(product.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = HatchAmber400, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(label, color = HatchTextMuted, fontSize = 10.sp)
    }
}

@Composable
fun CommercialDirectoryModal(
    accountType: AccountType,
    entries: List<CommercialDirectoryEntry>,
    language: AppLanguage,
    strings: HatchStrings,
    onDismiss: () -> Unit,
    onSelectSeller: (String) -> Unit,
    onOpenProduct: (Product) -> Unit
) {
    var searchQuery by remember(accountType) { mutableStateOf("") }
    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) entries
        else entries.filter { entry ->
            entry.name.lowercase(Locale.ROOT).contains(q) ||
                entry.businessActivity.lowercase(Locale.ROOT).contains(q) ||
                entry.city.lowercase(Locale.ROOT).contains(q) ||
                entry.products.any {
                    it.titleAr.lowercase(Locale.ROOT).contains(q) ||
                        it.titleEn.lowercase(Locale.ROOT).contains(q)
                }
        }
    }

    val isCompany = accountType == AccountType.COMPANY
    val title = if (isCompany) strings.registeredCompaniesTitle else strings.registeredStoresTitle
    val emptyText = if (isCompany) strings.noCompaniesYet else strings.noStoresYet
    val typeIcon = if (isCompany) Icons.Rounded.Business else Icons.Rounded.Storefront

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(color = HatchBgDark, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(HatchSurfaceDark)
                        .border(0.5.dp, HatchBorderDark)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = HatchTextPrimary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(typeIcon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(18.dp))
                        Text(
                            text = "$title (${filteredEntries.size})",
                            color = HatchTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isCompany) {
                                if (language == AppLanguage.AR) "ابحث عن شركة أو مجال عمل أو منتج..." else "Search company, field, or product..."
                            } else {
                                if (language == AppLanguage.AR) "ابحث عن متجر أو نشاط أو منتج..." else "Search store, activity, or product..."
                            },
                            color = HatchTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = HatchAmber400) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = HatchTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HatchSurfaceDark,
                        unfocusedContainerColor = HatchSurfaceDark,
                        focusedBorderColor = HatchAmber500,
                        unfocusedBorderColor = HatchBorderDark,
                        focusedTextColor = HatchTextPrimary,
                        unfocusedTextColor = HatchTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )

                if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(HatchSurfaceDark)
                                    .border(1.dp, HatchAmber500.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(typeIcon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(30.dp))
                            }
                            Text(
                                text = emptyText,
                                color = HatchTextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredEntries, key = { "dir_${accountType.wireValue}_${it.sellerId}" }) { entry ->
                            CommercialDirectoryCard(
                                entry = entry,
                                language = language,
                                strings = strings,
                                onSelectEntry = { onSelectSeller(entry.sellerId) },
                                onOpenProduct = onOpenProduct
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommercialDirectoryCard(
    entry: CommercialDirectoryEntry,
    language: AppLanguage,
    strings: HatchStrings,
    onSelectEntry: () -> Unit,
    onOpenProduct: (Product) -> Unit
) {
    val typeIcon = if (entry.accountType == AccountType.COMPANY) Icons.Rounded.Business else Icons.Rounded.Storefront
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HatchSurfaceDark)
            .border(1.dp, HatchAmber500.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
            .clickable { onSelectEntry() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HatchCardElevated)
                    .border(1.5.dp, HatchAmber500, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.logoUrl,
                        contentDescription = entry.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(typeIcon, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(26.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = entry.name,
                        color = HatchTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.verifiedIdentity) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = strings.verifiedBadge,
                            tint = HatchEmerald400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (entry.businessActivity.isNotBlank()) {
                    Text(
                        text = entry.businessActivity,
                        color = HatchEmerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entry.city.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(12.dp))
                            Text(
                                text = entry.city.substringBefore(" ("),
                                color = HatchTextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = HatchAmber400, modifier = Modifier.size(12.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", entry.rating),
                            color = HatchAmber400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            BadgePill(
                text = if (language == AppLanguage.AR) "${entry.products.size} منتج" else "${entry.products.size} items",
                bg = HatchAmber500.copy(alpha = 0.16f),
                fg = HatchAmber400
            )
        }

        if (entry.products.isEmpty()) {
            Text(
                text = if (language == AppLanguage.AR) "لا توجد منتجات معروضة حالياً" else "No active products yet",
                color = HatchTextMuted,
                fontSize = 11.sp
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entry.products.take(10), key = { "dir_prod_${entry.sellerId}_${it.id}" }) { product ->
                    val title = if (language == AppLanguage.AR) product.titleAr else product.titleEn.ifBlank { product.titleAr }
                    val thumb = product.thumbnailUrl?.takeIf { it.isNotBlank() } ?: product.images.firstOrNull().orEmpty()
                    Column(
                        modifier = Modifier
                            .width(126.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HatchCardElevated)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(12.dp))
                            .clickable { onOpenProduct(product) }
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HatchBgDark),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumb.isNotBlank()) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = title,
                            color = HatchTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = HatchFormatters.formatSdg(product.price, language),
                            color = HatchAmber400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
