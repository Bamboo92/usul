// file: app/src/main/java/abualqasim/dr3/usul/ui/components/AssistChipRow.kt
package abualqasim.dr3.usul.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun AssistChipRow(city: String?, district: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!city.isNullOrBlank()) AssistChip(onClick = {}, label = { Text("City: $city") })
        if (!district.isNullOrBlank()) AssistChip(onClick = {}, label = { Text("District: $district") })
    }
}
