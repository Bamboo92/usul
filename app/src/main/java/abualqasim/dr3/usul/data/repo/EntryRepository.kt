package abualqasim.dr3.usul.data.repo

import abualqasim.dr3.usul.data.db.*

class EntryRepository(private val db: AppDb) {
    val entries = db.entryDao().flowAll()

    suspend fun get(id: String) = db.entryDao().byId(id)

    suspend fun createDraft(
        title: String?,
        categoryId: Long?,
        materialId: Long?,
        surfaceId: Long?,
        description: String?,
        city: String?,
        district: String?,
        creatorHash: String
    ) {
        val now = System.currentTimeMillis()
        val e = Entry(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            categoryId = categoryId,
            materialId = materialId,
            surfaceId = surfaceId,
            description = description,
            city = city,
            district = district,
            creatorHash = creatorHash,
            createdAt = now,
            updatedAt = now
        )
        db.entryDao().insert(e)
    }

    suspend fun update(e: Entry) = db.entryDao().update(e)
    suspend fun delete(id: String) = db.entryDao().deleteById(id)

    suspend fun duplicateToDraft(id: String): Entry? {
        val src = db.entryDao().byId(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = src.copy(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now
        )
        db.entryDao().insert(copy)
        return copy
    }

    suspend fun categories() = db.categoryDao().all()
    suspend fun surfaces() = db.surfaceDao().all()
    suspend fun materialsForCategory(categoryId: Long?) =
        if (categoryId == null) emptyList()
        else db.categoryMaterialDao().materialsForCategory(categoryId)

    suspend fun addCustomCategory(name: String): Long =
        db.categoryDao().insert(Category(nameEn = name, userDefined = true))

    suspend fun addCustomMaterial(name: String): Long =
        db.materialDao().insert(Material(nameEn = name, userDefined = true))

    suspend fun addCustomSurface(name: String): Long =
        db.surfaceDao().insert(Surface(nameEn = name, userDefined = true))

    suspend fun tieCategoryMaterial(categoryId: Long, materialId: Long) {
        db.categoryMaterialDao().tie(CategoryMaterialCrossRef(categoryId, materialId))
    }

    suspend fun getAllBlocking() = db.entryDao().getAllBlocking()
    fun db() = db
}