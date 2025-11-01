package abualqasim.dr3.usul.data.db

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

class AppDb private constructor(context: Context) {
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = _entries

    private val categoriesList = mutableListOf<Category>()
    private val materialsList = mutableListOf<Material>()
    private val surfacesList = mutableListOf<Surface>()

    suspend fun insert(e: Entry) {
        _entries.value = _entries.value + e
    }

    suspend fun delete(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
    }

    suspend fun get(id: String): Entry? = _entries.value.find { it.id == id }
    suspend fun update(entry: Entry) {
        _entries.value = _entries.value.map { if (it.id == entry.id) entry else it }
    }

    suspend fun categories(): List<Category> = categoriesList
    suspend fun materialsForCategory(id: Long?): List<Material> = materialsList
    suspend fun surfaces(): List<Surface> = surfacesList

    suspend fun addCustomCategory(name: String): Long {
        val id = (categoriesList.maxOfOrNull { it.id } ?: 0L) + 1
        categoriesList += Category(id, name)
        return id
    }

    suspend fun addCustomMaterial(name: String): Long {
        val id = (materialsList.maxOfOrNull { it.id } ?: 0L) + 1
        materialsList += Material(id, name)
        return id
    }

    suspend fun addCustomSurface(name: String): Long {
        val id = (surfacesList.maxOfOrNull { it.id } ?: 0L) + 1
        surfacesList += Surface(id, name)
        return id
    }

    suspend fun tieCategoryMaterial(categoryId: Long, materialId: Long) {}

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: AppDb(context).also { instance = it }
        }
    }
}
