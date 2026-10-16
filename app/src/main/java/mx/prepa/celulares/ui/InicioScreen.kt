package mx.prepa.celulares.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.prepa.celulares.data.Registro
import mx.prepa.celulares.util.Fechas

@Composable
fun InicioScreen(
    vm: AppViewModel,
    onRecibir: () -> Unit,
    onDevolver: () -> Unit,
    onVerTodo: () -> Unit,
    onPestana: (Pestana) -> Unit
) {
    val registros by vm.registros.collectAsStateWithLifecycle()
    val ahora = System.currentTimeMillis()
    val enResguardo = registros.count { it.enResguardo }
    val devueltosHoy = registros.count { r -> r.horaEntregaAlumno?.let { Fechas.mismoDia(it, ahora) } == true }
    val recientes = registros.take(8)

    Column(Modifier.fillMaxSize().background(C.Fondo)) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)) {
                    Text(Fechas.fechaLarga(ahora), style = Tx.Pequeno)
                    Text("Control de celulares", style = Tx.Titulo, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Contador("En resguardo", enResguardo, C.Resguardo, Modifier.weight(1f))
                    Contador("Devueltos hoy", devueltosHoy, C.Acento, Modifier.weight(1f))
                }
            }
            item {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accion(
                        titulo = "Recibir celular",
                        detalle = "Escanear QR y registrar entrega",
                        icono = Icons.Outlined.QrCodeScanner,
                        principal = true,
                        onClick = onRecibir
                    )
                    Accion(
                        titulo = "Devolver celular",
                        detalle = "Escanear QR y registrar devolución",
                        icono = Icons.AutoMirrored.Outlined.Undo,
                        principal = false,
                        onClick = onDevolver
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Actividad reciente", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta, modifier = Modifier.weight(1f))
                    TextButton(onClick = onVerTodo) { Text("Ver todo", color = C.Acento, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }
            }
            if (recientes.isEmpty()) {
                item {
                    Text(
                        "Aún no hay registros. Toca «Recibir celular» para escanear el primer QR.",
                        style = Tx.Pequeno,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
            items(recientes, key = { it.id }) { r -> FilaReciente(r) }
        }
        BarraInferior(Pestana.INICIO, onPestana)
    }
}

@Composable
private fun Contador(titulo: String, valor: Int, punto: Color, modifier: Modifier) {
    Tarjeta(modifier, padding = androidx.compose.foundation.layout.PaddingValues(16.dp), radio = 14) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(punto))
                Text(titulo, style = Tx.Pequeno)
            }
            Text("$valor", fontFamily = Mono, fontSize = 32.sp, fontWeight = FontWeight.Medium, color = C.Tinta)
        }
    }
}

@Composable
private fun Accion(titulo: String, detalle: String, icono: ImageVector, principal: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    val fondo = if (principal) C.Acento else C.Superficie
    val tinta = if (principal) Color.White else C.Tinta
    var mod = Modifier.fillMaxWidth().clip(forma).background(fondo)
    if (!principal) mod = mod.border(1.dp, C.Borde, forma)
    Row(
        mod.clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(if (principal) Color.White.copy(alpha = 0.14f) else C.Gris),
            contentAlignment = Alignment.Center
        ) { Icon(icono, contentDescription = null, tint = tinta) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = tinta)
            Text(detalle, fontSize = 13.sp, color = if (principal) Color(0xFFDCE5F7) else C.Secundario)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = tinta)
    }
}

@Composable
private fun FilaReciente(r: Registro) {
    val devuelto = r.horaEntregaAlumno
    Column(Modifier.padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(r.nombre, style = Tx.CuerpoM, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.semestre}° semestre · ${r.especialidad}", style = Tx.Pequeno)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Fechas.horaCorta(devuelto ?: r.horaEntregaAdmin), fontFamily = Mono, fontSize = 14.sp, color = C.Tinta)
                Text(if (devuelto != null) "Devolución" else "Entrega", style = Tx.Mini)
            }
        }
        HorizontalDivider(color = C.Borde)
    }
}
