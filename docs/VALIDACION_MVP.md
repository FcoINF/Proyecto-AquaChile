# Validación del avance local de AquaFlow

## Flujo operativo completo — 26 de octubre de 2026

Evidencia vigente de `feature/persistencia`, que conserva `develop` (`868808c`) y agrega [el ciclo operativo completo](FLUJO_OPERATIVO.md). Los resultados anteriores se mantienen debajo como registros históricos.

Comando ejecutado: `./gradlew.bat testDebugUnitTest lintDebug connectedDebugAndroidTest`. **BUILD SUCCESSFUL** antes de subir los cambios. Entorno: JDK 17, Pixel 7 Pro / Android 13 (API 33).

| Verificación | Resultado |
|---|---|
| Unitarias | 118 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Android | 23 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Lint | 0 errores, 37 advertencias |
| Room | Migraciones de versiones 1–5 a v6, sin borrado de datos |

Evidencias: [unitarias](evidencias/flujo-operativo/unitarias.json), [Android](evidencias/flujo-operativo/android.json), [Lint](evidencias/flujo-operativo/lint.json).

Se comprueban tareas obligatorias y borradores, autorización previa al inicio, propiedad y rol del supervisor, rechazo de doble inicio concurrente en Room, cierre obligatorio con salud de salida válida, preservación de la salud inicial, recuperación tras reabrir en ejecución y tras cerrar, filtros combinados de fecha/centro/buzo/estado y controles de pantalla. Pasan también las regresiones de apertura, DPR-24, fotografías, salud, AST y navegación existentes. Las reglas de salud y AST no tienen diferencias respecto de develop.

La prueba de fotografías existente utiliza un archivo de prueba; la captura con cámara física sigue siendo una comprobación manual. La operación entre roles permanece local al mismo dispositivo.

## Validación histórica: integración sobre develop (25 de octubre de 2026)

Rama de entrega: `feature/persistencia`. Base: `origin/develop` en `868808c`. Esta ejecución reemplaza como evidencia vigente las pruebas de la primera implementación sobre la base antigua.

Comando final: `./gradlew.bat testDebugUnitTest lintDebug connectedDebugAndroidTest`, con JDK 17 y Pixel 7 Pro / Android 13 (API 33). Resultado: **BUILD SUCCESSFUL** antes de publicar.

| Verificación | Resultado |
|---|---|
| Pruebas unitarias | 113, 0 fallos, 0 errores, 0 omitidas |
| Pruebas Android | 21, 0 fallos, 0 errores, 0 omitidas |
| Lint | 0 errores; 37 advertencias, sin supresiones nuevas |
| Compatibilidad | Migraciones hasta Room v5 y reapertura de base verificadas |

Evidencia extraída de los informes finales: [unitarias](evidencias/estados-secciones/unitarias.json), [Android](evidencias/estados-secciones/android.json) y [Lint](evidencias/estados-secciones/lint.json).

Cobertura específica de integración:

- Los botones de borrador de checklist, salud y AST mantienen BORRADOR incluso con campos válidos; la confirmación posterior produce COMPLETA.
- Recuperación por ID del flujo actual de develop, conservando estados, categoría y badge del checklist. Aperturas incompletas no se confirman al cargar.
- Compatibilidad conservadora: JSON anterior sin estado requiere reconfirmación, porque develop guardaba también borradores sin distinguirlos.
- Bloqueo de ambas decisiones en el resumen y en `DetalleFaenaScreen`, además del repositorio transaccional.
- AST admite pasos vacíos y exige críticas confirmadas según develop. `Reglas.kt`, `ReglasAst.kt` y `AquaFlowDatabase.kt` no presentan diferencias respecto de esa base.
- Regresiones de apertura, encuesta de salud, navegación, migraciones, evidencias y cierre incluidas.

Ver [alcance completo](ESTADOS_SECCIONES.md). La ejecución instrumentada compiló los artefactos debug necesarios; no publica una release ni incorpora APK al repositorio.

## Validación histórica del 24 de octubre

## Resultado ejecutado

| Verificación | Resultado |
|---|---|
| Compilación de aplicación y APK de pruebas | Correcta |
| `testDebugUnitTest` | 100 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `connectedDebugAndroidTest` | 8 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Emulador | Pixel 7 Pro, Android 13 / API 33, autorizado por ADB |
| Entorno de compilación | Windows, JDK 17, compileSdk 35 |

Resultados extraídos de los XML de Gradle: [unitarias](evidencias/mvp/unitarias.json) y [Android](evidencias/mvp/android.json). Los totales incluyen las pruebas de ejemplo que ya existían en el proyecto.

## Cobertura relevante

- Guardado asíncrono: no navega antes del éxito; bloquea doble envío; conserva datos y permite reintentar después de fallar.
- Recuperación: restaura apertura y secciones; cambiar las condiciones iniciales invalida los antecedentes anteriores de forma coherente.
- Identidad y decisión: el formulario no reasigna propietario; el supervisor no puede usar la operación autenticada de decisión; una faena decidida no se edita.
- Fotografías: la referencia apunta a un archivo legible después de cerrar/reabrir Room; cancelar la captura no inventa fotos; el resultado pendiente se restaura antes de componer la pantalla.
- Migraciones reales: bases creadas desde los esquemas 1, 2 y 3 abren con Room v4 conservando el registro. No se usa recuperación destructiva.
- Ciclo local: apertura, checklist, salud, AST, decisión y cierre sobreviven al cierre/reapertura de la base en disco.
- Cierre: exige datos válidos, revisión de salida, autorización y propietario/jefatura; no admite cierre de rechazada ni sobrescritura. Responsable y hora provienen de sesión/dispositivo.
- UI Compose: formulario de cierre valida y exige confirmación antes de enviar.
- UI de navegación real: login → apertura → checklist → salud → AST → inicio → nueva faena. La segunda encuesta comienza vacía, sin reutilizar los signos de la primera.

## Guion de demostración para el equipo

1. Ingresar como Supervisor (`supervisor` / `1234`). Crear faena y mostrar fecha/hora del dispositivo y propietario de sesión.
2. Intentar continuar sin completar datos y observar la validación. Completar centro, buzo y condiciones iniciales.
3. Revisar los cuatro equipos. Capturar una foto real; cancelar otra captura para demostrar que no agrega una evidencia falsa.
4. Guardar checklist, completar salud con datos ficticios y completar AST. Consultar el resumen.
5. Reiniciar la aplicación y volver a ingresar. En Historial buscar la faena, abrir el detalle y mostrar secciones y foto.
6. Cambiar a Administrador (`jefe` / `1234`) en el mismo dispositivo. Autorizar la faena desde el detalle; demostrar también un rechazo con motivo usando otra faena.
7. Abrir la autorizada y registrar minutos, observaciones y revisión de salida. Confirmar el cierre, filtrar por Cerrada y comprobar que permanece tras reiniciar.
8. Mostrar las clases reales UI, ViewModel, StateFlow, Repository, Entity, DAO y Database; localizar los commits y las tareas.

## Verificaciones pendientes del equipo

- Captura completa con la aplicación de cámara de un teléfono físico, permisos/acceso y cancelación. La prueba automatizada de fotos escribe una imagen de prueba a través de FileProvider; no acciona el obturador de una cámara física.
- Ensayo del flujo en modo avión y revisión visual con diferentes tamaños de pantalla. Las pruebas locales no dependen de un backend, pero eso no sustituye el ensayo de campo.
- Revisión con el cliente de campos, catálogos, checklist, AST, reglas de salud y formulario de cierre. Los datos actuales son académicos.
- Ensayo individual de 15 minutos, modificación en vivo sin IA durante la defensa y entrega por AVA.
- Evidencia de participación de cada integrante: los commits de esta intervención no acreditan por sí solos colaboración de todo el equipo.

## Límites conocidos

No existe backend definido: no hay identidad remota ni sincronización entre dispositivos. Los archivos de fotos retiradas del checklist se conservan hasta implementar una limpieza segura. Las imágenes simuladas por versiones anteriores no pueden recuperarse a partir de sus nombres ficticios. Un cierre registra los datos introducidos por el usuario; no integra sensores ni telemetría automática de inmersión.
