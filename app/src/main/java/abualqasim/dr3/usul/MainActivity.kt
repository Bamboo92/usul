// file: app/src/main/java/abualqasim/dr3/usul/ui/MainActivity.kt
package abualqasim.dr3.usul.ui

import abualqasim.dr3.usul.ui.form.FormScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppNavHost() } }
    }
}

@Composable
fun AppNavHost(start: String = "main") {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start) {
        composable("main") { MainScreen(nav) }
        composable("form") { FormScreen(nav) }
        composable("form?fromId={fromId}&edit={edit}") { backStackEntry ->
            val fromId = backStackEntry.arguments?.getString("fromId")
            val isEdit = backStackEntry.arguments?.getString("edit") == "true"
            FormScreen(nav, fromId = fromId, isEdit = isEdit)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    nav: NavController,
    vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val entries by vm.uiEntries.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    var lastExportPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Entries") },
                actions = {
                    TextButton(onClick = {
                        // implement export if needed; placeholder toast
                        Toast.makeText(context, "Use your export function here", Toast.LENGTH_SHORT).show()
                    }) { Text("Export") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("form") }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(entries, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text(e.title ?: "(no title)") },
                    supportingContent = {
                        Column {
                            Text("${e.city ?: "-"} / ${e.district ?: "-"}")
                            if (!e.description.isNullOrBlank())
                                Text(e.description!!, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.clickable { nav.navigate("form?fromId=${e.id}&edit=true") },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { nav.navigate("form?fromId=${e.id}") }) { Text("Copy") }
                            TextButton(onClick = { vm.delete(e.id) }) { Text("Delete") }
                        }
                    }
                )
                Divider()
            }
        }
    }
}
