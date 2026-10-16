package mx.prepa.celulares.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.prepa.celulares.data.Registro
import mx.prepa.celulares.util.Fechas
import java.text.Normalizer

enum class Filtro(val titulo: String) {
    TODOS("Todos"), RESGUARDO("En resguardo"), DEVUELTOS("Devueltos"), COMP("COMP"), SEB("SEB")
}

private fun sinAcentos(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegistrosScreen(
    vm: AppViewModel,
    filtroInicial: Filtro,
    onAbrir: (Registro) -> Unit,
    onExportar: () -> Unit,
    onPestana: (Pestana) -> Unit
) {
    val registros by vm.registros.collectAsStateWithLifecycle()
    var filtro by rememberSaveable { mutableStateOf(filtroInicial) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var porEliminar by remember { mutableStateOf<Registro?>(null) }

    val q = sinAcentos(busqueda.trim())
    val visibles = registros.filter { r ->
        val pasaFiltro = when (filtro) {
            Filtro.TODOS -> true
            Filtro.RESGUARDO -> r.enResguardo
            Filtro.DEVUELTOS -> !r.enResguardo
            Filtro.COMP -> r.especialidad == "COMP"
            Filtro.SEB -> r.especialidad == "SEB"
        }
        pasaFiltro && (q.isEmpty() || sinAcentos(r.nombre).contains(q))
    }.sortedByDescending { it.horaEntregaAdmin }
    val porDia = visibles.groupBy { Fechas.inicioDelDia(it.horaEntregaAdmin) }

    Column(Modifier.fillMaxSize().background(C.Fondo)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registros", style = Tx.Titulo, modifier = Modifier.weight(1f))
            IconButton(
                onClick = onExportar,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Superficie)
                    .border(1.dp, C.Borde, RoundedCornerShape(12.dp))
            ) { Icon(Icons.Outlined.FileDownload, contentDescription = "Exportar a Excel", tint = C.Tinta) }
        }

        TextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por nombre", color = Color(0xFF6B717D)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = C.Secundario) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = C.Superficie,
                unfocusedContainerColor = C.Superficie,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = C.Acento
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .border(1.dp, C.Borde, RoundedCornerShape(12.dp))
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Filtro.entries.forEach { f -> Chip(f.titulo, f == filtro) { filtro = f } }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (visibles.isEmpty()) {
                item {
                    Text(
                        if (registros.isEmpty()) "Todavía no hay registros." else "Ningún registro coincide con la búsqueda.",
                        style = Tx.Pequeno, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                    )
                }
            }
            porDia.forEach { (dia, lista) ->
                item(key = "dia-$dia") {
                    Text(
                        Fechas.encabezadoDia(dia).uppercase(),
                        style = Tx.Seccion,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(lista, key = { it.id }) { r ->
                    TarjetaRegistro(
                        r,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onAbrir(r) },
                                onLongClick = { porEliminar = r }
                            )
                    )
                }
            }
            if (visibles.isNotEmpty()) {
                item {
                    Text(
                        "Toca un celular en resguardo para devolverlo. Mantén presionado para eliminar un registro.",
                        style = Tx.Mini, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
            }
        }
        BarraInferior(Pestana.REGISTROS, onPestana)
    }

    porEliminar?.let { r ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar registro") },
            text = { Text("¿Eliminar el registro de ${r.nombre} (${Fechas.fecha(r.horaEntregaAdmin)} ${Fechas.horaCorta(r.horaEntregaAdmin)})? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.eliminar(r.id); porEliminar = null }) { Text("Eliminar", color = C.Error) }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text("Cancelar", color = C.Tinta2) }
            },
            containerColor = C.Superficie
        )
    }
}

@Composable
private fun TarjetaRegistro(r: Registro, modifier: Modifier) {
    Tarjeta(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), radio = 14) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(r.nombre, style = Tx.CuerpoM, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${r.semestre}° semestre · ${r.especialidad}", style = Tx.Pequeno)
                }
                Estado(r.enResguardo)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Admin", style = Tx.Pequeno)
                Text(Fechas.horaCorta(r.horaEntregaAdmin), fontFamily = Mono, fontSize = 13.sp, color = C.Tinta)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = C.Secundario, modifier = Modifier.size(14.dp))
                Text("Alumno", style = Tx.Pequeno)
                Text(r.horaEntregaAlumno?.let { Fechas.horaCorta(it) } ?: "—", fontFamily = Mono, fontSize = 13.sp, color = C.Tinta, fontWeight = FontWeight.Normal)
            }
        }
    }
}
