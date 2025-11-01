// file: app/src/main/java/abualqasim/dr3/usul/data/db/AppDb.kt
package abualqasim.dr3.usul.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Entry::class,
        Category::class, Material::class, Surface::class,
        CategoryMaterialCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun materialDao(): MaterialDao
    abstract fun surfaceDao(): SurfaceDao
    abstract fun categoryMaterialDao(): CategoryMaterialDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb {
            val cached = INSTANCE
            if (cached != null) return cached
            return synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "jitzer.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedCallback())
                    .build().also { INSTANCE = it }
            }
        }
    }
}

private class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Categories (nameEn can be Arabic)
        db.execSQL("""INSERT INTO category(id,nameEn,nameLat,userDefined) VALUES
            (1,'نقش','Incisura',0),
            (2,'غرافيتي','Graffiti',0),
            (3,'منحوتة','Sculptura',0),
            (4,'لوحة','Tabula',0),
            (5,'ملصق','Poster',0)
        """.trimIndent())

        // Materials
        db.execSQL("""INSERT INTO material(id,nameEn,nameLat,userDefined) VALUES
            (1,'حجر','Lapis',0),
            (2,'معدن','Metallum',0),
            (3,'خشب','Lignum',0),
            (4,'دهان رذاذ','Aerosol',0),
            (5,'حبر','Atramentum',0)
        """.trimIndent())

        // Surfaces
        db.execSQL("""INSERT INTO surface(id,nameEn,userDefined) VALUES
            (1,'حجر',0),
            (2,'معدن',0),
            (3,'خشب',0),
            (4,'خرسانة',0),
            (5,'قرميد',0)
        """.trimIndent())

        // Allowed ties
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (2,4)") // Graffiti -> Spray
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (1,1)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (1,2)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (1,3)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (1,4)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (3,1)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (3,2)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (3,3)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (4,5)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (5,5)")
        db.execSQL("INSERT INTO category_material(categoryId,materialId) VALUES (4,4)")
    }
}