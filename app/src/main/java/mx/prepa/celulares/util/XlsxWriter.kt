package mx.prepa.celulares.util

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generador mínimo de archivos Excel (.xlsx) sin dependencias externas.
 * Una hoja, fila de encabezado en negritas, encabezado fijo y filtros automáticos.
 */
object XlsxWriter {

    sealed interface Celda {
        data class Texto(val valor: String) : Celda
        data class Numero(val valor: Number) : Celda
        data object Vacia : Celda
    }

    fun escribir(
        salida: OutputStream,
        nombreHoja: String,
        encabezados: List<String>,
        filas: List<List<Celda>>,
        anchos: List<Double>
    ) {
        ZipOutputStream(salida).use { zip ->
            fun agregar(ruta: String, contenido: String) {
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            agregar("[Content_Types].xml", CONTENT_TYPES)
            agregar("_rels/.rels", RELS)
            agregar("xl/workbook.xml", workbook(nombreHoja))
            agregar("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            agregar("xl/styles.xml", STYLES)
            agregar("xl/worksheets/sheet1.xml", hoja(encabezados, filas, anchos))
        }
    }

    private fun hoja(encabezados: List<String>, filas: List<List<Celda>>, anchos: List<Double>): String {
        val sb = StringBuilder()
        val ultimaCol = columna(encabezados.size - 1)
        val ultimaFila = filas.size + 1
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<dimension ref="A1:$ultimaCol$ultimaFila"/>""")
        sb.append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        sb.append("<cols>")
        anchos.forEachIndexed { i, w ->
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("</cols><sheetData>")

        sb.append("""<row r="1">""")
        encabezados.forEachIndexed { i, h ->
            sb.append("""<c r="${columna(i)}1" t="inlineStr" s="1"><is><t>${escapar(h)}</t></is></c>""")
        }
        sb.append("</row>")

        filas.forEachIndexed { f, fila ->
            val r = f + 2
            sb.append("""<row r="$r">""")
            fila.forEachIndexed { i, celda ->
                val ref = "${columna(i)}$r"
                when (celda) {
                    is Celda.Texto -> sb.append("""<c r="$ref" t="inlineStr" s="2"><is><t xml:space="preserve">${escapar(celda.valor)}</t></is></c>""")
                    is Celda.Numero -> sb.append("""<c r="$ref" s="3"><v>${celda.valor}</v></c>""")
                    Celda.Vacia -> Unit
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        sb.append("""<autoFilter ref="A1:$ultimaCol$ultimaFila"/>""")
        sb.append("</worksheet>")
        return sb.toString()
    }

    /** 0 -> A, 25 -> Z, 26 -> AA */
    private fun columna(indice: Int): String {
        var n = indice + 1
        val sb = StringBuilder()
        while (n > 0) {
            val m = (n - 1) % 26
            sb.insert(0, ('A' + m))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun escapar(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c < ' ' && c != '\t' && c != '\n' && c != '\r' -> Unit // caracteres no válidos en XML
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun workbook(nombreHoja: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            """<sheets><sheet name="${escapar(nombreHoja.take(31))}" sheetId="1" r:id="rId1"/></sheets>""" +
            """<definedNames><definedName name="_xlnm._FilterDatabase" localSheetId="0" hidden="1">'${escapar(nombreHoja.take(31))}'!${'$'}A${'$'}1</definedName></definedNames>""" +
            """</workbook>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    // Estilos: 0 = normal, 1 = encabezado (negritas, fondo azul, texto blanco), 2 = texto con borde, 3 = número centrado con borde
    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="2">""" +
            """<font><sz val="11"/><name val="Calibri"/><family val="2"/></font>""" +
            """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>""" +
            """</fonts>""" +
            """<fills count="3">""" +
            """<fill><patternFill patternType="none"/></fill>""" +
            """<fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FF1B4DB1"/><bgColor indexed="64"/></patternFill></fill>""" +
            """</fills>""" +
            """<borders count="2">""" +
            """<border><left/><right/><top/><bottom/><diagonal/></border>""" +
            """<border><left style="thin"><color rgb="FFD5D6D2"/></left><right style="thin"><color rgb="FFD5D6D2"/></right><top style="thin"><color rgb="FFD5D6D2"/></top><bottom style="thin"><color rgb="FFD5D6D2"/></bottom><diagonal/></border>""" +
            """</borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="4">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>""" +
            """</cellXfs>""" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            """</styleSheet>"""
}
