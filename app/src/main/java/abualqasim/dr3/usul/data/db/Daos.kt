package abualqasim.dr3.usul.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry ORDER BY createdAt DESC")
    fun flowAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entry ORDER BY createdAt DESC")
    suspend fun getAllBlocking(): List<Entry>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun byId(id: String): Entry?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(e: Entry)

    @Update
    suspend fun update(e: Entry)

    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY userDefined, nameEn")
    suspend fun all(): List<Category>

    @Insert
    suspend fun insert(c: Category): Long
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material ORDER BY userDefined, nameEn")
    suspend fun all(): List<Material>

    @Insert
    suspend fun insert(m: Material): Long
}

@Dao
interface SurfaceDao {
    @Query("SELECT * FROM surface ORDER BY userDefined, nameEn")
    suspend fun all(): List<Surface>

    @Insert
    suspend fun insert(s: Surface): Long
}

@Dao
interface CategoryMaterialDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tie(ref: CategoryMaterialCrossRef)

    @Query("""
      SELECT material.* FROM material
      INNER JOIN category_material cm ON cm.materialId = material.id
      WHERE cm.categoryId = :categoryId
      ORDER BY material.userDefined, material.nameEn
    """)
    suspend fun materialsForCategory(categoryId: Long): List<Material>
}
