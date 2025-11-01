// file: app/src/main/java/abualqasim/dr3/usul/ui/MainViewModel.kt
package abualqasim.dr3.usul.ui

import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import abualqasim.dr3.usul.data.db.AppDb
import abualqasim.dr3.usul.data.db.Category
import abualqasim.dr3.usul.data.db.Material
import abualqasim.dr3.usul.data.db.Surface
import abualqasim.dr3.usul.data.repo.EntryRepository
import abualqasim.dr3.usul.session.SessionStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import abualqasim.dr3.usul.util.exportEntriesToExcel

data class VocabMaps(
    val cat: Map<Long, String> = emptyMap(),
    val mat: Map<Long, String> = emptyMap(),
    val sur: Map<Long, String> = emptyMap()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDb.get(app)
    private val repo = EntryRepository(db)
    private val session = SessionStore(app)

    val uiEntries = repo.entries.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _sessionCity = MutableStateFlow<String?>(null)
    private val _sessionDistrict = MutableStateFlow<String?>(null)
    private val _showLocationPrompt = MutableStateFlow(false)

    val showLocationPrompt = _showLocationPrompt.asStateFlow()
    val sessionCity = _sessionCity.asStateFlow()
    val sessionDistrict = _sessionDistrict.asStateFlow()

    val citySuggestions = listOf("Berlin","Hamburg","München","Köln","Frankfurt","Düsseldorf","Stuttgart")
    val districtSuggestions = listOf("Mitte","Nord","Süd","Ost","West")

    private val _vocabMaps = MutableStateFlow(VocabMaps())
    val vocabMaps: StateFlow<VocabMaps> = _vocabMaps

    fun refreshVocab() = viewModelScope.launch {
        val cats = repo.categories().associate { it.id to it.nameEn }
        val mats = db.materialDao().all().associate { it.id to it.nameEn }
        val surs = repo.surfaces().associate { it.id to it.nameEn }
        _vocabMaps.value = VocabMaps(cats, mats, surs)
    }

    fun beginFormSession(fromId: String? = null, isEdit: Boolean = false) = viewModelScope.launch {
        val lastCity = session.cityFlow.firstOrNull().orEmpty()
        val lastDistrict = session.districtFlow.firstOrNull().orEmpty()
        _sessionCity.value = lastCity.ifBlank { null }
        _sessionDistrict.value = lastDistrict.ifBlank { null }
        _showLocationPrompt.value = (fromId == null && !isEdit)
    }

    fun setSessionCity(city: String, district: String) = viewModelScope.launch {
        _sessionCity.value = city
        _sessionDistrict.value = district
        session.setCity(city, district)
    }

    fun setSessionCityAndHideDialog(city: String, district: String) = viewModelScope.launch {
        setSessionCity(city, district)
        _showLocationPrompt.value = false
    }

    fun dismissLocationDialog() { _showLocationPrompt.value = false }
    fun endFormSession() { _sessionCity.value = null; _sessionDistrict.value = null; _showLocationPrompt.value = false }

    suspend fun getEntryById(id: String) = repo.get(id)
    suspend fun loadCategories(): List<Category> = repo.categories()
    suspend fun loadMaterialsFor(categoryId: Long?): List<Material> = repo.materialsForCategory(categoryId)
    suspend fun loadSurfaces(): List<Surface> = repo.surfaces()

    suspend fun saveNew(
        title: String?,
        categoryId: Long?,
        materialId: Long?,
        surfaceId: Long?,
        description: String?,
        nearPhotoPath: String?,
        farPhotoPath: String?
    ): String {
        val isAllEmpty = (title.isNullOrBlank()
                && categoryId == null
                && materialId == null
                && surfaceId == null
                && description.isNullOrBlank()
                && nearPhotoPath.isNullOrBlank()
                && farPhotoPath.isNullOrBlank())
        if (isAllEmpty) return ""

        val id = java.util.UUID.randomUUID().toString()
        val city = _sessionCity.value
        val dist = _sessionDistrict.value
        val hash = session.userHashFlow.firstOrNull().orEmpty().ifBlank { "stub-hash" }

        repo.createDraftWithId(
            id = id,
            title = title,
            categoryId = categoryId,
            materialId = materialId,
            surfaceId = surfaceId,
            description = description,
            city = city,
            district = dist,
            creatorHash = hash,
            nearPhotoPath = nearPhotoPath,
            farPhotoPath = farPhotoPath
        )
        return id
    }

    fun updateEntry(
        id: String,
        title: String?,
        categoryId: Long?,
        materialId: Long?,
        surfaceId: Long?,
        description: String?,
        nearPhotoPath: String?,
        farPhotoPath: String?
    ) = viewModelScope.launch {
        val entry = repo.get(id) ?: return@launch
        val updated = entry.copy(
            title = title,
            categoryId = categoryId,
            materialId = materialId,
            surfaceId = surfaceId,
            description = description,
            nearPhotoPath = nearPhotoPath ?: entry.nearPhotoPath,
            farPhotoPath = farPhotoPath ?: entry.farPhotoPath,
            updatedAt = System.currentTimeMillis()
        )
        repo.update(updated)
    }

    fun duplicate(id: String) = viewModelScope.launch { repo.duplicateToDraft(id) }

    fun delete(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val entry = repo.get(id)
        repo.delete(id)
        listOf(entry?.nearPhotoPath, entry?.farPhotoPath)
            .mapNotNull { it?.let(::File) }
            .forEach { if (it.exists()) it.delete() }
    }

    suspend fun addCustomCategory(name: String): Long = repo.addCustomCategory(name)
    suspend fun addCustomMaterial(name: String): Long = repo.addCustomMaterial(name)
    suspend fun addCustomSurface(name: String): Long = repo.addCustomSurface(name)

    fun addCustomCategoryAndTie(name: String, categoryIdToTie: Long? = null, materialIdToTie: Long? = null) =
        viewModelScope.launch {
            val newId = repo.addCustomCategory(name)
            if (materialIdToTie != null) repo.tieCategoryMaterial(newId, materialIdToTie)
        }

    suspend fun sessionCityOnce(): String? = session.cityOnce()

    suspend fun movePhotosToCategoryFolder(
        context: Context,
        categoryName: String?,
        nearPath: String?,
        farPath: String?,
        title: String,
    ): Pair<String?, String?> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val categoryFolderName = categoryName?.ifBlank { null } ?: "غير مصنف"
        val folder = File(root, "photos/$categoryFolderName").apply { mkdirs() }

        fun sanitizeTitle(raw: String): String {
            return raw.ifBlank { "Entry" }
                .replace(Regex("[^A-Za-z0-9_\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF ]"), " ")
                .trim()
                .ifBlank { "Entry" }
        }

        val safeTitle = sanitizeTitle(title)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(Date())
        val targetRoot = folder.canonicalFile

        fun moveIfNeeded(path: String?, tag: String): String? {
            val src = path?.let(::File) ?: return null
            if (!src.exists()) return null
            val parent = src.canonicalFile.parentFile
            if (parent?.canonicalPath == targetRoot.canonicalPath) {
                return src.absolutePath
            }
            val destName = "$safeTitle $timestamp $tag.jpg"
            val dest = File(targetRoot, destName)
            return try {
                src.copyTo(dest, overwrite = true)
                if (src.canonicalPath != dest.canonicalPath) {
                    src.delete()
                }
                dest.absolutePath
            } catch (_: Throwable) {
                path
            }
        }

        val newNear = moveIfNeeded(nearPath, "Near")
        val newFar = moveIfNeeded(farPath, "Far")
        newNear to newFar
    }

    fun exportEntries(
        context: Context,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = exportEntriesToExcel(context.applicationContext, db)
                withContext(Dispatchers.Main) { onResult(Result.success(file)) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) { onResult(Result.failure(t)) }
            }
        }
    }
}
