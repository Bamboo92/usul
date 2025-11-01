package abualqasim.dr3.usul.ui.form

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import abualqasim.dr3.usul.ui.MainViewModel
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(emptyList<Category>()) }
    var materials by remember { mutableStateOf(emptyList<Material>()) }
    var surfaces by remember { mutableStateOf(emptyList<Surface>()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedMaterial by remember { mutableStateOf<Material?>(null) }
    var selectedSurface by remember { mutableStateOf<Surface?>(null) }
    var nearPhotoPath by remember { mutableStateOf<String?>(null) }
    var farPhotoPath by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()
    val titleFR = remember { FocusRequester() }
    val descFR = remember { FocusRequester() }
    val saveFR = remember { FocusRequester() }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) showPhotoDialog = true
    }

    LaunchedEffect(Unit) {
        categories = vm.loadCategories()
        surfaces = vm.loadSurfaces()

        if (fromId != null) {
            val e = vm.getEntryById(fromId)
            if (e != null) {
                title = e.title.orEmpty()
                description = e.description.orEmpty()
                nearPhotoPath = e.nearPhotoPath
                farPhotoPath = e.farPhotoPath
                selectedCategory = categories.firstOrNull { it.id == e.categoryId }
                materials = vm.loadMaterialsFor(selectedCategory?.id)
                selectedMaterial = materials.firstOrNull { it.id == e.materialId }
                selectedSurface = surfaces.firstOrNull { it.id == e.surfaceId }
            }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(if (isEdit) "Edit Entry" else "New Entry") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraField("Near", nearPhotoPath) {
                    val file = File(context.cacheDir, "near_${System.currentTimeMillis()}.jpg")
                    val uri = createImageTargetUri(context, file)
                    nearPhotoPath = file.absolutePath
                    cameraLauncher.launch(uri)
                }
                CameraField("Far", farPhotoPath) {
                    val file = File(context.cacheDir, "far_${System.currentTimeMillis()}.jpg")
                    val uri = createImageTargetUri(context, file)
                    farPhotoPath = file.absolutePath
                    cameraLauncher.launch(uri)
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFR),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { descFR.requestFocus() })
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descFR),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { saveFR.requestFocus() })
            )

            Spacer(Modifier.height(12.dp))
            SearchableDropdown(
                label = "Category",
                items = categories,
                selected = selectedCategory,
                textOf = { it.nameEn },
                onSelected = {
                    selectedCategory = it
                    scope.launch {
                        materials = vm.loadMaterialsFor(it?.id)
                        selectedMaterial = null
                    }
                },
                onAddCustom = { name ->
                    scope.launch {
                        val id = vm.repo.addCustomCategory(name)
                        categories = vm.loadCategories()
                        selectedCategory = categories.firstOrNull { it.id == id }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
            SearchableDropdown(
                label = "Material",
                items = materials,
                selected = selectedMaterial,
                textOf = { it.nameEn },
                onSelected = { selectedMaterial = it },
                onAddCustom = { name ->
                    scope.launch {
                        val id = vm.addCustomMaterialAndTie(name, selectedCategory?.id)
                        materials = vm.loadMaterialsFor(selectedCategory?.id)
                        selectedMaterial = materials.firstOrNull { it.id == id }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
            SearchableDropdown(
                label = "Surface",
                items = surfaces,
                selected = selectedSurface,
                textOf = { it.nameEn },
                onSelected = { selectedSurface = it },
                onAddCustom = { name ->
                    scope.launch {
                        val id = vm.repo.addCustomSurface(name)
                        surfaces = vm.loadSurfaces()
                        selectedSurface = surfaces.firstOrNull { it.id == id }
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            Button(
                modifier = Modifier.focusRequester(saveFR),
                onClick = {
                    scope.launch {
                        vm.saveNew(
                            title.takeIf { it.isNotBlank() },
                            selectedCategory?.id,
                            selectedMaterial?.id,
                            selectedSurface?.id,
                            description.takeIf { it.isNotBlank() },
                            nearPhotoPath,
                            farPhotoPath
                        )
                        nav.popBackStack()
                    }
                }
            ) { Text("Save Entry") }
        }
    }
}

@Composable
fun CameraField(label: String, path: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        if (path != null && File(path).exists()) {
            Image(
                painter = rememberAsyncImagePainter(path),
                contentDescription = label,
                modifier = Modifier.size(140.dp).clickable { onClick() }
            )
        } else {
            OutlinedButton(onClick = onClick, modifier = Modifier.size(140.dp)) {
                Text(label)
            }
        }
    }
}
