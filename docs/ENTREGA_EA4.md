# Entrega EA4 de AquaFlow

La APK original de esta entrega se identificó como **1.0.0-rc.1**, código **2**, paquete `com.duoc.aquaflow`. El [repositorio de AquaFlow](https://github.com/FcoINF/Proyecto-AquaChile) es público.

La APK generada en esta sesión se verificó con el esquema de firma v2 y el certificado local `AquaFlow Defensa Local`. Su SHA-256 es `7889d681dcc09ef4a0d4a631074b939fa2e9a1aa2ab10a49f1ec337c8340fbd5`. Se compiló desde `develop` con cambios locales, incluidos los de la migración de Room a la versión 8 y la paleta del calendario.

## APK release

Usa el mismo almacén de claves para futuras actualizaciones de la APK. Las variables `AQUAFLOW_RELEASE_KEYSTORE`, `AQUAFLOW_RELEASE_STORE_PASSWORD`, `AQUAFLOW_RELEASE_KEY_ALIAS` y `AQUAFLOW_RELEASE_KEY_PASSWORD` se configuran en la sesión de compilación. Conserva el almacén de claves y sus credenciales fuera de Git y de los archivos compartidos en AVA.

```powershell
.\gradlew.bat :app:assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
Get-FileHash app/build/outputs/apk/release/app-release.apk -Algorithm SHA256
git rev-parse HEAD
git status --short
```

La release usa Cloudflare para iniciar sesión por defecto. Para probar el intercambio de expedientes con Spring Boot, compila con `-PaquaflowApiUrl=https://<servidor>/` y usa un servidor accesible desde el teléfono. Para la demo local, compila con `-PaquaflowApiUrl=`. Si usas Spring Boot en el emulador, compila en debug con `-PaquaflowApiUrl=http://10.0.2.2:8080/`.

En el teléfono de la defensa, autoriza la depuración USB, identifica el número de serie con `adb devices -l` e instala con `adb -s <serie> install -r app/build/outputs/apk/release/app-release.apk`. Si Android detecta una firma distinta de la instalada, conserva la instalación y sus datos hasta decidir qué dispositivo usar para la defensa.

## Evidencias para la defensa

| Evidencia | Qué registrar | Estado |
| --- | --- | --- |
| Compose | El detalle de un expediente crítico muestra la alerta y mantiene deshabilitado **Cerrar faena**. Conservar el informe de la prueba instrumentada. | La prueba compiló. Falta ejecutarla con ADB. |
| Swagger | Con jefatura, consultar el ID de una apertura creada por un supervisor. Capturar la respuesta 200 de `GET /api/faenas/{id}`. | [Swagger y OpenAPI verificados en la integración](evidencias/ea4/api.md). Falta la captura visual. |
| Envío y reintento | Confirmar la apertura en Android y consultar el mismo ID en Swagger. Enviar el mismo cierre dos veces y verificar que jefatura ve un solo registro en **Cierres recibidos y alertas**. | El reenvío de apertura y cierre se verificó en la API. Falta la demostración desde Android. La sincronización completa del expediente sigue pendiente. |
| Roles | Mostrar la apertura desde supervisor y su consulta desde jefatura. Registrar el 403 cuando jefatura intenta escribir una apertura. | Respuestas verificadas en la integración de la API. |
| Trello | Adjuntar el enlace al tablero del equipo y una captura de las tarjetas de la entrega con su estado y responsable. | Falta el enlace al tablero. |
| Cobertura | Adjuntar un informe de ViewModel y Repository que acredite el umbral de la pauta. | Pendiente. Se excluyeron las pruebas unitarias de este trabajo. |

Para ejecutar la prueba Compose en un dispositivo conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.FlujoOperativoUiTest#expedienteCriticoMuestraAlertaYDeshabilitaCerrarFaena'
```

El registro en AVA requiere confirmar los nombres de **las dos personas que entregarán**. El README conserva la lista de integrantes del equipo.
