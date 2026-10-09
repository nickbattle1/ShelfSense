package com.example.shelfsense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val c = ShelfTheme.colors
    Button(
        // stays enabled while loading so the colour doesn't flash grey, repeat taps are just ignored
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary,
            contentColor = c.onPrimary,
            disabledContainerColor = c.disabled,
            disabledContentColor = c.muted
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .semantics { if (loading) stateDescription = "Working" }
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.onPrimary, strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = ShelfTheme.colors.primary
) {
    val c = ShelfTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, if (enabled) contentColor else c.line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.surface, contentColor = contentColor),
        modifier = modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = ShelfTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        // the solid fill shows the selection, and FilterChip tells TalkBack it's selected.
        // no tick icon, since adding one widens the chip and shoves its neighbours along
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = c.surface,
            labelColor = c.ink,
            selectedContainerColor = c.primary,
            selectedLabelColor = c.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = c.line,
            selectedBorderColor = c.primary
        )
    )
}
