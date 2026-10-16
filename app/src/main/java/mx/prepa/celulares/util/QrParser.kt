package mx.prepa.celulares.util

import org.json.JSONObject
import java.text.Normalizer

data class DatosAlumno(
    val nombre: String,
    val semestre: Int,
    val especialidad: String
)

sealed interface ResultadoQr {
    data class Ok(val datos: DatosAlumno) : ResultadoQr
    data class Error(val mensaje: String) : ResultadoQr
}

/**
 * Interpreta el contenido del QR del alumno. Acepta cualquiera de estos formatos:
 *
 *  1. JSON:           {"nombre":"Ana Sofía Ramírez","semestre":5,"especialidad":"COMP"}
 *  2. Clave: valor:   Nombre: Ana Sofía Ramírez / Semestre: 5 / Especialidad: COMP  (una por línea)
 *  3. Separado por |, ; , o saltos de línea:   Ana Sofía Ramírez|5|COMP   (en cualquier orden)
 */
object QrParser {

    private val clavesNombre = setOf("nombre", "name", "alumno", "nombrealumno", "estudiante")
    private val clavesSemestre = setOf("semestre", "sem", "semester", "grado")
    private val clavesEspecialidad = setOf("especialidad", "esp", "carrera", "area", "especializacion")

    fun parse(texto: String): ResultadoQr {
        val t = texto.trim()
        if (t.isEmpty()) return ResultadoQr.Error("El código QR está vacío.")

        val campos: Triple<String?, String?, String?> = when {
            t.startsWith("{") -> desdeJson(t) ?: return ResultadoQr.Error("El QR contiene un JSON no válido.")
            t.lines().any { it.contains(':') } && !t.contains('|') -> desdeClaveValor(t)
            else -> desdeSeparado(t)
        }
        return validar(campos.first, campos.second, campos.third)
    }

    fun validar(nombreTxt: String?, semestreTxt: String?, especialidadTxt: String?): ResultadoQr {
        val nombre = nombreTxt?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        if (nombre.length < 2) return ResultadoQr.Error("No se encontró el nombre del alumno en el QR.")
        val semestre = semestreTxt?.let { parseSemestre(it) }
            ?: return ResultadoQr.Error("No se encontró un semestre válido (1 a 6) en el QR.")
        val esp = especialidadTxt?.let { parseEspecialidad(it) }
            ?: return ResultadoQr.Error("La especialidad debe ser COMP o SEB.")
        return ResultadoQr.Ok(DatosAlumno(nombre, semestre, esp))
    }

    private fun desdeJson(t: String): Triple<String?, String?, String?>? = try {
        val o = JSONObject(t)
        var n: String? = null; var s: String? = null; var e: String? = null
        o.keys().forEach { k ->
            val clave = normalizar(k).replace(" ", "").replace("_", "")
            val v = o.opt(k)?.toString()
            when (clave) {
                in clavesNombre -> n = v
                in clavesSemestre -> s = v
                in clavesEspecialidad -> e = v
            }
        }
        Triple(n, s, e)
    } catch (_: Exception) {
        null
    }

    private fun desdeClaveValor(t: String): Triple<String?, String?, String?> {
        var n: String? = null; var s: String? = null; var e: String? = null
        t.split('\n', ';').forEach { linea ->
            val i = linea.indexOf(':')
            if (i <= 0) return@forEach
            val clave = normalizar(linea.substring(0, i)).replace(" ", "").replace("_", "")
            val valor = linea.substring(i + 1).trim()
            when (clave) {
                in clavesNombre -> n = valor
                in clavesSemestre -> s = valor
                in clavesEspecialidad -> e = valor
            }
        }
        return Triple(n, s, e)
    }

    /** Campos sin etiqueta: se identifica cuál es la especialidad, cuál el semestre y el resto es el nombre. */
    private fun desdeSeparado(t: String): Triple<String?, String?, String?> {
        val separador = listOf('|', ';', '\n', ',', '\t').firstOrNull { t.contains(it) }
            ?: return Triple(null, null, null)
        val partes = t.split(separador).map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()

        val iEsp = partes.indexOfFirst { parseEspecialidad(it) != null }
        val esp = if (iEsp >= 0) partes.removeAt(iEsp) else null
        val iSem = partes.indexOfFirst { it.any(Char::isDigit) && parseSemestre(it) != null }
            .takeIf { it >= 0 }
            ?: partes.indexOfFirst { it.length <= 12 && parseSemestre(it) != null }
        val sem = if (iSem >= 0) partes.removeAt(iSem) else null
        val nombre = partes.maxByOrNull { it.length }
        return Triple(nombre, sem, esp)
    }

    private val ordinales = mapOf(
        "primer" to 1, "primero" to 1, "segundo" to 2, "tercer" to 3, "tercero" to 3,
        "cuarto" to 4, "quinto" to 5, "sexto" to 6
    )

    fun parseSemestre(texto: String): Int? {
        val n = normalizar(texto)
        Regex("\\d+").find(n)?.value?.toIntOrNull()?.let { return it.takeIf { v -> v in 1..6 } }
        return ordinales.entries.firstOrNull { n.split(" ").contains(it.key) }?.value
    }

    fun parseEspecialidad(texto: String): String? {
        val n = normalizar(texto).filter { it.isLetter() }.uppercase()
        return when {
            n == "COMP" || n.startsWith("COMPUTACION") || n.startsWith("COMPUTO") -> "COMP"
            n == "SEB" -> "SEB"
            else -> null
        }
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
}
