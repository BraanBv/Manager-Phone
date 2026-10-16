package mx.prepa.celulares.ui

import android.Manifest
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun EscanerScreen(
    vm: AppViewModel,
    modoInicial: Modo,
    onAtras: () -> Unit,
    onConfirmarEntrega: () -> Unit,
    onConfirmarDevolucion: (Long) -> Unit,
    onManual: () -> Unit,
    onBuscarEnResguardo: () -> Unit
) {
    val context = LocalContext.current
    var modo by rememberSaveable { mutableStateOf(modoInicial) }
    var tienePermiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permisoNegado by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tienePermiso = ok
        permisoNegado = !ok
    }
    LaunchedEffect(Unit) { if (!tienePermiso) pedirPermiso.launch(Manifest.permission.CAMERA) }

    var error by remember { mutableStateOf<String?>(null) }
    var linterna by remember { mutableStateOf(false) }
    var camara by remember { mutableStateOf<Camera?>(null) }
    val procesando = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val modoActual by rememberUpdatedState(modo)

    val alLeer: (String) -> Unit = { texto ->
        if (procesando.compareAndSet(false, true)) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when (val r = vm.procesarQr(texto, modoActual)) {
                    ResultadoEscaneo.IrAConfirmarEntrega -> onConfirmarEntrega()
                    is ResultadoEscaneo.IrAConfirmarDevolucion -> onConfirmarDevolucion(r.id)
                    is ResultadoEscaneo.Error -> {
                        error = r.mensaje
                        delay(2500)
                        error = null
                        procesando.set(false)
                    }
                }
            }
        }
    }

    // Iconos claros en la barra de estado mientras la pantalla es oscura
    val vista = LocalView.current
    DisposableEffect(Unit) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        control?.isAppearanceLightStatusBars = false
        onDispose { control?.isAppearanceLightStatusBars = true }
    }

    val esRecepcion = modo == Modo.RECEPCION
    val colorMarco = if (esRecepcion) Color.White else C.ResguardoEscaner

    Column(Modifier.fillMaxSize().background(C.Escaner)) {
        BarraSuperior(
            titulo = if (esRecepcion) "Recibir celular" else "Devolver celular",
            onAtras = onAtras,
            oscuro = true
        ) {
            if (camara?.cameraInfo?.hasFlashUnit() == true) {
                IconButton(
                    onClick = {
                        linterna = !linterna
                        camara?.cameraControl?.enableTorch(linterna)
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
                ) {
                    Icon(
                        if (linterna) Icons.Outlined.FlashOff else Icons.Outlined.FlashOn,
                        contentDescription = if (linterna) "Apagar linterna" else "Encender linterna"
                    )
                }
            }
        }

        // Selector Recepción / Devolución
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Segmento("Recepción", esRecepcion, Modifier.weight(1f)) { modo = Modo.RECEPCION; error = null }
            Segmento("Devolución", !esRecepcion, Modifier.weight(1f)) { modo = Modo.DEVOLUCION; error = null }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (tienePermiso) {
                VistaCamara(
                    onLectura = alLeer,
                    onCamara = { camara = it },
                    modifier = Modifier.fillMaxSize()
                )
                MarcoEscaneo(colorMarco)
            } else {
                Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Se necesita acceso a la cámara para leer los códigos QR.",
                        color = Color(0xFFC9CDD5), fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                    Text(
                        if (permisoNegado) "Abrir ajustes" else "Permitir cámara",
                        color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Acento)
                            .clickable {
                                if (permisoNegado) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                                    )
                                } else pedirPermiso.launch(Manifest.permission.CAMERA)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            // Mensaje de error o instrucción bajo el marco
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp, start = 24.dp, end = 24.dp)) {
                val e = error
                if (e != null) {
                    Row(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(C.ErrorSuave).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = C.Error, modifier = Modifier.size(20.dp))
                        Text(e, color = C.Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (tienePermiso) {
                    Text(
                        if (esRecepcion) "Apunta al código QR que muestra el celular del alumno"
                        else "Escanea el QR del celular que vas a devolver al alumno",
                        color = Color(0xFFC9CDD5), fontSize = 15.sp, textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Panel inferior
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(C.Superficie)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (esRecepcion) "Se registrará la hora de entrega al administrador automáticamente."
                else "Se registrará la hora de entrega al alumno automáticamente.",
                style = Tx.Pequeno
            )
            if (esRecepcion) BotonSecundario("Ingresar datos manualmente", onManual, icono = Icons.Outlined.Keyboard)
            else BotonSecundario("Buscar en celulares en resguardo", onBuscarEnResguardo, icono = Icons.Outlined.Search)
        }
    }
}

@Composable
private fun Segmento(texto: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (activo) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontSize = 14.sp,
            fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
            color = if (activo) C.Tinta else Color(0xFFC9CDD5)
        )
    }
}

/** Oscurece todo menos el recuadro central y dibuja las esquinas y la línea de escaneo. */
@Composable
private fun MarcoEscaneo(color: Color) {
    val transicion = rememberInfiniteTransition(label = "linea")
    val avance by transicion.animateFloat(
        initialValue = 0.12f, targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "avance"
    )
    Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val lado = 248.dp.toPx()
        val radio = 20.dp.toPx()
        val esquina = 40.dp.toPx()
        val grosor = 3.dp.toPx()
        val izq = (size.width - lado) / 2f
        val arr = (size.height - lado) / 2f

        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(Color.Transparent, Offset(izq, arr), Size(lado, lado), CornerRadius(radio), blendMode = BlendMode.Clear)

        val trazo = Stroke(width = grosor, cap = StrokeCap.Round)
        val d = radio * 2
        // Cuatro esquinas: un arco y dos líneas cortas cada una
        val der = izq + lado
        val aba = arr + lado
        // superior izquierda
        drawArc(color, 180f, 90f, false, Offset(izq, arr), Size(d, d), style = trazo)
        drawLine(color, Offset(izq + radio, arr), Offset(izq + esquina, arr), grosor, StrokeCap.Round)
        drawLine(color, Offset(izq, arr + radio), Offset(izq, arr + esquina), grosor, StrokeCap.Round)
        // superior derecha
        drawArc(color, 270f, 90f, false, Offset(der - d, arr), Size(d, d), style = trazo)
        drawLine(color, Offset(der - radio, arr), Offset(der - esquina, arr), grosor, StrokeCap.Round)
        drawLine(color, Offset(der, arr + radio), Offset(der, arr + esquina), grosor, StrokeCap.Round)
        // inferior izquierda
        drawArc(color, 90f, 90f, false, Offset(izq, aba - d), Size(d, d), style = trazo)
        drawLine(color, Offset(izq + radio, aba), Offset(izq + esquina, aba), grosor, StrokeCap.Round)
        drawLine(color, Offset(izq, aba - radio), Offset(izq, aba - esquina), grosor, StrokeCap.Round)
        // inferior derecha
        drawArc(color, 0f, 90f, false, Offset(der - d, aba - d), Size(d, d), style = trazo)
        drawLine(color, Offset(der - radio, aba), Offset(der - esquina, aba), grosor, StrokeCap.Round)
        drawLine(color, Offset(der, aba - radio), Offset(der, aba - esquina), grosor, StrokeCap.Round)

        // línea de escaneo
        val y = arr + lado * avance
        val linea = if (color == Color.White) Color(0xFF6C93E8) else color
        drawLine(linea, Offset(izq + 24.dp.toPx(), y), Offset(der - 24.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun VistaCamara(onLectura: (String) -> Unit, onCamara: (Camera?) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alLeer by rememberUpdatedState(onLectura)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val futuro = ProcessCameraProvider.getInstance(context)
        val ejecutor = Executors.newSingleThreadExecutor()
        val lector = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        futuro.addListener({
            val proveedor = futuro.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analisis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analisis.setAnalyzer(ejecutor, AnalizadorQr(lector) { texto -> alLeer(texto) })
            try {
                proveedor.unbindAll()
                onCamara(proveedor.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis))
            } catch (_: Exception) {
                onCamara(null)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            if (futuro.isDone) runCatching { futuro.get().unbindAll() }
            onCamara(null)
            ejecutor.shutdown()
            lector.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Pasa cada cuadro de la cámara a ML Kit; el resultado llega en el hilo principal. */
private class AnalizadorQr(
    private val lector: BarcodeScanner,
    private val onTexto: (String) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null) {
            proxy.close(); return
        }
        val imagen = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        lector.process(imagen)
            .addOnSuccessListener { codigos ->
                codigos.firstNotNullOfOrNull { c -> c.rawValue?.takeIf { it.isNotBlank() } }?.let(onTexto)
            }
            .addOnCompleteListener { proxy.close() }
    }
}
