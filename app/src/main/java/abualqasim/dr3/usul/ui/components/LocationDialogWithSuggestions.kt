package abualqasim.dr3.usul.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun LocationDialogWithSuggestions(
    initialCity: String,
    initialDistrict: String,
    citySuggestions: List<String>,
    districtSuggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var city by remember { mutableStateOf(initialCity) }
    var district by remember { mutableStateOf(initialDistrict) }

    val cityFR = remember { FocusRequester() }
    val districtFR = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Location") },
        text = {
            Column {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.fillMaxWidth().focusRequester(cityFR),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { districtFR.requestFocus() })
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District") },
                    modifier = Modifier.fillMaxWidth().focusRequester(districtFR),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(city, district) })
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(city, district) }) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )

    LaunchedEffect(Unit) { cityFR.requestFocus() }
}
