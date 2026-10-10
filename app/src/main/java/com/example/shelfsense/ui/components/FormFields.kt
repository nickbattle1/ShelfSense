package com.example.shelfsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.ui.theme.ShelfTheme
import java.time.LocalDate

// shared form parts from the prototype. labels sit above the field in small caps,
// and errors or helper text sit underneath so the field itself never jumps around

@Composable
private fun FieldLabel(text: String, isError: Boolean) {
    val c = ShelfTheme.colors
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (isError) c.urgent else c.muted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun FieldFootnote(error: String?, helper: String?) {
    val c = ShelfTheme.colors
    when {
        error != null -> Text(
            error,
            style = MaterialTheme.typography.labelMedium,
            color = c.urgent,
            modifier = Modifier.padding(top = 4.dp)
        )
        helper != null -> Text(
            helper,
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// white boxes with a warm outline, shared by every text, dropdown and date field
@Composable
fun fieldColors(errorOnDisabled: Boolean = false): TextFieldColors {
    val c = ShelfTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        disabledTextColor = c.ink,
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface,
        disabledContainerColor = c.surface,
        errorContainerColor = c.surface,
        cursorColor = c.primary,
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.line,
        // the date field is drawn disabled so it can't take focus, which also hides its error outline
        disabledBorderColor = if (errorOnDisabled) c.urgent else c.line,
        errorBorderColor = c.urgent,
        focusedLeadingIconColor = c.primary,
        unfocusedLeadingIconColor = c.primary,
        disabledLeadingIconColor = c.primary,
        focusedTrailingIconColor = c.muted,
        unfocusedTrailingIconColor = c.muted,
        disabledTrailingIconColor = c.muted,
        focusedPlaceholderColor = c.muted,
        unfocusedPlaceholderColor = c.muted,
        disabledPlaceholderColor = c.muted
    )
}

@Composable
fun TextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    autofill: ContentType? = null,
    enabled: Boolean = true,
    maxLength: Int = 60
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    // lets the password manager recognise email and password boxes
    val autofillModifier = if (autofill != null) Modifier.semantics { contentType = autofill } else Modifier
    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        FieldLabel(label, error != null)
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            enabled = enabled,
            placeholder = { Text(placeholder) },
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors(),
            visualTransformation = if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = {
                if (onImeAction != null) onImeAction() else defaultKeyboardAction(imeAction)
            }),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth().then(autofillModifier)
        )
        FieldFootnote(error, helper)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an option",
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    optionIcon: ((T) -> ImageVector)? = null
) {
    val c = ShelfTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        FieldLabel(label, error != null)
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) { expanded = it } }
        ) {
            OutlinedTextField(
                value = if (selected != null) optionLabel(selected) else "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                placeholder = { Text(placeholder) },
                leadingIcon = if (optionIcon != null && selected != null) {
                    { Icon(optionIcon(selected), contentDescription = null) }
                } else {
                    null
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = error != null,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        leadingIcon = if (optionIcon != null) {
                            { Icon(optionIcon(option), contentDescription = null, tint = c.primary) }
                        } else {
                            null
                        },
                        trailingIcon = if (option == selected) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected", tint = c.primary) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
        FieldFootnote(error, helper)
    }
}

@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    earliest: LocalDate? = null,
    latest: LocalDate? = null,
    placeholder: String = "Select a date"
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val shown = value?.let { Dates.long(it) }
    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        FieldLabel(label, error != null)
        Box {
            OutlinedTextField(
                value = shown ?: "",
                onValueChange = {},
                enabled = false,
                singleLine = true,
                placeholder = { Text(placeholder) },
                trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(errorOnDisabled = error != null),
                modifier = Modifier.fillMaxWidth()
            )
            // the disabled field ignores taps, so this overlay opens the picker instead
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Choose a date") { showPicker = true }
                    .semantics { contentDescription = "$label, ${shown ?: "not set"}" }
            )
        }
        FieldFootnote(error, helper)
    }
    if (showPicker) {
        DatePickerPopup(
            initial = value,
            earliest = earliest,
            latest = latest,
            onDismiss = { showPicker = false },
            onConfirm = { picked ->
                onPicked(picked)
                showPicker = false
            }
        )
    }
}

@Composable
private fun DatePickerPopup(
    initial: LocalDate?,
    earliest: LocalDate?,
    latest: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // the picker hands back UTC midnight, Dates converts it so the day never shifts by time zone
    val limits = remember(earliest, latest) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Dates.fromPickerMillis(utcTimeMillis)
                return (earliest == null || !day.isBefore(earliest)) && (latest == null || !day.isAfter(latest))
            }

            override fun isSelectableYear(year: Int): Boolean =
                (earliest == null || year >= earliest.year) && (latest == null || year <= latest.year)
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let { Dates.toPickerMillis(it) },
        selectableDates = limits
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(Dates.fromPickerMillis(it)) } },
                enabled = state.selectedDateMillis != null
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

// one tap shortcuts under a date field, the proposal's quick date selection
@Composable
fun QuickDateRow(options: List<Pair<String, LocalDate>>, selected: LocalDate?, onPick: (LocalDate) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, date) ->
            ChoiceChip(label, selected == date) { onPick(date) }
        }
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ShelfTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .border(1.dp, c.line, RoundedCornerShape(10.dp))
            .padding(4.dp)
            .selectableGroup()
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    // 48dp keeps each half at the minimum touch target, and the control lines up with the 56dp fields
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) c.primary else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) c.onPrimary else c.ink)
            }
        }
    }
}
