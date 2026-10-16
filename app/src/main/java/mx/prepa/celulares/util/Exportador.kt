package mx.prepa.celulares.util

import mx.prepa.celulares.data.Registro
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Columna(val titulo: String, val ancho: Double) {
    FECHA("Fecha", 12.0),
    HORA_ADMIN("Hora de entrega al administrador", 30.0),
    NOMBRE("Nombre del alumno", 36.0),
    SEMESTRE("Semestre", 11.0),
    ESPECIALIDAD("Especialidad", 14.0),
    HORA_ALUMNO("Hora de entrega al alumno", 26.0)
}

enum class Periodo { HOY, SEMANA, RANGO }

data class OpcionesExportacion(
    val periodo: Periodo = Periodo.HOY,
    val desde: Long = Fechas.inicioDelDia(System.currentTimeMillis()),
    val hasta: Long = Fechas.finDelDia(System.currentTimeMillis()),
    val especialidad: String? = null, // null = todas
    val columnas: Set<Columna> = Columna.entries.toSet()
)

object Fechas {
    private val locale = Locale("es", "MX")
    fun hora(ms: Long): String = SimpleDateFormat("HH:mm:ss", locale).format(Date(ms))
    fun horaCorta(ms: Long): String = SimpleDateFormat("HH:mm", locale).format(Date(ms))
    fun fecha(ms: Long): String = SimpleDateFormat("dd/MM/yyyy", locale).format(Date(ms))
    fun fechaArchivo(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", locale).format(Date(ms))
    fun fechaLarga(ms: Long): String =
        SimpleDateFormat("EEEE d 'de' MMMM", locale).format(Date(ms)).replaceFirstChar { it.uppercase() }
    fun encabezadoDia(ms: Long): String {
        val hoy = inicioDelDia(System.currentTimeMillis())
        val dia = inicioDelDia(ms)
        val txt = SimpleDateFormat("d MMM", locale).format(Date(ms)).replace(".", "")
        return when (dia) {
            hoy -> "Hoy · $txt"
            hoy - 86_400_000L -> "Ayer · $txt"
            else -> SimpleDateFormat("EEEE", locale).format(Date(ms)).replaceFirstChar { it.uppercase() } + " · $txt"
        }
    }

    fun mismoDia(a: Long, b: Long) = inicioDelDia(a) == inicioDelDia(b)

    fun inicioDelDia(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun finDelDia(ms: Long): Long = inicioDelDia(ms) + 86_400_000L - 1

    /** Lunes de la semana actual. */
    fun inicioDeSemana(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDelDia(ms)
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }.timeInMillis

    fun duracion(desde: Long, hasta: Long): String {
        val min = ((hasta - desde) / 60_000L).coerceAtLeast(0)
        val h = min / 60
        val m = min % 60
        return if (h > 0) "$h h $m min" else "$m min"
    }
}

object Exportador {

    fun rango(op: OpcionesExportacion, ahora: Long = System.currentTimeMillis()): Pair<Long, Long> = when (op.periodo) {
        Periodo.HOY -> Fechas.inicioDelDia(ahora) to Fechas.finDelDia(ahora)
        Periodo.SEMANA -> Fechas.inicioDeSemana(ahora) to Fechas.finDelDia(ahora)
        Periodo.RANGO -> Fechas.inicioDelDia(op.desde) to Fechas.finDelDia(op.hasta)
    }

    fun filtrar(registros: List<Registro>, op: OpcionesExportacion): List<Registro> =
        registros.filter { op.especialidad == null || it.especialidad == op.especialidad }

    fun nombreArchivo(op: OpcionesExportacion): String {
        val (d, h) = rango(op)
        val base = if (Fechas.mismoDia(d, h)) Fechas.fechaArchivo(d)
        else "${Fechas.fechaArchivo(d)}_a_${Fechas.fechaArchivo(h)}"
        val esp = op.especialidad?.let { "_$it" } ?: ""
        return "registro_celulares_$base$esp.xlsx"
    }

    fun escribir(salida: OutputStream, registros: List<Registro>, op: OpcionesExportacion) {
        val columnas = Columna.entries.filter { it in op.columnas }
        val filas = registros.map { r ->
            columnas.map { c ->
                when (c) {
                    Columna.FECHA -> XlsxWriter.Celda.Texto(Fechas.fecha(r.horaEntregaAdmin))
                    Columna.HORA_ADMIN -> XlsxWriter.Celda.Texto(Fechas.hora(r.horaEntregaAdmin))
                    Columna.NOMBRE -> XlsxWriter.Celda.Texto(r.nombre)
                    Columna.SEMESTRE -> XlsxWriter.Celda.Numero(r.semestre)
                    Columna.ESPECIALIDAD -> XlsxWriter.Celda.Texto(r.especialidad)
                    Columna.HORA_ALUMNO -> {
                        val h = r.horaEntregaAlumno
                        when {
                            h == null -> XlsxWriter.Celda.Texto("En resguardo")
                            Fechas.mismoDia(h, r.horaEntregaAdmin) -> XlsxWriter.Celda.Texto(Fechas.hora(h))
                            else -> XlsxWriter.Celda.Texto("${Fechas.fecha(h)} ${Fechas.hora(h)}")
                        }
                    }
                }
            }
        }
        XlsxWriter.escribir(
            salida = salida,
            nombreHoja = "Registros",
            encabezados = columnas.map { it.titulo },
            filas = filas,
            anchos = columnas.map { it.ancho }
        )
    }
}
