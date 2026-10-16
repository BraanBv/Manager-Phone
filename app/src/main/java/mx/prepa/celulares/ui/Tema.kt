package mx.prepa.celulares.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta del diseño: fondo gris cálido, un solo acento azul, naranja = en resguardo. */
object C {
    val Fondo = Color(0xFFF6F6F4)
    val Superficie = Color(0xFFFFFFFF)
    val Borde = Color(0xFFE3E3DF)
    val BordeFuerte = Color(0xFFD5D6D2)
    val Divisor = Color(0xFFEDEDEA)
    val Tinta = Color(0xFF15171C)
    val Tinta2 = Color(0xFF3E4450)
    val Secundario = Color(0xFF5B616E)
    val Acento = Color(0xFF1B4DB1)
    val AcentoSuave = Color(0xFFE8EEFA)
    val AcentoOscuro = Color(0xFF143A86)
    val Resguardo = Color(0xFFC2620A)
    val ResguardoSuave = Color(0xFFFDF0E1)
    val ResguardoOscuro = Color(0xFF8A4205)
    val Gris = Color(0xFFEEF0F3)
    val Error = Color(0xFFB3261E)
    val ErrorSuave = Color(0xFFFCE8E6)
    val Escaner = Color(0xFF0E1014)
    val ResguardoEscaner = Color(0xFFF0A35E)
    val Excel = Color(0xFF1D6B45)
    val ExcelSuave = Color(0xFFE6F2EC)
}

val Mono = FontFamily.Monospace

@Composable
fun TemaApp(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Acento,
            onPrimary = Color.White,
            background = C.Fondo,
            surface = C.Superficie,
            onSurface = C.Tinta,
            onBackground = C.Tinta,
            outline = C.BordeFuerte,
            error = C.Error
        ),
        content = content
    )
}

object Tx {
    val Titulo = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta, letterSpacing = (-0.2).sp)
    val Barra = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta)
    val Cuerpo = TextStyle(fontSize = 15.sp, color = C.Tinta)
    val CuerpoM = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Tinta)
    val Pequeno = TextStyle(fontSize = 13.sp, color = C.Secundario)
    val Mini = TextStyle(fontSize = 12.sp, color = C.Secundario)
    val Seccion = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Secundario, letterSpacing = 0.7.sp)
}

/** Encabezado con botón de regreso, como en las pantallas secundarias del diseño. */
@Composable
fun BarraSuperior(titulo: String, onAtras: () -> Unit, oscuro: Boolean = false, acciones: @Composable RowScope.() -> Unit = {}) {
    val color = if (oscuro) Color.White else C.Tinta
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAtras) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = color)
        }
        Text(titulo, style = Tx.Barra.copy(color = color), modifier = Modifier.weight(1f))
        acciones()
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icono: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.Acento, contentColor = Color.White)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, C.BordeFuerte),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = C.Tinta)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BotonTexto(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = C.Tinta2)
    ) { Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(20.dp), radio: Int = 16, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(radio.dp))
            .background(C.Superficie)
            .border(1.dp, C.Borde, RoundedCornerShape(radio.dp))
            .padding(padding)
    ) { content() }
}

@Composable
fun Etiqueta(texto: String) {
    Text(
        texto,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.Tinta2),
        modifier = Modifier
            .border(1.dp, C.BordeFuerte, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun Estado(enResguardo: Boolean) {
    val (fondo, tinta, texto) = if (enResguardo) Triple(C.ResguardoSuave, C.ResguardoOscuro, "En resguardo")
    else Triple(C.AcentoSuave, C.AcentoOscuro, "Devuelto")
    Text(
        texto,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tinta),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun Aviso(texto: String, fondo: Color, tinta: Color, icono: ImageVector) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(20.dp))
        Text(texto, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tinta))
    }
}

@Composable
fun Avatar(nombre: String) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.let { p ->
        ((p.firstOrNull()?.take(1) ?: "") + (p.getOrNull(if (p.size >= 3) p.size - 2 else 1)?.take(1) ?: "")).uppercase()
    }
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(C.Gris),
        contentAlignment = Alignment.Center
    ) { Text(iniciales, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.Tinta2) }
}

/** Chip de filtro: negro relleno si está activo, contorno si no. */
@Composable
fun Chip(texto: String, activo: Boolean, onClick: () -> Unit) {
    Text(
        texto,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (activo) Color.White else C.Tinta2),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (activo) C.Tinta else C.Superficie)
            .border(1.dp, if (activo) C.Tinta else C.BordeFuerte, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

enum class Pestana { INICIO, REGISTROS, EXPORTAR }

@Composable
fun BarraInferior(actual: Pestana, onIr: (Pestana) -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Superficie)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Borde))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
            ItemBarra(Icons.Outlined.Home, "Inicio", actual == Pestana.INICIO) { onIr(Pestana.INICIO) }
            ItemBarra(Icons.AutoMirrored.Outlined.List, "Registros", actual == Pestana.REGISTROS) { onIr(Pestana.REGISTROS) }
            ItemBarra(Icons.Outlined.FileDownload, "Exportar", actual == Pestana.EXPORTAR) { onIr(Pestana.EXPORTAR) }
        }
    }
}

@Composable
private fun RowScope.ItemBarra(icono: ImageVector, texto: String, activo: Boolean, onClick: () -> Unit) {
    val color = if (activo) C.Acento else C.Secundario
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(texto, fontSize = 12.sp, fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium, color = color)
    }
}
