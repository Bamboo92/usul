package abualqasim.dr3.usul.util

import android.content.Context
import abualqasim.dr3.usul.data.db.AppDb
import java.io.File

suspend fun exportEntriesToCsv(
    context: Context,
    db: AppDb
): File {
    val folder = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
    val file = File(folder, "export_${System.currentTimeMillis()}.csv")

    val cats = db.categoryDao().all().associateBy { it.id }
    val mats = db.materialDao().all().associateBy { it.id }
    val surs = db.surfaceDao().all().associateBy { it.id }

    val entries = db.entryDao().getAllBlocking()

    file.bufferedWriter().use { out ->
        out.appendLine(
            listOf(
                "id","title","category","material","surface",
                "description","city","district","creatorHash",
                "nearPhotoPath","nearPhotoLink",
                "farPhotoPath","farPhotoLink",
                "createdAt","updatedAt"
            ).joinToString(",")
        )
        for (e in entries) {
            val category = e.categoryId?.let { cats[it]?.nameEn }.orEmpty()
            val material = e.materialId?.let { mats[it]?.nameEn }.orEmpty()
            val surface  = e.surfaceId?.let { surs[it]?.nameEn }.orEmpty()

            val nearPath = e.nearPhotoPath.orEmpty()
            val farPath  = e.farPhotoPath.orEmpty()

            val nearLink = hyperlinkFor(nearPath, "near")
            val farLink  = hyperlinkFor(farPath, "far")

            out.appendLine(listOf(
                e.id,
                e.title.orEmpty(),
                category,
                material,
                surface,
                e.description.orEmpty(),
                e.city.orEmpty(),
                e.district.orEmpty(),
                e.creatorHash,
                nearPath,
                nearLink,
                farPath,
                farLink,
                e.createdAt.toString(),
                e.updatedAt.toString()
            ).joinToString(",") { csvEscape(it) })
        }
    }
    return file
}

private fun hyperlinkFor(path: String, label: String): String =
    if (path.isBlank()) ""
    else """=HYPERLINK("file:///$path","$label")"""

private fun csvEscape(s: String): String =
    if (s.any { it == ',' || it == '"' || it == '\n' }) "\"${s.replace("\"","\"\"")}\"" else s
