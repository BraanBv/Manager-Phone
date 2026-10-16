# Control de celulares — App Android

App para registrar la entrega y devolución de los celulares de los alumnos, leyendo un código QR.
Todo se guarda **localmente en el teléfono** (no necesita internet) y se exporta a **Excel (.xlsx)**.

## Datos que registra

| Dato | Cómo se obtiene |
|---|---|
| Hora de entrega al administrador | Automática, al escanear el QR en **Recepción** |
| Nombre del alumno | Del QR |
| Semestre | Del QR (1 a 6) |
| Especialidad (COMP / SEB) | Del QR |
| Hora en que se le entregó al alumno | Automática, al confirmar en **Devolución** |

## Flujo

1. **Inicio** → *Recibir celular* → escanear QR → **Confirmar entrega** → regresa al escáner para el siguiente alumno.
2. **Inicio** → *Devolver celular* → escanear QR → **Confirmar devolución** (muestra el tiempo en resguardo).
3. **Registros**: búsqueda por nombre y filtros (En resguardo, Devueltos, COMP, SEB). Toca un registro en resguardo para devolverlo; mantén presionado para eliminarlo.
4. **Exportar**: periodo (Hoy, Semana o Rango de fechas), especialidad y columnas → *Exportar a Excel* (eliges dónde guardarlo) o *Compartir* (WhatsApp, correo, Drive…).

Si un QR no se puede leer, en Recepción está **Ingresar datos manualmente**.

## Formato del QR del alumno

La app acepta cualquiera de estos tres formatos (puedes generarlos con cualquier generador de QR):

```
Ana Sofía Ramírez López|5|COMP
```
```
Nombre: Ana Sofía Ramírez López
Semestre: 5
Especialidad: COMP
```
```json
{"nombre": "Ana Sofía Ramírez López", "semestre": 5, "especialidad": "COMP"}
```

- En el formato con `|` el orden no importa; también acepta `;` o `,` como separador.
- Semestre: `5`, `5°`, `5to` o `quinto`.
- Especialidad: `COMP` (o `Computación`) y `SEB`.
- La devolución busca el celular en resguardo **por nombre**, así que el nombre del QR debe ser siempre el mismo.

## Cómo generar el APK

### Opción A — Android Studio (recomendada)
1. Instala [Android Studio](https://developer.android.com/studio) (versión Ladybug 2024.2 o más reciente).
2. *File → Open* y elige la carpeta `ControlCelulares`. Espera a que termine la sincronización de Gradle.
3. Conecta el teléfono con *Depuración USB* activada y presiona ▶ **Run**.
   Para obtener el archivo: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*; queda en `app/build/outputs/apk/`.

### Opción B — GitHub (sin instalar nada)
1. Sube esta carpeta a un repositorio de GitHub.
2. En la pestaña **Actions** se ejecuta *Compilar APK* automáticamente.
3. Al terminar (unos 5 min), descarga el artefacto **ControlCelulares-apk** e instálalo en el teléfono
   (Android pedirá permitir “instalar apps de fuentes desconocidas”).

### Línea de comandos
```
./gradlew assembleRelease
```
Requiere JDK 17 y el Android SDK (variable `ANDROID_HOME` o archivo `local.properties`).

> El APK de release está firmado con la clave de depuración para poder instalarlo directamente.
> Para publicarlo en Google Play, configura tu propia clave de firma en `app/build.gradle.kts`.

## Requisitos

- Android 8.0 o superior, con cámara.
- Permiso de cámara (se pide la primera vez que se abre el escáner).

## Estructura

```
app/src/main/java/mx/prepa/celulares/
├── MainActivity.kt          Navegación entre pantallas
├── data/                    Base de datos local (Room): Registro, RegistroDao, AppDatabase
├── util/
│   ├── QrParser.kt          Interpreta el contenido del QR
│   ├── Exportador.kt        Filtros de periodo/especialidad/columnas y formato de fechas
│   └── XlsxWriter.kt        Genera el archivo .xlsx (sin librerías externas)
└── ui/
    ├── Tema.kt              Colores, tipografía y componentes comunes
    ├── InicioScreen.kt      Inicio con contadores y actividad reciente
    ├── EscanerScreen.kt     Cámara + lectura de QR (CameraX + ML Kit, sin internet)
    ├── ConfirmarScreens.kt  Confirmar entrega / Confirmar devolución
    ├── RegistrosScreen.kt   Lista, búsqueda y filtros
    ├── ExportarScreen.kt    Opciones de exportación a Excel
    └── ManualScreen.kt      Captura manual
```
