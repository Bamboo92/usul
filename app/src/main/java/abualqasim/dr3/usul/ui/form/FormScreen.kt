// file: app/src/main/java/abualqasim/dr3/usul/ui/form/FormScreen.kt
package abualqasim.dr3.usul.ui.form

import abualqasim.dr3.usul.data.db.Category
import abualqasim.dr3.usul.data.db.Material
import abualqasim.dr3.usul.data.db.Surface
import abualqasim.dr3.usul.ui.MainViewModel
import abualqasim.dr3.usul.ui.components.AssistChipRow
import abualqasim.dr3.usul.ui.components.LocationDialogWithSuggestions
import abualqasim.dr3.usul.ui.components.SearchableDropdown
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
    var farPhotoPath by remember { mutableStateOf<String?>(null) }
    var captureInProgress by remember { mutableStateOf<PhotoType?>(null) }
    var reviewPendingFor by remember { mutableStateOf<PhotoType?>(null) }

    fun newTargetFile(tag: String) =
        File(context.cacheDir, "${tag}_${System.currentTimeMillis()}.jpg")

    fun clearPhoto(type: PhotoType) {
        when (type) {
            PhotoType.NEAR -> {
                nearPhotoPath?.let { File(it).delete() }
                nearPhotoPath = null
            }
            PhotoType.FAR -> {
                farPhotoPath?.let { File(it).delete() }
                farPhotoPath = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val type = captureInProgress
        if (success) {
            if (type != null) {
                reviewPendingFor = type
            }
        } else {
            if (type != null) {
                clearPhoto(type)
            }
        }
        captureInProgress = null
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AssistChipRow(sessionCity, sessionDistrict)
            Spacer(Modifier.height(12.dp))

            // PHOTOS FIRST
            PhotoCaptureSection(
                nearPhotoPath = nearPhotoPath,
                farPhotoPath = farPhotoPath,
                reviewPendingFor = reviewPendingFor,
                onRequestCapture = { type ->
                    startCapture(
                        type = type,
                        newTargetFile = ::newTargetFile,
                        setPath = {
                            when (type) {
                                PhotoType.NEAR -> nearPhotoPath = it
                                PhotoType.FAR -> farPhotoPath = it
                            }
                        },
                        setCapture = { captureInProgress = it },
                        context = context,
                        cameraLauncher = cameraLauncher
                    )
                },
                onCancel = { pendingType ->
                    clearPhoto(pendingType)
                    reviewPendingFor = null
                    takePhotoFR.requestFocus()
                },
                onAccept = {
                    reviewPendingFor = null
                    takePhotoFR.requestFocus()
                },
                onNext = {
                    reviewPendingFor = null
                    clearPhoto(PhotoType.FAR)
                    startCapture(
                        type = PhotoType.FAR,
                        newTargetFile = ::newTargetFile,
                        setPath = { farPhotoPath = it },
                        setCapture = { captureInProgress = it },
                        context = context,
                        cameraLauncher = cameraLauncher
                    )
                },
                onRedo = { pendingType ->
                    clearPhoto(pendingType)
                    reviewPendingFor = null
                    startCapture(
                        type = pendingType,
                        newTargetFile = ::newTargetFile,
                        setPath = {
                            when (pendingType) {
                                PhotoType.NEAR -> nearPhotoPath = it
                                PhotoType.FAR -> farPhotoPath = it
                            }
                        },
                        setCapture = { captureInProgress = it },
                        context = context,
                        cameraLauncher = cameraLauncher
                    )
                },
                takePhotoRequester = takePhotoFR
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

    }

    LaunchedEffect(Unit) { takePhotoFR.requestFocus() }
}

private enum class PhotoType { NEAR, FAR }

private fun startCapture(
    type: PhotoType,
    newTargetFile: (String) -> File,
    setPath: (String) -> Unit,
    setCapture: (PhotoType) -> Unit,
    context: Context,
    cameraLauncher: ActivityResultLauncher<Uri>
) {
    val tag = when (type) {
        PhotoType.NEAR -> "near"
        PhotoType.FAR -> "far"
    }
    val target = newTargetFile(tag)
    setPath(target.absolutePath)
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        target
    )
    setCapture(type)
    cameraLauncher.launch(photoUri)
}

@Composable
private fun PhotoCaptureSection(
    nearPhotoPath: String?,
    farPhotoPath: String?,
    reviewPendingFor: PhotoType?,
    onRequestCapture: (PhotoType) -> Unit,
    onCancel: (PhotoType) -> Unit,
    onAccept: () -> Unit,
    onNext: () -> Unit,
    onRedo: (PhotoType) -> Unit,
    takePhotoRequester: FocusRequester,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Photos (optional)")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(takePhotoRequester),
                enabled = reviewPendingFor == null,
                onClick = { onRequestCapture(PhotoType.NEAR) }
            ) {
                Text("Take Near")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = reviewPendingFor == null,
                onClick = { onRequestCapture(PhotoType.FAR) }
            ) {
                Text("Take Far")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoPreview(
                modifier = Modifier.weight(1f),
                label = "Near",
                filePath = nearPhotoPath
            )

            PhotoPreview(
                modifier = Modifier.weight(1f),
                label = "Far",
                filePath = farPhotoPath
            )
        }

        AnimatedVisibility(visible = reviewPendingFor != null) {
            val pendingType = reviewPendingFor ?: return@AnimatedVisibility

            Column {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onCancel(pendingType) }
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onAccept
                    ) {
                        Text("Accept")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = pendingType == PhotoType.NEAR,
                        onClick = onNext
                    ) {
                        Text("Next")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onRedo(pendingType) }
                    ) {
                        Text("Redo")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPreview(
    modifier: Modifier = Modifier,
    label: String,
    filePath: String?,
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)

            if (filePath != null) {
                AsyncImage(
                    model = File(filePath),
                    contentDescription = "$label photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photo", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
