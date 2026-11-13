package com.hatch.marketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CategoryId
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketFilterState
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductCondition
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.SudaneseCities
import com.hatch.marketplace.ui.components.AdMobNativeCard
import com.hatch.marketplace.ui.components.ProductCard
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber400
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchBorderDark
import com.hatch.marketplace.ui.theme.HatchBorderSubtle
import com.hatch.marketplace.ui.theme.HatchCardElevated
import com.hatch.marketplace.ui.theme.HatchEmerald400
import com.hatch.marketplace.ui.theme.HatchSurfaceDark
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary
import com.hatch.marketplace.ui.theme.HatchTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketBrowseScreen(
    products: List<Product>,
    favoriteIds: Set<String>,
    filter: MarketFilterState,
    searchHistory: List<String>,
    sponsoredAds: List<SponsoredAdItem>,
    adConfig: AdMobConfig,
    isSignedIn: Boolean,
    isRefreshing: Boolean,
    language: AppLanguage,
    strings: HatchStrings,
    onRefresh: () -> Unit,
    onUpdateFilter: ((MarketFilterState) -> MarketFilterState) -> Unit,
    onSelectCity: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAuth: () -> Unit,
    onAdImpression: (String) -> Unit,
    onAdClick: (SponsoredAdItem) -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }

    val nativeAds = remember(sponsoredAds) {
        sponsoredAds.filter { it.isActive && (it.format == "native" || it.format == "all") }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isSignedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HatchAmber500.copy(alpha = 0.12f))
                        .clickable { onOpenAuth() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.guestBrowseBanner, color = HatchAmber400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        text = strings.signInEmail,
                        color = HatchBgDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HatchAmber500)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { q ->
                        onUpdateFilter { it.copy(searchQuery = q) }
                        searchFocused = true
                    },
                    placeholder = { Text(strings.searchPlaceholder, color = HatchTextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = HatchAmber400) },
                    trailingIcon = {
                        if (filter.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onUpdateFilter { it.copy(searchQuery = "") } }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = HatchTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onCommitSearch(filter.searchQuery)
                        searchFocused = false
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HatchSurfaceDark,
                        unfocusedContainerColor = HatchSurfaceDark,
                        focusedBorderColor = HatchAmber500,
                        unfocusedBorderColor = HatchBorderDark,
                        focusedTextColor = HatchTextPrimary,
                        unfocusedTextColor = HatchTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (filter.activeFilterCount > 0) HatchAmber500 else HatchSurfaceDark)
                            .border(1.dp, HatchBorderDark, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = strings.filters,
                            tint = if (filter.activeFilterCount > 0) HatchBgDark else HatchAmber400
                        )
                    }
                    if (filter.activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(HatchEmerald400),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(filter.activeFilterCount.toString(), color = HatchBgDark, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            if (searchFocused && searchHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HatchSurfaceDark)
                        .border(1.dp, HatchBorderDark, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.recentSearches, color = HatchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.clearAll,
                            color = HatchAmber400,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                onClearSearchHistory()
                                searchFocused = false
                            }
                        )
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        searchHistory.forEach { term ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(HatchCardElevated)
                                    .clickable {
                                        onUpdateFilter { it.copy(searchQuery = term) }
                                        searchFocused = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = HatchTextMuted, modifier = Modifier.size(13.dp))
                                Text(term, color = HatchTextPrimary, fontSize = 11.sp)
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = HatchTextMuted,
                                    modifier = Modifier.size(13.dp).clickable { onRemoveSearchHistory(term) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryChip(strings.allCategories, filter.selectedCategory == "all") {
                    onUpdateFilter { it.copy(selectedCategory = "all") }
                }
                CategoryId.entries.forEach { cat ->
                    CategoryChip(strings.categoryLabel(cat.wireValue), filter.selectedCategory == cat.wireValue) {
                        onUpdateFilter { it.copy(selectedCategory = cat.wireValue) }
                    }
                }
            }

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.AR) "لا توجد منتجات مطابقة للفلاتر الحالية" else "No listings match your filters",
                            color = HatchTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = {
                            onSelectCity("all")
                            onUpdateFilter { MarketFilterState() }
                        }) {
                            Text(strings.resetFilters, color = HatchAmber400)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    products.forEachIndexed { idx, product ->
                        item(key = "prod_${product.id}") {
                            ProductCard(
                                product = product,
                                isFavorite = favoriteIds.contains(product.id),
                                language = language,
                                strings = strings,
                                onClick = { onOpenProduct(product) },
                                onToggleFavorite = { onToggleFavorite(product.id) }
                            )
                        }
                        if (adConfig.isEnabled && (idx == 1 || idx == 6) && nativeAds.isNotEmpty()) {
                            val ad = nativeAds[(idx / 5) % nativeAds.size]
                            item(key = "native_ad_${idx}_${ad.id}", span = { GridItemSpan(2) }) {
                                AdMobNativeCard(ad, language, strings, onAdImpression, onAdClick)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, containerColor = HatchSurfaceDark) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.filters, color = HatchTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = strings.resetFilters,
                        color = HatchAmber400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onSelectCity("all")
                            onUpdateFilter { MarketFilterState(searchQuery = it.searchQuery, selectedCategory = it.selectedCategory) }
                        }
                    )
                }

                Text(strings.allCities, color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryChip(strings.allCities, filter.selectedCity == "all") { onSelectCity("all") }
                    SudaneseCities.ALL.forEach { city ->
                        CategoryChip(city.substringBefore(" ("), filter.selectedCity == city) { onSelectCity(city) }
                    }
                }

                Text(strings.conditionLabel, color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryChip(strings.allConditions, filter.selectedCondition == "all") {
                        onUpdateFilter { it.copy(selectedCondition = "all") }
                    }
                    ProductCondition.entries.forEach { cond ->
                        CategoryChip(strings.conditionLabel(cond.wireValue), filter.selectedCondition == cond.wireValue) {
                            onUpdateFilter { it.copy(selectedCondition = cond.wireValue) }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.priceRange, color = HatchTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.unlimitedPrice, color = HatchTextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = filter.unlimitedPrice,
                            onCheckedChange = { checked -> onUpdateFilter { it.copy(unlimitedPrice = checked) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = HatchBgDark, checkedTrackColor = HatchAmber500)
                        )
                    }
                }
                if (!filter.unlimitedPrice) {
                    Text(HatchFormatters.formatSdg(filter.maxPrice, language), color = HatchAmber400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(
                        value = filter.maxPrice.toFloat().coerceIn(50_000f, 10_000_000f),
                        onValueChange = { value -> onUpdateFilter { it.copy(maxPrice = value.toDouble()) } },
                        valueRange = 50_000f..10_000_000f,
                        colors = SliderDefaults.colors(thumbColor = HatchAmber500, activeTrackColor = HatchAmber500)
                    )
                }

                FilterCheckboxRow(strings.onlyBankak, filter.onlyBankak) { v -> onUpdateFilter { it.copy(onlyBankak = v) } }
                FilterCheckboxRow(strings.onlyUrgent, filter.onlyUrgent) { v -> onUpdateFilter { it.copy(onlyUrgent = v) } }
                FilterCheckboxRow(strings.onlyNegotiable, filter.onlyNegotiable) { v -> onUpdateFilter { it.copy(onlyNegotiable = v) } }

                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(strings.applyFilters, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) HatchAmber500 else HatchSurfaceDark)
            .border(1.dp, if (selected) HatchAmber500 else HatchBorderSubtle, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) HatchBgDark else HatchTextSecondary, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium)
    }
}

@Composable
private fun FilterCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HatchCardElevated)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = HatchTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = HatchAmber500, checkmarkColor = HatchBgDark))
    }
}
