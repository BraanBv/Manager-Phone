package mx.prepa.celulares.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.prepa.celulares.util.QrParser
import mx.prepa.celulares.util.ResultadoQr

/** Captura manual para cuando un QR no se puede leer. */
@Composable
fun ManualScreen(vm: AppViewModel, onAtras: () -> Unit, onContinuar: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var semestre by rememberSaveable { mutableStateOf<Int?>(null) }
    var especialidad by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(C.Fondo).imePadding()) {
        BarraSuperior("Captura manual", onAtras = onAtras)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Usa esta opción solo si el QR del alumno no se puede leer.", style = Tx.Pequeno)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NOMBRE DEL ALUMNO", style = Tx.Seccion)
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    placeholder = { Text("Nombre completo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = C.Superficie,
                        unfocusedContainerColor = C.Superficie,
                        unfocusedBorderColor = C.Borde,
                        focusedBorderColor = C.Acento
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SEMESTRE", style = Tx.Seccion)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { s -> Chip("$s°", semestre == s) { semestre = s; error = null } }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ESPECIALIDAD", style = Tx.Seccion)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("COMP", "SEB").forEach { e -> Chip(e, especialidad == e) { especialidad = e; error = null } }
                }
            }

            error?.let { Text(it, color = C.Error, style = Tx.Pequeno.copy(color = C.Error)) }
        }
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
            BotonPrincipal("Continuar", onClick = {
                when (val r = QrParser.validar(nombre, semestre?.toString(), especialidad)) {
                    is ResultadoQr.Error -> error = r.mensaje
                        .replace(" en el QR", "")
                        .replace(" del alumno en el QR", "")
                    is ResultadoQr.Ok -> scope.launch {
                        vm.prepararEntrega(r.datos, "captura manual")
                        onContinuar()
                    }
                }
            })
        }
    }
}
