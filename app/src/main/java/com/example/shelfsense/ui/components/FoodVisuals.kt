package com.example.shelfsense.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shelfsense.R
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.ActionInfo
import com.example.shelfsense.domain.Urgency
import com.example.shelfsense.ui.theme.ShelfTheme
import java.io.File

fun FoodCategory.icon(): ImageVector = when (this) {
    FoodCategory.FRUIT_VEG -> Icons.Filled.Eco
    FoodCategory.DAIRY -> Icons.Filled.LocalDrink
    FoodCategory.MEAT_SEAFOOD -> Icons.Filled.SetMeal
    FoodCategory.BAKERY -> Icons.Filled.BakeryDining
    FoodCategory.PANTRY_SAUCES -> Icons.Filled.Opacity
    FoodCategory.DRY_GOODS -> Icons.Filled.Grain
    FoodCategory.FROZEN -> Icons.Filled.AcUnit
    FoodCategory.LEFTOVERS -> Icons.Filled.RiceBowl
}

fun StorageLocation.icon(): ImageVector = when (this) {
    StorageLocation.FRIDGE -> Icons.Filled.Kitchen
    StorageLocation.FREEZER -> Icons.Filled.AcUnit
    StorageLocation.PANTRY -> Icons.Filled.Inventory2
}

// the illustrated produce from the prototype, used when a fresh item has no photo
private fun produceArtFor(name: String): Int? {
    val n = name.lowercase()
    return when {
        "tomato" in n -> R.drawable.tomatoes
        "lettuce" in n -> R.drawable.lettuce
        "milk" in n -> R.drawable.milk
        else -> null
    }
}

// order of preference: the person's own photo, the Open Food Facts photo,
// the prototype's produce art, then the category icon
@Composable
fun FoodThumb(
    name: String,
    category: FoodCategory,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    photoPath: String? = null
) {
    val c = ShelfTheme.colors
    val art = remember(name) { produceArtFor(name) }
    val model: Any? = photoPath?.let { File(it) } ?: imageUrl
    when {
        model != null -> Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .border(1.dp, c.line, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // the category icon sits underneath, so a slow, missing or failed image still shows something
            Icon(category.icon(), contentDescription = null, tint = c.muted, modifier = Modifier.size(size * 0.45f))
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        art != null -> Image(painterResource(art), contentDescription = null, modifier = modifier.size(size))
        else -> Box(
            modifier.size(size).background(c.tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon(), contentDescription = null, tint = c.primary, modifier = Modifier.size(size * 0.5f))
        }
    }
}

data class UrgencyStyle(val fg: Color, val bg: Color, val icon: ImageVector)

// a null urgency means the item has no date to count down to
@Composable
@ReadOnlyComposable
fun urgencyStyle(urgency: Urgency?): UrgencyStyle {
    val c = ShelfTheme.colors
    return when (urgency) {
        null -> UrgencyStyle(c.muted, c.chip, Icons.Filled.AllInclusive)
        Urgency.OVERDUE -> UrgencyStyle(c.urgent, c.urgentBg, Icons.Filled.Error)
        Urgency.TODAY, Urgency.URGENT -> UrgencyStyle(c.urgent, c.urgentBg, Icons.Filled.Warning)
        Urgency.SOON -> UrgencyStyle(c.warn, c.warnBg, Icons.Filled.Schedule)
        Urgency.LATER -> UrgencyStyle(c.primary, c.tint, Icons.Filled.CheckCircle)
    }
}

// urgency always shows as words beside an icon, never colour alone
@Composable
fun UrgencyLabel(info: ActionInfo?, modifier: Modifier = Modifier) {
    val style = urgencyStyle(info?.urgency)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            info?.let { ActionDates.daysLeftLabel(it.daysLeft) } ?: "No expiry",
            style = MaterialTheme.typography.bodyMedium,
            color = style.fg
        )
    }
}
