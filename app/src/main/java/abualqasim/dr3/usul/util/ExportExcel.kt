package abualqasim.dr3.usul.util

import android.content.Context
import abualqasim.dr3.usul.data.db.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun exportEntriesToExcel(
    context: Context,
    db: AppDb
): File = withContext(Dispatchers.IO) {
    val folder = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(folder, "export_$timestamp.xlsx")

    val categories = db.categoryDao().all().associateBy { it.id }
    val materials = db.materialDao().all().associateBy { it.id }
    val surfaces = db.surfaceDao().all().associateBy { it.id }
    val entries = db.entryDao().getAllBlocking()

    XSSFWorkbook().use { workbook ->
        val headerStyle = workbook.createHeaderStyle()
        val bodyStyle = workbook.createBodyStyle()
        val hyperlinkStyle = workbook.createHyperlinkStyle()
        val dateStyle = workbook.createDateStyle()
        val sheet = workbook.createSheet("Entries")
        val headers = listOf(
            "ID",
            "Title",
            "Category",
            "Material",
            "Surface",
            "Description",
            "City",
            "District",
            "Creator Hash",
            "Near Photo Path",
            "Near Photo Link",
            "Far Photo Path",
            "Far Photo Link",
            "Created At",
            "Updated At"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        val creationHelper = workbook.creationHelper

        entries.forEachIndexed { rowIndex, entry ->
            val row = sheet.createRow(rowIndex + 1)
            val category = entry.categoryId?.let { categories[it]?.nameEn }.orEmpty()
            val material = entry.materialId?.let { materials[it]?.nameEn }.orEmpty()
            val surface = entry.surfaceId?.let { surfaces[it]?.nameEn }.orEmpty()

            val values = listOf(
                entry.id,
                entry.title.orEmpty(),
                category,
                material,
                surface,
                entry.description.orEmpty(),
                entry.city.orEmpty(),
                entry.district.orEmpty(),
                entry.creatorHash,
                entry.nearPhotoPath.orEmpty(),
                entry.farPhotoPath.orEmpty()
            )

            values.take(9).forEachIndexed { index, value ->
                val cell = row.createCell(index)
                cell.setCellValue(value)
                cell.cellStyle = bodyStyle
            }

            // Near path column
            row.createCell(9).apply {
                setCellValue(values[9])
                cellStyle = bodyStyle
            }

            // Near hyperlink column
            row.createCell(10).apply {
                if (values[9].isNotBlank()) {
                    val link = creationHelper.createHyperlink(HyperlinkType.FILE)
                    link.address = "file:///${values[9]}"
                    setHyperlink(link)
                    setCellValue("open")
                    cellStyle = hyperlinkStyle
                } else {
                    setCellValue("")
                    cellStyle = bodyStyle
                }
            }

            // Far path column
            row.createCell(11).apply {
                setCellValue(values[10])
                cellStyle = bodyStyle
            }

            // Far hyperlink column
            row.createCell(12).apply {
                if (values[10].isNotBlank()) {
                    val link = creationHelper.createHyperlink(HyperlinkType.FILE)
                    link.address = "file:///${values[10]}"
                    setHyperlink(link)
                    setCellValue("open")
                    cellStyle = hyperlinkStyle
                } else {
                    setCellValue("")
                    cellStyle = bodyStyle
                }
            }

            row.createCell(13).apply {
                setCellValue(Date(entry.createdAt))
                cellStyle = dateStyle
            }

            row.createCell(14).apply {
                setCellValue(Date(entry.updatedAt))
                cellStyle = dateStyle
            }
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        FileOutputStream(file).use { output -> workbook.write(output) }
    }

    file
}

private fun XSSFWorkbook.createHeaderStyle(): XSSFCellStyle = createCellStyle().apply {
    setFont(createFont().apply {
        bold = true
        fontHeightInPoints = 11
    })
    alignment = HorizontalAlignment.CENTER
    fillPattern = FillPatternType.SOLID_FOREGROUND
    fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
    setThinBorders()
}

private fun XSSFWorkbook.createBodyStyle(): XSSFCellStyle = createCellStyle().apply {
    setFont(createFont().apply { fontHeightInPoints = 11 })
    wrapText = true
    setThinBorders()
}

private fun XSSFWorkbook.createHyperlinkStyle(): XSSFCellStyle = createCellStyle().apply {
    setFont(createFont().apply {
        fontHeightInPoints = 11
        underline = XSSFFont.U_SINGLE
        color = IndexedColors.BLUE.index
    })
    wrapText = true
    setThinBorders()
}

private fun XSSFWorkbook.createDateStyle(): XSSFCellStyle = createCellStyle().apply {
    setFont(createFont().apply { fontHeightInPoints = 11 })
    dataFormat = creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss")
    setThinBorders()
}

private fun XSSFCellStyle.setThinBorders() {
    borderTop = BorderStyle.THIN
    borderBottom = BorderStyle.THIN
    borderLeft = BorderStyle.THIN
    borderRight = BorderStyle.THIN
}
