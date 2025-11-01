package abualqasim.dr3.usul.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import abualqasim.dr3.usul.data.db.*
import abualqasim.dr3.usul.data.repo.*
import abualqasim.dr3.usul.session.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDb.get(app)
    private val repo = EntryRepository(db)
    private val session = SessionStore(app)

    val uiEntries = repo.entries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getEntryById(id: String) = repo.get(id)

    suspend fun loadCategories() = repo.categories()
    suspend fun loadMaterialsFor(categoryId: Long?) = repo.materialsForCategory(categoryId)
    suspend fun loadSurfaces() = repo.surfaces()

    fun saveNew(
        title: String?,
        categoryId: Long?,
        materialId: Long?,
        surfaceId: Long?,
        description: String?,
        nearPhotoPath: String?,
        farPhotoPath: String?
    ) = viewModelScope.launch {
        val city = session.cityFlow.firstOrNull().orEmpty().ifBlank { null }
        val dist = session.districtFlow.firstOrNull().orEmpty().ifBlank { null }
        val hash = session.userHashFlow.firstOrNull().orEmpty().ifBlank { "stub-hash" }
        repo.createDraft(title, categoryId, materialId, surfaceId, description, city, dist, hash, nearPhotoPath, farPhotoPath)
    }

    fun duplicate(id: String) = viewModelScope.launch { repo.duplicateToDraft(id) }

    fun delete(id: String) = viewModelScope.launch {
        val entry = repo.get(id)
        repo.delete(id)
        listOf(entry?.nearPhotoPath, entry?.farPhotoPath)
            .mapNotNull { it?.let(::File) }
            .forEach { if (it.exists()) it.delete() }
    }

    suspend fun sessionCityOnce(): String? = session.cityFlow.firstOrNull()

    fun setSessionCity(city: String, district: String) = viewModelScope.launch {
        session.setCity(city, district)
    }

    fun exportCsv(onDone: (File) -> Unit, onError: (Throwable) -> Unit) =
        viewModelScope.launch {
            try {
                val file = exportEntriesToCsv(getApplication(), db)
                onDone(file)
            } catch (t: Throwable) {
                onError(t)
            }
        }

    suspend fun addCustomMaterialAndTie(name: String, categoryId: Long?): Long {
        val id = repo.addCustomMaterial(name)
        if (categoryId != null) repo.tieCategoryMaterial(categoryId, id)
        return id
    }

    fun movePhotosToCategoryFolder(
        context: Context,
        entryId: String,
        categoryName: String,
        nearPath: String?,
        farPath: String?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val safeName = categoryName.ifBlank { "غير مصنف" }
        val folder = File(context.getExternalFilesDir(null), "photos/$safeName").apply { mkdirs() }

        fun move(srcPath: String?, tag: String): String? {
            val src = srcPath?.let(::File) ?: return null
            if (!src.exists()) return null
            val dest = File(folder, "${entryId}_${tag}.jpg")
            return try {
                src.copyTo(dest, overwrite = true)
                src.delete()
                dest.absolutePath
            } catch (_: Throwable) { null }
        }

        val newNear = move(nearPath, "near")
        val newFar  = move(farPath, "far")

        val entry = repo.get(entryId) ?: return@launch
        repo.update(entry.copy(
            nearPhotoPath = newNear ?: entry.nearPhotoPath,
            farPhotoPath = newFar ?: entry.farPhotoPath,
            updatedAt = System.currentTimeMillis()
        ))
    }
}