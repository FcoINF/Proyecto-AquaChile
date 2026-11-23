package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaOptionPicker(
    title: String,
    value: String,
    options: List<String>,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    onSelected: (String) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        Surface(
            onClick = { open = true },
            enabled = enabled,
            modifier = modifier.fillMaxWidth().semantics { contentDescription = label ?: title },
            shape = shape,
            color = colors.background,
            border = BorderStroke(1.dp, if (open) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) colors.onSurfaceVariant else colors.onSurface
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = colors.primary)
            }
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    IconButton(onClick = { open = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar selector")
                    }
                }
                Column(
                    Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { option ->
                        val selected = option == value
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
                                .background(if (selected) colors.primaryContainer else colors.background)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = {
                                    open = false
                                    onSelected(option)
                                })
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(option, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
