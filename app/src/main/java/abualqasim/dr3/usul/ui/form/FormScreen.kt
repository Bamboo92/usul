// file: app/src/main/java/abualqasim/dr3/usul/ui/form/FormScreen.kt
package abualqasim.dr3.usul.ui.form

import abualqasim.dr3.usul.data.db.Category
import abualqasim.dr3.usul.data.db.Material
import abualqasim.dr3.usul.data.db.Surface
import abualqasim.dr3.usul.ui.MainViewModel
import abualqasim.dr3.usul.ui.camera.CameraCaptureContract
import abualqasim.dr3.usul.ui.camera.CameraCaptureRequest
import abualqasim.dr3.usul.ui.camera.PhotoType
import abualqasim.dr3.usul.ui.components.AssistChipRow
import abualqasim.dr3.usul.ui.components.LocationDialogWithSuggestions
import abualqasim.dr3.usul.ui.components.SearchableDropdown
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    var farPhotoPath by remember { mutableStateOf<String?>(null) }

    var photoViewer by remember { mutableStateOf<PhotoViewerState?>(null) }

    fun clearPhoto(type: PhotoType) {
        when (type) {
            PhotoType.NEAR -> {
                nearPhotoPath?.let { runCatching { File(it).delete() } }
                nearPhotoPath = null
            }
            PhotoType.FAR -> {
                farPhotoPath?.let { runCatching { File(it).delete() } }
                farPhotoPath = null
            }
        }
        if (photoViewer?.type == type) {
            photoViewer = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(CameraCaptureContract()) { result ->
        result ?: return@rememberLauncherForActivityResult
        if (result.nearPath != nearPhotoPath) {
            nearPhotoPath?.takeIf { it != result.nearPath }?.let { runCatching { File(it).delete() } }
            nearPhotoPath = result.nearPath
        }
        if (result.farPath != farPhotoPath) {
            farPhotoPath?.takeIf { it != result.farPath }?.let { runCatching { File(it).delete() } }
            farPhotoPath = result.farPath
        }
    }

    // focus order
    val titleFR = remember { FocusRequester() }
    val categoryFR = remember { FocusRequester() }
    val materialFR = remember { FocusRequester() }
    val surfaceFR = remember { FocusRequester() }
    val descFR = remember { FocusRequester() }
    val saveFR = remember { FocusRequester() }

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

    val formScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEdit) "Edit Entry" else if (fromId != null) "Copy Entry" else "New Entry") }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { vm.endFormSession(); nav.popBackStack() }) { Text("Cancel") }
                Button(
                    modifier = Modifier
                        .focusRequester(saveFR)
                        .focusable(),
                    onClick = {
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
                                    farPath = farPhotoPath,
                                    title = title
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(formScrollState),
            verticalArrangement = Arrangement.Top
        ) {
            AssistChipRow(sessionCity, sessionDistrict)
            Spacer(Modifier.height(12.dp))

            // PHOTOS FIRST
            PhotoCaptureRow(
                nearPhotoPath = nearPhotoPath,
                farPhotoPath = farPhotoPath,
                onCapture = { type ->
                    cameraLauncher.launch(
                        CameraCaptureRequest(
                            startType = type,
                            title = title,
                            nearPath = nearPhotoPath,
                            farPath = farPhotoPath
                        )
                    )
                },
                onView = { type, path ->
                    photoViewer = PhotoViewerState(type, path)
                }
            )

            Spacer(Modifier.height(24.dp))

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
                keyboardActions = KeyboardActions(onNext = { saveFR.requestFocus() })
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

        photoViewer?.let { viewer ->
            PhotoViewerDialog(
                label = if (viewer.type == PhotoType.NEAR) "Near photo" else "Far photo",
                path = viewer.path,
                onDelete = { clearPhoto(viewer.type) },
                onDismiss = { photoViewer = null }
            )
        }

    }

}
private data class PhotoViewerState(val type: PhotoType, val path: String)

@Composable
private fun PhotoCaptureRow(
    nearPhotoPath: String?,
    farPhotoPath: String?,
    onCapture: (PhotoType) -> Unit,
    onView: (PhotoType, String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Photos (optional)")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoPreviewCard(
                modifier = Modifier.weight(1f),
                label = "Near",
                filePath = nearPhotoPath,
                onClick = {
                    if (nearPhotoPath != null) onView(PhotoType.NEAR, nearPhotoPath) else onCapture(PhotoType.NEAR)
                }
            )

            PhotoPreviewCard(
                modifier = Modifier.weight(1f),
                label = "Far",
                filePath = farPhotoPath,
                onClick = {
                    if (farPhotoPath != null) onView(PhotoType.FAR, farPhotoPath) else onCapture(PhotoType.FAR)
                }
            )
        }
    }
}

@Composable
private fun PhotoPreviewCard(
    modifier: Modifier = Modifier,
    label: String,
    filePath: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)

            if (filePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(filePath))
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$label photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "Tap to view or delete",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to capture",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewerDialog(
    label: String,
    path: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(path))
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$label photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentScale = ContentScale.Fit
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    ) {
                        Text("Delete")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
