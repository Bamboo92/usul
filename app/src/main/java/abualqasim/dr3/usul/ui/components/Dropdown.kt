package abualqasim.dr3.usul.ui.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SimpleDropdown(
    label: String,
    items: List<T>,
    selected: T?,
    textOf: (T) -> String,
    onSelected: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.let(textOf) ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            value = display,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("— None —") },
                onClick = { onSelected(null); expanded = false }
            )
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(textOf(item)) },
                    onClick = { onSelected(item); expanded = false }
                )
            }
        }
    }
}