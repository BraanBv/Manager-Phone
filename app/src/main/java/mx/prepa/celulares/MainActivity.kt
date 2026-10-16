package mx.prepa.celulares

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.prepa.celulares.ui.AppViewModel
import mx.prepa.celulares.ui.ConfirmarDevolucionScreen
import mx.prepa.celulares.ui.ConfirmarEntregaScreen
import mx.prepa.celulares.ui.EscanerScreen
import mx.prepa.celulares.ui.ExportarScreen
import mx.prepa.celulares.ui.Filtro
import mx.prepa.celulares.ui.InicioScreen
import mx.prepa.celulares.ui.ManualScreen
import mx.prepa.celulares.ui.Modo
import mx.prepa.celulares.ui.Pestana
import mx.prepa.celulares.ui.RegistrosScreen
import mx.prepa.celulares.ui.TemaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Iconos oscuros en la barra de estado sobre el fondo claro
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            TemaApp { App() }
        }
    }
}

private object Rutas {
    const val INICIO = "inicio"
    const val ESCANEAR = "escanear/{modo}"
    const val CONFIRMAR_ENTREGA = "confirmarEntrega"
    const val CONFIRMAR_DEVOLUCION = "confirmarDevolucion/{id}"
    const val REGISTROS = "registros?filtro={filtro}"
    const val EXPORTAR = "exportar"
    const val MANUAL = "manual"

    fun escanear(modo: Modo) = "escanear/${modo.name}"
    fun confirmarDevolucion(id: Long) = "confirmarDevolucion/$id"
    fun registros(filtro: Filtro = Filtro.TODOS) = "registros?filtro=${filtro.name}"
}

@Composable
private fun App() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()

    fun irAPestana(p: Pestana) {
        if (p == Pestana.INICIO) {
            volverAInicio(nav)
            return
        }
        val ruta = if (p == Pestana.REGISTROS) Rutas.registros() else Rutas.EXPORTAR
        nav.navigate(ruta) {
            popUpTo(Rutas.INICIO) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = nav,
        startDestination = Rutas.INICIO,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(Rutas.INICIO) {
            InicioScreen(
                vm = vm,
                onRecibir = { nav.navigate(Rutas.escanear(Modo.RECEPCION)) },
                onDevolver = { nav.navigate(Rutas.escanear(Modo.DEVOLUCION)) },
                onVerTodo = { irAPestana(Pestana.REGISTROS) },
                onPestana = ::irAPestana
            )
        }

        composable(
            Rutas.ESCANEAR,
            arguments = listOf(navArgument("modo") { type = NavType.StringType })
        ) { entrada ->
            val modo = runCatching { Modo.valueOf(entrada.arguments?.getString("modo") ?: "") }.getOrDefault(Modo.RECEPCION)
            EscanerScreen(
                vm = vm,
                modoInicial = modo,
                onAtras = { volverAInicio(nav) },
                onConfirmarEntrega = { nav.navigate(Rutas.CONFIRMAR_ENTREGA) },
                onConfirmarDevolucion = { id -> nav.navigate(Rutas.confirmarDevolucion(id)) },
                onManual = { nav.navigate(Rutas.MANUAL) },
                onBuscarEnResguardo = { nav.navigate(Rutas.registros(Filtro.RESGUARDO)) }
            )
        }

        composable(Rutas.CONFIRMAR_ENTREGA) {
            ConfirmarEntregaScreen(
                vm = vm,
                onAtras = { nav.popBackStack() },
                // Tras registrar, regresa al escáner para recibir el siguiente celular
                onListo = { volverAEscaner(nav, Modo.RECEPCION) }
            )
        }

        composable(
            Rutas.CONFIRMAR_DEVOLUCION,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong("id") ?: 0L
            ConfirmarDevolucionScreen(
                vm = vm,
                id = id,
                onAtras = { nav.popBackStack() },
                onListo = {
                    // Si se abrió desde la lista de registros, regresa a la lista; si no, al escáner
                    val anterior = nav.previousBackStackEntry?.destination?.route.orEmpty()
                    if (anterior.startsWith("registros")) nav.popBackStack()
                    else volverAEscaner(nav, Modo.DEVOLUCION)
                }
            )
        }

        composable(
            Rutas.REGISTROS,
            arguments = listOf(navArgument("filtro") { type = NavType.StringType; defaultValue = Filtro.TODOS.name })
        ) { entrada ->
            val filtro = runCatching { Filtro.valueOf(entrada.arguments?.getString("filtro") ?: "") }.getOrDefault(Filtro.TODOS)
            RegistrosScreen(
                vm = vm,
                filtroInicial = filtro,
                onAbrir = { r -> nav.navigate(Rutas.confirmarDevolucion(r.id)) },
                onExportar = { nav.navigate(Rutas.EXPORTAR) },
                onPestana = ::irAPestana
            )
        }

        composable(Rutas.EXPORTAR) {
            ExportarScreen(vm = vm, onAtras = { if (!nav.popBackStack()) irAPestana(Pestana.INICIO) })
        }

        composable(Rutas.MANUAL) {
            ManualScreen(
                vm = vm,
                onAtras = { nav.popBackStack() },
                onContinuar = {
                    nav.navigate(Rutas.CONFIRMAR_ENTREGA) {
                        popUpTo(Rutas.MANUAL) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun volverAInicio(nav: NavHostController) {
    nav.popBackStack(Rutas.INICIO, inclusive = false)
}

/** Deja la pila como Inicio → Escáner, listo para el siguiente alumno. */
private fun volverAEscaner(nav: NavHostController, modo: Modo) {
    nav.navigate(Rutas.escanear(modo)) {
        popUpTo(Rutas.INICIO) { inclusive = false }
        launchSingleTop = true
    }
}
