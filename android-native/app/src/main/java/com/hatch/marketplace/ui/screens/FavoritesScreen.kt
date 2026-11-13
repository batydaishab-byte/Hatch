package com.hatch.marketplace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.ui.components.ProductCard
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.theme.HatchAmber500
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchRose500
import com.hatch.marketplace.ui.theme.HatchTextMuted
import com.hatch.marketplace.ui.theme.HatchTextPrimary

@Composable
fun FavoritesScreen(
    favorites: List<Product>,
    language: AppLanguage,
    strings: HatchStrings,
    onOpenProduct: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExploreMarket: () -> Unit
) {
    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = HatchRose500, modifier = Modifier.size(56.dp))
                Text(
                    text = if (language == AppLanguage.AR) "قائمة المفضلة فارغة" else "No saved items yet",
                    color = HatchTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.AR) "اضغط على القلب لحفظ المنتجات ومقارنتها لاحقاً" else "Tap the heart on any item to save it here",
                    color = HatchTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onExploreMarket,
                    colors = ButtonDefaults.buttonColors(containerColor = HatchAmber500, contentColor = HatchBgDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.browseAllAds, fontWeight = FontWeight.Black)
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
            items(favorites, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    isFavorite = true,
                    language = language,
                    strings = strings,
                    onClick = { onOpenProduct(product) },
                    onToggleFavorite = { onToggleFavorite(product.id) }
                )
            }
        }
    }
}
