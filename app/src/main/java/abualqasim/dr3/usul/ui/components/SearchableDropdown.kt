// file: app/src/main/java/abualqasim/dr3/usul/ui/components/SearchableDropdown.kt
package abualqasim.dr3.usul.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropdown(
    label: String,
    items: List<T>,
    selected: T?,
    textOf: (T) -> String,
    onSelected: (T?) -> Unit,
    onAddCustom: (String) -> Unit,
    onNext: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selected?.let(textOf) ?: "") }

    LaunchedEffect(selected) { query = selected?.let(textOf) ?: "" }

    val filtered = remember(query, items) {
        if (query.isBlank()) items else items.filter { textOf(it).contains(query, true) }
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            keyboardOptions = KeyboardOptions(imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(
                onNext = { onNext?.invoke() },
                onDone = { onNext?.invoke() }
            )
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.take(8).forEach { item ->
                DropdownMenuItem(
                    text = { Text(textOf(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
            if (query.isNotBlank() && filtered.none { textOf(it).equals(query, true) }) {
                DropdownMenuItem(
                    text = { Text("+ Add \"$query\"") },
                    onClick = {
                        onAddCustom(query.trim())
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("— Clear —") },
                onClick = { onSelected(null); expanded = false }
            )
        }
    }
}
