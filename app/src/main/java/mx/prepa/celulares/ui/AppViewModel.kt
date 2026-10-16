package mx.prepa.celulares.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.prepa.celulares.data.AppDatabase
import mx.prepa.celulares.data.Registro
import mx.prepa.celulares.util.DatosAlumno
import mx.prepa.celulares.util.Exportador
import mx.prepa.celulares.util.OpcionesExportacion
import mx.prepa.celulares.util.QrParser
import mx.prepa.celulares.util.ResultadoQr
import java.io.File

enum class Modo { RECEPCION, DEVOLUCION }

/** Datos leídos del QR, esperando confirmación de la entrega. */
data class Captura(
    val datos: DatosAlumno,
    val horaEntregaAdmin: Long,
    val qrTexto: String,
    val yaEnResguardo: Registro? // si el alumno ya tiene un celular sin devolver
)

sealed interface ResultadoEscaneo {
    data object IrAConfirmarEntrega : ResultadoEscaneo
    data class IrAConfirmarDevolucion(val id: Long) : ResultadoEscaneo
    data class Error(val mensaje: String) : ResultadoEscaneo
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).registroDao()

    val registros: StateFlow<List<Registro>> = dao.todos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _captura = MutableStateFlow<Captura?>(null)
    val captura: StateFlow<Captura?> = _captura.asStateFlow()

    private val _opcionesExportacion = MutableStateFlow(OpcionesExportacion())
    val opcionesExportacion: StateFlow<OpcionesExportacion> = _opcionesExportacion.asStateFlow()

    fun actualizarOpciones(f: (OpcionesExportacion) -> OpcionesExportacion) {
        _opcionesExportacion.value = f(_opcionesExportacion.value)
    }

    /** Procesa el texto leído por la cámara según el modo de la pantalla. */
    suspend fun procesarQr(texto: String, modo: Modo): ResultadoEscaneo {
        val datos = when (val r = QrParser.parse(texto)) {
            is ResultadoQr.Error -> return ResultadoEscaneo.Error(r.mensaje)
            is ResultadoQr.Ok -> r.datos
        }
        return when (modo) {
            Modo.RECEPCION -> {
                prepararEntrega(datos, texto)
                ResultadoEscaneo.IrAConfirmarEntrega
            }
            Modo.DEVOLUCION -> {
                val activo = dao.activoPorNombre(datos.nombre)
                    ?: return ResultadoEscaneo.Error("${datos.nombre} no tiene un celular en resguardo.")
                ResultadoEscaneo.IrAConfirmarDevolucion(activo.id)
            }
        }
    }

    /** La hora de entrega al administrador se toma en el momento de la lectura. */
    suspend fun prepararEntrega(datos: DatosAlumno, qrTexto: String) {
        val activo = dao.activoPorNombre(datos.nombre)
        _captura.value = Captura(datos, System.currentTimeMillis(), qrTexto, activo)
    }

    fun confirmarEntrega(alTerminar: () -> Unit) {
        val c = _captura.value ?: return
        viewModelScope.launch {
            dao.insertar(
                Registro(
                    nombre = c.datos.nombre,
                    semestre = c.datos.semestre,
                    especialidad = c.datos.especialidad,
                    horaEntregaAdmin = c.horaEntregaAdmin,
                    qrTexto = c.qrTexto
                )
            )
            _captura.value = null
            alTerminar()
        }
    }

    fun cancelarCaptura() {
        _captura.value = null
    }

    suspend fun registro(id: Long): Registro? = dao.porId(id)

    /** La hora de entrega al alumno se guarda al confirmar. */
    fun confirmarDevolucion(id: Long, alTerminar: () -> Unit) {
        viewModelScope.launch {
            dao.marcarDevuelto(id, System.currentTimeMillis())
            alTerminar()
        }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch { dao.eliminar(id) }
    }

    /** Registros que entran en la exportación con las opciones actuales. */
    suspend fun registrosParaExportar(op: OpcionesExportacion): List<Registro> {
        val (desde, hasta) = Exportador.rango(op)
        return Exportador.filtrar(dao.entre(desde, hasta), op)
    }

    /** Guarda el Excel en la ubicación elegida por el usuario. Devuelve el número de filas. */
    suspend fun exportarA(uri: Uri): Int = withContext(Dispatchers.IO) {
        val op = _opcionesExportacion.value
        val lista = registrosParaExportar(op)
        val ctx = getApplication<Application>()
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { Exportador.escribir(it, lista, op) }
            ?: error("No se pudo abrir el archivo de destino.")
        lista.size
    }

    /** Genera el Excel en caché para compartirlo (correo, WhatsApp, Drive...). */
    suspend fun exportarParaCompartir(): File = withContext(Dispatchers.IO) {
        val op = _opcionesExportacion.value
        val lista = registrosParaExportar(op)
        val dir = File(getApplication<Application>().cacheDir, "exportaciones").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val archivo = File(dir, Exportador.nombreArchivo(op))
        archivo.outputStream().use { Exportador.escribir(it, lista, op) }
        archivo
    }
}
