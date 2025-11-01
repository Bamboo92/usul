package abualqasim.dr3.usul.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey val id: String,
    val title: String?,
    val categoryId: Long?,
    val materialId: Long?,
    val surfaceId: Long?,
    val description: String?,
    val city: String?,
    val district: String?,
    val creatorHash: String,
    val nearPhotoPath: String? = null,
    val farPhotoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameLat: String? = null,
    val userDefined: Boolean = false
)

@Entity(tableName = "material")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameLat: String? = null,
    val userDefined: Boolean = false
)

@Entity(tableName = "surface")
data class Surface(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val userDefined: Boolean = false
)

@Entity(
    tableName = "category_material",
    primaryKeys = ["categoryId", "materialId"]
)
data class CategoryMaterialCrossRef(
    val categoryId: Long,
    val materialId: Long
)