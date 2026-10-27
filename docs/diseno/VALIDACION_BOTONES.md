# Validación de composición de botones

Fecha: 26 de octubre de 2026. Rama: `feature/persistencia`, sobre `641abd9`.

Comando: `./gradlew.bat testDebugUnitTest lintDebug connectedDebugAndroidTest`.

Resultado final: **BUILD SUCCESSFUL**. JDK 17; Pixel 7 Pro, Android 13 / API 33.

- Unitarias: 118, sin fallos, errores ni omisiones.
- Android: 23, sin fallos, errores ni omisiones.
- Lint: 0 errores; 37 advertencias.

[Totales extraídos de los XML de Gradle](validacion-botones.json).

La primera ejecución no pudo ejecutar Android porque no había dispositivo conectado. Se inició el emulador y se completó la ejecución final sin fallos. No se alteraron ni omitieron pruebas.

El diff de aplicación se limita a seis pantallas: orden, agrupación, separación y ancho de controles. Se conservan colores, formas, tipografías, textos, callbacks, validación y persistencia. Las regresiones existentes cubren apertura, navegación, salud, tareas, decisiones, inicio y cierre. Esta evidencia no sustituye una evaluación con usuarios ni una revisión en todos los tamaños de pantalla.

[Diagnóstico y tres variantes](BOTONES_LAYOUT.md).
