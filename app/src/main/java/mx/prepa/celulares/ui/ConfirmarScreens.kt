package mx.prepa.celulares.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mx.prepa.celulares.data.Registro
import mx.prepa.celulares.util.Fechas

@Composable
fun ConfirmarEntregaScreen(vm: AppViewModel, onAtras: () -> Unit, onListo: () -> Unit) {
    val captura by vm.captura.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var guardando by remember { mutableStateOf(false) }
    val c = captura
    if (c == null) {
        LaunchedEffect(Unit) { if (!guardando) onAtras() }
        return
    }
    val d = c.datos

    Column(Modifier.fillMaxSize().background(C.Fondo)) {
        BarraSuperior("Confirmar entrega", onAtras = { vm.cancelarCaptura(); onAtras() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val previo = c.yaEnResguardo
            if (previo != null) {
                Aviso(
                    "Este alumno ya tiene un celular en resguardo desde las ${Fechas.horaCorta(previo.horaEntregaAdmin)}",
                    C.ResguardoSuave, C.ResguardoOscuro, Icons.Outlined.WarningAmber
                )
            } else {
                Aviso("Código QR leído correctamente", C.AcentoSuave, C.AcentoOscuro, Icons.Outlined.Check)
            }

            Tarjeta(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CabeceraAlumno(d.nombre, d.semestre, d.especialidad)
                    Column {
                        Campo("Nombre del alumno", d.nombre)
                        Campo("Semestre", "${d.semestre}°")
                        Campo("Especialidad", d.especialidad)
                        Campo("Fecha", Fechas.fecha(c.horaEntregaAdmin))
                        Campo("Hora de entrega al alumno", "Pendiente")
                    }
                }
            }

            Tarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Hora de entrega al administrador", style = Tx.Pequeno)
                        Text("Registrada automáticamente", style = Tx.Mini)
                    }
                    Text(Fechas.hora(c.horaEntregaAdmin), fontFamily = Mono, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = C.Tinta)
                }
            }
        }
        Column(
            Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonPrincipal("Registrar entrega", enabled = !guardando, onClick = {
                guardando = true
                vm.confirmarEntrega {
                    Toast.makeText(context, "Entrega registrada: ${d.nombre}", Toast.LENGTH_SHORT).show()
                    onListo()
                }
            })
            BotonTexto("Cancelar", onClick = { vm.cancelarCaptura(); onAtras() })
        }
    }
}

@Composable
fun ConfirmarDevolucionScreen(vm: AppViewModel, id: Long, onAtras: () -> Unit, onListo: () -> Unit) {
    val context = LocalContext.current
    var registro by remember { mutableStateOf<Registro?>(null) }
    var cargado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(id) {
        registro = vm.registro(id)
        cargado = true
    }
    LaunchedEffect(Unit) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(1000)
        }
    }

    val r = registro
    if (r == null) {
        Column(Modifier.fillMaxSize().background(C.Fondo)) {
            BarraSuperior("Confirmar devolución", onAtras = onAtras)
            if (cargado) {
                Text(
                    "No se encontró el registro.", style = Tx.Pequeno, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.Fondo)) {
        BarraSuperior("Confirmar devolución", onAtras = onAtras)
        val yaDevuelto = r.horaEntregaAlumno
        val horaAlumno = yaDevuelto ?: ahora

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (yaDevuelto != null) {
                Aviso("Este celular ya fue devuelto", C.AcentoSuave, C.AcentoOscuro, Icons.Outlined.Info)
            } else {
                Aviso("Celular en resguardo encontrado", C.ResguardoSuave, C.ResguardoOscuro, Icons.Outlined.Check)
            }

            Tarjeta(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CabeceraAlumno(r.nombre, r.semestre, r.especialidad)
                    HorizontalDivider(color = C.Divisor)
                    Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Entrega al administrador", style = Tx.Mini)
                            Text(Fechas.hora(r.horaEntregaAdmin), fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = C.Tinta)
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(C.Divisor))
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Entrega al alumno", style = Tx.Mini)
                            Text(Fechas.hora(horaAlumno), fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = C.Acento)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Fondo).padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("Tiempo en resguardo", style = Tx.Pequeno.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                        Text(Fechas.duracion(r.horaEntregaAdmin, horaAlumno), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta)
                    }
                }
            }

            if (yaDevuelto == null) {
                Text(
                    "Verifica que el celular corresponda al alumno antes de confirmar. La hora de entrega al alumno se guarda al presionar Confirmar.",
                    style = Tx.Pequeno, lineHeight = 19.sp
                )
            }
        }
        Column(
            Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (yaDevuelto == null) {
                BotonPrincipal("Confirmar devolución", enabled = !guardando, onClick = {
                    guardando = true
                    vm.confirmarDevolucion(r.id) {
                        Toast.makeText(context, "Devolución registrada: ${r.nombre}", Toast.LENGTH_SHORT).show()
                        onListo()
                    }
                })
                BotonTexto("Cancelar", onClick = onAtras)
            } else {
                BotonPrincipal("Volver", onClick = onAtras)
            }
        }
    }
}

@Composable
fun CabeceraAlumno(nombre: String, semestre: Int, especialidad: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Avatar(nombre)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(nombre, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta, lineHeight = 22.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("$semestre° semestre")
                Etiqueta(especialidad)
            }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String) {
    HorizontalDivider(color = C.Divisor)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = Tx.Pequeno.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
        Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.Tinta, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}
