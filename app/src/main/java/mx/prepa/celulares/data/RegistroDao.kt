package mx.prepa.celulares.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {

    @Insert
    suspend fun insertar(registro: Registro): Long

    @Query("UPDATE registros SET horaEntregaAlumno = :hora WHERE id = :id")
    suspend fun marcarDevuelto(id: Long, hora: Long)

    @Query("DELETE FROM registros WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("SELECT * FROM registros WHERE id = :id")
    suspend fun porId(id: Long): Registro?

    @Query("SELECT * FROM registros ORDER BY MAX(horaEntregaAdmin, IFNULL(horaEntregaAlumno, 0)) DESC")
    fun todos(): Flow<List<Registro>>

    /** Celular del alumno que sigue en resguardo (el más reciente). */
    @Query(
        """SELECT * FROM registros
           WHERE horaEntregaAlumno IS NULL AND nombre = :nombre COLLATE NOCASE
           ORDER BY horaEntregaAdmin DESC LIMIT 1"""
    )
    suspend fun activoPorNombre(nombre: String): Registro?

    @Query(
        """SELECT * FROM registros
           WHERE horaEntregaAdmin BETWEEN :desde AND :hasta
           ORDER BY horaEntregaAdmin ASC"""
    )
    suspend fun entre(desde: Long, hasta: Long): List<Registro>
}
