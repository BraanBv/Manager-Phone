package mx.prepa.celulares.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mx.prepa.celulares.util.Columna
import mx.prepa.celulares.util.Exportador
import mx.prepa.celulares.util.Fechas
import mx.prepa.celulares.util.Periodo
import java.util.Calendar
import java.util.TimeZone

private const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarScreen(vm: AppViewModel, onAtras: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val op by vm.opcionesExportacion.collectAsStateWithLifecycle()
    val registros by vm.registros.collectAsStateWithLifecycle()
    var mostrarCalendario by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }

    val cantidad by produceState(initialValue = 0, op, registros) {
        value = vm.registrosParaExportar(op).size
    }
    val (desde, hasta) = Exportador.rango(op)
    val nombreArchivo = Exportador.nombreArchivo(op)

    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_XLSX)) { uri ->
        if (uri == null) {
            trabajando = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val msg = runCatching { vm.exportarA(uri) }
                .fold({ n -> "Excel guardado con $n registro${if (n == 1) "" else "s"}" }, { "No se pudo guardar: ${it.message}" })
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            trabajando = false
        }
    }

    fun compartir() {
        trabajando = true
        scope.launch {
            runCatching { vm.exportarParaCompartir() }
                .onSuccess { archivo ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = MIME_XLSX
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, archivo.nameWithoutExtension)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir Excel"))
                }
                .onFailure { Toast.makeText(context, "No se pudo generar el archivo: ${it.message}", Toast.LENGTH_LONG).show() }
            trabajando = false
        }
    }

    Column(Modifier.fillMaxSize().background(C.Fondo)) {
        BarraSuperior("Exportar a Excel", onAtras = onAtras)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Periodo
            Seccion("PERIODO") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE9E9E6)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(Periodo.HOY to "Hoy", Periodo.SEMANA to "Semana", Periodo.RANGO to "Rango").forEach { (p, t) ->
                        val activo = op.periodo == p
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (activo) C.Superficie else Color.Transparent)
                                .clickable {
                                    vm.actualizarOpciones { it.copy(periodo = p) }
                                    if (p == Periodo.RANGO) mostrarCalendario = true
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, fontSize = 14.sp, fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium, color = if (activo) C.Tinta else C.Tinta2)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoFecha("Desde", Fechas.fecha(desde), Modifier.weight(1f)) {
                        vm.actualizarOpciones { it.copy(periodo = Periodo.RANGO) }; mostrarCalendario = true
                    }
                    CampoFecha("Hasta", Fechas.fecha(hasta), Modifier.weight(1f)) {
                        vm.actualizarOpciones { it.copy(periodo = Periodo.RANGO) }; mostrarCalendario = true
                    }
                }
            }

            // Especialidad
            Seccion("ESPECIALIDAD") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Todas", "COMP" to "COMP", "SEB" to "SEB").forEach { (e, t) ->
                        Chip(t, op.especialidad == e) { vm.actualizarOpciones { it.copy(especialidad = e) } }
                    }
                }
            }

            // Columnas
            Seccion("COLUMNAS") {
                Tarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), radio = 14) {
                    Column {
                        Columna.entries.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = C.Divisor)
                            val marcada = c in op.columnas
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        vm.actualizarOpciones {
                                            val nuevas = if (marcada) it.columnas - c else it.columnas + c
                                            it.copy(columnas = nuevas.ifEmpty { it.columnas })
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.titulo, fontSize = 14.sp, color = C.Tinta, modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = marcada,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = C.Acento)
                                )
                            }
                        }
                    }
                }
            }

            // Vista previa del archivo
            Tarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), radio = 14) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(C.ExcelSuave),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Description, contentDescription = null, tint = C.Excel, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(nombreArchivo, fontFamily = Mono, fontSize = 12.sp, color = C.Tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "$cantidad registro${if (cantidad == 1) "" else "s"} · ${op.columnas.size} columnas · 1 hoja",
                            style = Tx.Mini
                        )
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(C.Fondo).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(color = C.Borde)
            BotonPrincipal(
                "Exportar a Excel",
                icono = Icons.Outlined.FileDownload,
                enabled = !trabajando && cantidad > 0,
                onClick = { trabajando = true; guardar.launch(nombreArchivo) }
            )
            BotonSecundario("Compartir", onClick = { if (!trabajando && cantidad > 0) compartir() }, icono = Icons.Outlined.Share)
            Text(
                if (cantidad == 0) "No hay registros en el periodo seleccionado." else "Los registros se guardan solo en este dispositivo.",
                style = Tx.Mini, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (mostrarCalendario) {
        val estado = rememberDateRangePickerState(
            initialSelectedStartDateMillis = localAUtc(op.desde),
            initialSelectedEndDateMillis = localAUtc(op.hasta)
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    enabled = estado.selectedStartDateMillis != null,
                    onClick = {
                        val ini = estado.selectedStartDateMillis
                        val fin = estado.selectedEndDateMillis ?: ini
                        if (ini != null && fin != null) {
                            vm.actualizarOpciones {
                                it.copy(periodo = Periodo.RANGO, desde = utcALocal(ini), hasta = Fechas.finDelDia(utcALocal(fin)))
                            }
                        }
                        mostrarCalendario = false
                    }
                ) { Text("Aceptar", color = C.Acento) }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar", color = C.Tinta2) } }
        ) {
            DateRangePicker(state = estado, modifier = Modifier.weight(1f), title = {
                Text("Selecciona el periodo", modifier = Modifier.padding(start = 24.dp, top = 16.dp), color = C.Secundario)
            })
        }
    }
}

@Composable
private fun Seccion(titulo: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, style = Tx.Seccion)
        content()
    }
}

@Composable
private fun CampoFecha(etiqueta: String, valor: String, modifier: Modifier, onClick: () -> Unit) {
    Tarjeta(modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), radio = 12) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(etiqueta, style = Tx.Mini)
            Text(valor, fontFamily = Mono, fontSize = 15.sp, color = C.Tinta)
        }
    }
}

/** El selector de fechas trabaja en medianoche UTC; estas funciones convierten a/desde la hora local. */
private fun localAUtc(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcALocal(utc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear()
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
