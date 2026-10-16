package mx.prepa.celulares.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un registro = un celular entregado por un alumno.
 * Las horas se guardan como milisegundos (epoch) para poder filtrar por fecha.
 */
@Entity(tableName = "registros", indices = [Index("horaEntregaAdmin"), Index("nombre")])
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val semestre: Int,
    val especialidad: String,          // "COMP" o "SEB"
    val horaEntregaAdmin: Long,        // hora en que el alumno entregó el celular al administrador
    val horaEntregaAlumno: Long? = null, // hora en que se le devolvió al alumno (null = en resguardo)
    val qrTexto: String = ""           // contenido original del QR, por si hace falta revisarlo
) {
    val enResguardo: Boolean get() = horaEntregaAlumno == null
}
