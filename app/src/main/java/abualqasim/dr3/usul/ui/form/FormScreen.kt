// file: app/src/main/java/abualqasim/dr3/usul/ui/form/FormScreen.kt
package abualqasim.dr3.usul.ui.form

import abualqasim.dr3.usul.ui.MainViewModel
import abualqasim.dr3.usul.ui.components.LocationDialogWithSuggestions
import abualqasim.dr3.usul.ui.components.SearchableDropdown
import abualqasim.dr3.usul.data.db.Category
import abualqasim.dr3.usul.data.db.Material
import abualqasim.dr3.usul.data.db.Surface
import abualqasim.dr3.usul.ui.components.AssistChipRow
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    nav: NavController,
    fromId: String? = null,
    isEdit: Boolean = false,
    vm: MainViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedMaterial by remember { mutableStateOf<Material?>(null) }
    var selectedSurface by remember { mutableStateOf<Surface?>(null) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var materials by remember { mutableStateOf<List<Material>>(emptyList()) }
    var surfaces by remember { mutableStateOf<List<Surface>>(emptyList()) }

    // photos
    var nearPhotoPath by remember { mutableStateOf<String?>(null) }
    var farPhotoPath  by remember { mutableStateOf<String?>(null) }
    var takingNear by remember { mutableStateOf(true) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    fun newTargetFile(tag: String) =
        File(context.cacheDir, "${tag}_${System.currentTimeMillis()}.jpg")

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            showPhotoDialog = true
        } else {
            if (takingNear) nearPhotoPath = null else farPhotoPath = null
        }
    }

    // focus order
    val takePhotoFR = remember { FocusRequester() }
    val titleFR = remember { FocusRequester() }
    val categoryFR = remember { FocusRequester() }
    val materialFR = remember { FocusRequester() }
    val surfaceFR = remember { FocusRequester() }
    val descFR = remember { FocusRequester() }

    var showLocationDialog by remember { mutableStateOf(false) }
    val sessionCity by vm.sessionCity.collectAsStateWithLifecycle()
    val sessionDistrict by vm.sessionDistrict.collectAsStateWithLifecycle()

    LaunchedEffect(fromId, isEdit) {
        vm.beginFormSession(fromId = fromId, isEdit = isEdit)
        categories = vm.loadCategories()
        surfaces = vm.loadSurfaces()
        if (!fromId.isNullOrBlank()) {
            vm.getEntryById(fromId)?.let { e ->
                title = if (isEdit) e.title.orEmpty() else ""
                description = if (isEdit) e.description.orEmpty() else ""
                selectedCategory = e.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
                if (selectedCategory != null) {
                    materials = vm.loadMaterialsFor(selectedCategory!!.id)
                    selectedMaterial = e.materialId?.let { id -> materials.firstOrNull { it.id == id } }
                }
                selectedSurface = e.surfaceId?.let { id -> surfaces.firstOrNull { it.id == id } }
                nearPhotoPath = e.nearPhotoPath
                farPhotoPath = e.farPhotoPath
            }
        }
        val city = vm.sessionCityOnce()
        if (city.isNullOrBlank()) showLocationDialog = true
    }

    LaunchedEffect(selectedCategory?.id) {
        materials = vm.loadMaterialsFor(selectedCategory?.id)
    }

    BackHandler { vm.endFormSession(); nav.popBackStack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEdit) "Edit Entry" else if (fromId != null) "Copy Entry" else "New Entry") }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { vm.endFormSession(); nav.popBackStack() }) { Text("Cancel") }
                Button(onClick = {
                    scope.launch {
                        if (isEdit && fromId != null) {
                            vm.updateEntry(
                                id = fromId,
                                title = title.ifBlank { null },
                                categoryId = selectedCategory?.id,
                                materialId = selectedMaterial?.id,
                                surfaceId = selectedSurface?.id,
                                description = description.ifBlank { null },
                                nearPhotoPath = nearPhotoPath,
                                farPhotoPath = farPhotoPath
                            )
                            Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show()
                            vm.endFormSession()
                            nav.popBackStack()
                        } else {
                            val newId = vm.saveNew(
                                title = title.ifBlank { null },
                                categoryId = selectedCategory?.id,
                                materialId = selectedMaterial?.id,
                                surfaceId = selectedSurface?.id,
                                description = description.ifBlank { null },
                                nearPhotoPath = nearPhotoPath,
                                farPhotoPath = farPhotoPath
                            )
                            if (newId.isNotBlank()) {
                                vm.movePhotosToCategoryFolder(
                                    context = context,
                                    entryId = newId,
                                    categoryName = selectedCategory?.nameEn.orEmpty(),
                                    nearPath = nearPhotoPath,
                                    farPath = farPhotoPath
                                )
                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Empty entry not saved", Toast.LENGTH_SHORT).show()
                            }
                            vm.endFormSession()
                            nav.popBackStack()
                        }
                    }
                }) { Text("Save") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            AssistChipRow(sessionCity, sessionDistrict)
            Spacer(Modifier.height(12.dp))

            // PHOTOS FIRST
            Text("Photos (optional)")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f).focusRequester(takePhotoFR),
                    onClick = {
                        takingNear = true
                        val target = newTargetFile("near")
                        nearPhotoPath = target.absolutePath
                        cameraLauncher.launch(Uri.fromFile(target))
                    }
                ) { Text("Take Near") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        takingNear = false
                        val target = newTargetFile("far")
                        farPhotoPath = target.absolutePath
                        cameraLauncher.launch(Uri.fromFile(target))
                    }
                ) { Text("Take Far") }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    if (nearPhotoPath != null) {
                        AsyncImage(model = File(nearPhotoPath!!), contentDescription = "Near photo",
                            modifier = Modifier.fillMaxWidth().height(120.dp))
                    } else Text("Near: none")
                }
                Box(Modifier.weight(1f)) {
                    if (farPhotoPath != null) {
                        AsyncImage(model = File(farPhotoPath!!), contentDescription = "Far photo",
                            modifier = Modifier.fillMaxWidth().height(120.dp))
                    } else Text("Far: none")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFR),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { categoryFR.requestFocus() })
            )

            Spacer(Modifier.height(8.dp))

            Box(Modifier.focusRequester(categoryFR)) {
                SearchableDropdown(
                    label = "Category",
                    items = categories,
                    selected = selectedCategory,
                    textOf = { it.nameEn }, // supports Arabic
                    onSelected = { cat ->
                        val changed = (cat?.id != selectedCategory?.id)
                        selectedCategory = cat
                        if (changed) selectedMaterial = null
                    },
                    onAddCustom = { newName ->
                        scope.launch {
                            val id = vm.addCustomCategory(newName)
                            categories = vm.loadCategories()
                            selectedCategory = categories.firstOrNull { it.id == id }
                        }
                    },
                    onNext = { materialFR.requestFocus() }
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(Modifier.focusRequester(materialFR)) {
                SearchableDropdown(
                    label = "Material",
                    items = materials,
                    selected = selectedMaterial,
                    textOf = { it.nameEn },
                    onSelected = { selectedMaterial = it },
                    onAddCustom = { newName ->
                        scope.launch {
                            val id = vm.addCustomMaterial(newName)
                            materials = vm.loadMaterialsFor(selectedCategory?.id)
                            selectedMaterial = materials.firstOrNull { it.id == id }
                        }
                    },
                    onNext = { surfaceFR.requestFocus() }
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(Modifier.focusRequester(surfaceFR)) {
                SearchableDropdown(
                    label = "Surface",
                    items = surfaces,
                    selected = selectedSurface,
                    textOf = { it.nameEn },
                    onSelected = { selectedSurface = it },
                    onAddCustom = { newName ->
                        scope.launch {
                            val id = vm.addCustomSurface(newName)
                            surfaces = vm.loadSurfaces()
                            selectedSurface = surfaces.firstOrNull { it.id == id }
                        }
                    },
                    onNext = { descFR.requestFocus() }
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().focusRequester(descFR),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { takePhotoFR.requestFocus() })
            )
        }

        if (showLocationDialog) {
            LocationDialogWithSuggestions(
                initialCity = sessionCity.orEmpty(),
                initialDistrict = sessionDistrict.orEmpty(),
                citySuggestions = vm.citySuggestions,
                districtSuggestions = vm.districtSuggestions,
                onDismiss = { showLocationDialog = false },
                onSave = { city, district ->
                    vm.setSessionCity(city, district)
                    showLocationDialog = false
                }
            )
        }

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("Photo captured") },
                text = { Text(if (takingNear) "Near photo taken" else "Far photo taken") },
                confirmButton = {
                    Button(onClick = { showPhotoDialog = false }) { Text("Save") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            takingNear = !takingNear
                            val target = if (takingNear) newTargetFile("near") else newTargetFile("far")
                            if (takingNear) nearPhotoPath = target.absolutePath else farPhotoPath = target.absolutePath
                            cameraLauncher.launch(Uri.fromFile(target))
                            showPhotoDialog = false
                        }) { Text("Take Second") }
                        OutlinedButton(onClick = {
                            try {
                                val p = if (takingNear) nearPhotoPath else farPhotoPath
                                if (p != null) File(p).delete()
                                if (takingNear) nearPhotoPath = null else farPhotoPath = null
                            } catch (_: Throwable) {}
                            showPhotoDialog = false
                        }) { Text("Cancel") }
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) { takePhotoFR.requestFocus() }
}