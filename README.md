# AquaFlow - Control de Pre-Chequeo para Faenas de Buceo

## Descripción del Proyecto

AquaFlow es una aplicación móvil Android diseñada para la empresa AquaChile, cuyo objetivo es digitalizar el flujo de las faenas de buceo, permitiendo iniciar y verificar de forma rápida los requerimientos técnicos y de seguridad en terreno.

## Problemática

El proceso actual de pre-chequeo de seguridad para faenas de buceo se ejecuta mediante registros manuales en papel y bitácoras físicas. Esto expone la operación a:

- Omisiones involuntarias de pasos críticos de seguridad
- Pérdida de datos por condiciones climáticas marítimas adversas (vulnerabilidad a la intemperie)
- Falta de trazabilidad, lentitud en los procesos y nula analítica centralizada en tiempo real

## Solución Propuesta

Una aplicación móvil (offline-first) que reemplaza los formularios en papel para revisar el equipamiento técnico, realizar checklist de tareas operativas (AST), registrar datos de salud basal diarios de los trabajadores y capturar fotografías de evidencia in situ. Permite a los Jefes de Centro tomar decisiones seguras e informadas y centralizar la información.

## Características Principales (MVP)

- **Ingreso basado en roles**: Diferenciación de accesos e interfaces para perfiles de Administrador y Supervisor de Buceo
- **Digitalización de Formularios Obligatorios**: Pre-chequeo preventivo, AST (Análisis Seguro de Trabajo) y R-DPR-24
- **Modo Offline Activo**: Guardado local en el dispositivo (SQLite Room DB) previendo la falta de conexión en mar abierto, con sincronización posterior planificada (pendiente de backend)
- **Evidencia Fotográfica**: Uso de la cámara del dispositivo para capturar el estado físico y fallas de los equipos
- **Encuesta de Salud Física**: Registro de condiciones, presión, oxígeno y temperatura del buzo antes de cada inmersión
- **Módulo de Toma de Decisión**: Consolidación y resumen de faena para la autorización o rechazo (con motivo registrado) por parte de la jefatura
- **Historial de Faenas**: Acceso a expedientes digitalizados para auditar la información pasada de forma eficiente

## Flujo de Pantallas (User Flow)

El recorrido principal que sigue un operativo (Supervisor) dentro de la aplicación es:

1. **Login / Selección Rol**: Autenticación y selección del perfil correspondiente
2. **Dashboard (Inicio)**: Panel de control con resúmenes de operaciones en curso, tareas pendientes e historial
3. **Apertura de Faena**: Ingreso de los datos iniciales, selección de centro de cultivo, fecha y asignación de la cuadrilla/buzo
4. **Checklist DPR-24 (Equipamiento)**: Auditoría de equipos de soporte vital (compresor, rescate, alimentación) y captura de imágenes para observar fallas
5. **Formulario AST (Seguridad)**: Análisis de riesgos críticos y verificación de medidas de control (condiciones ambientales y de emergencia)
6. **Encuesta de Salud y Signos**: Registro de la declaración de salud y toma de signos vitales tácticos
7. **Autorización / Rechazo**: Consolidación final donde el sistema (y la jefatura) aprueba o cancela el descenso en base a la información recolectada
8. **Registro Posterior / Cierre**: Historial con la telemetría, tiempo de inmersión y revisión de salida del agua

## Identidad Visual y UI

La interfaz fue diseñada para asegurar la legibilidad en exteriores y bajo fuerte luminosidad, apoyada en **Material Design 3**:

| Elemento | Valor | Uso |
|----------|-------|-----|
| Color Principal | `#F95716` | Naranja intenso - Llamadas a la acción e iconografía principal |
| Color Secundario | `#0B1723` | Azul oscuro marino - Contenedores y contrastes profundos |
| Color de Fondo | `#FFFFFF` | Blanco puro - Facilitando la lectura |
| Tipografía | Inter (Bold) | Legibilidad inmediata |

**Logotipo**: Representa la continuidad fluida (sin interrupciones) de los buzos y su medio ambiente (el mar).

## Tecnologías Utilizadas

### Estado actual: avance funcional local

- **Flujo integrado:** apertura → DPR-24 y fotos → salud inicial → AST → tareas → resumen → autorización de jefatura → inicio por supervisor → salud de salida y cierre desde el historial.
- **Room v6:** apertura, condiciones iniciales, checklist, AST, salud, decisión y cierre. Migraciones 1→2→3→4→5→6 sin recuperación destructiva.
- **Estados de sección:** distingue borrador, completa y sin registrar al guardar y recuperar; integra los botones de borrador y el detalle de historial de `develop`. Las secciones antiguas sin estado requieren reconfirmación. Bloquea autorizar y rechazar expedientes incompletos en pantalla y en la transacción de persistencia. Ver [alcance de la primera propuesta](docs/ESTADOS_SECCIONES.md).
- **Guardado confirmado:** se avanza después de persistir; los errores quedan visibles y permiten reintentar. Recuperar una faena restaura sus secciones.
- **Identidad local:** el supervisor pertenece a la sesión y no puede cambiarse en el formulario. Las faenas decididas no se pueden editar; los cierres no se sobrescriben.
- **Fotografías:** captura a archivo interno con `TakePicture` y `FileProvider`; referencias en Room y miniaturas en checklist y detalle. Cancelar o fallar la cámara no agrega evidencias simuladas. Los nombres ficticios guardados por versiones antiguas no contienen imágenes recuperables: la app informa cuando la vista previa no está disponible.
- **Dos recursos del dispositivo:** cámara y fecha/hora automática, visible en apertura y detalle. La captura se delega a la app de cámara con acceso temporal al URI; AquaFlow no solicita el permiso de cámara para usar directamente el hardware.
- **Inicio:** solo el supervisor propietario inicia una faena autorizada; se registra responsable y fecha/hora, con estado En ejecución.
- **Tareas e historial:** checklist de tareas independiente del DPR-24; búsqueda por centro o buzo combinable con fecha exacta y estado. Ver [flujo operativo completo](docs/FLUJO_OPERATIVO.md).
- **Cierre:** desde el detalle de una faena en ejecución, el supervisor propietario registra minutos de inmersión, observaciones, confirmación de revisión de salida y una segunda encuesta de salud independiente. Se guarda responsable y fecha/hora; el historial muestra el estado Cerrada.
- **MVVM:** pantallas → ViewModels/StateFlow → repositorio → DAO → Room. Checklist y formulario de cierre conservan su borrador mediante `SavedStateHandle`.

### Ejecución y pruebas

1. Abrir el proyecto con Android Studio y configurar Android SDK 35.
2. Seleccionar un JDK compatible con Gradle; esta entrega fue verificada con **JDK 17**. `local.properties` debe apuntar al SDK de cada equipo y no se versiona.
3. Sincronizar Gradle y ejecutar `app` en Android 7.0/API 24 o superior. Las pruebas de esta entrega usan Pixel 7 Pro con Android 13/API 33.
4. Credenciales **de demostración**: perfil Supervisor, usuario `supervisor`, contraseña `1234`; perfil Administrador, usuario `jefe`, contraseña `1234`.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

La última orden requiere dispositivo o emulador conectado y autorizado por ADB. Si el proyecto está bajo OneDrive, el build se redirige a `%USERPROFILE%/.gradle/aquachile-build/<clave>/app`; el APK queda en `outputs/apk/debug/app-debug.apk` dentro de ese directorio.

La evidencia de pruebas y el guion de demostración se mantienen en [Validación del MVP](docs/VALIDACION_MVP.md). Las tareas implementadas y pendientes se registran en [Planificación](docs/PLANIFICACION_MVP.md); el [roadmap](docs/ROADMAP_MVP.md) conserva la correspondencia con las rúbricas.

### Límites del alcance actual

La aplicación funciona localmente y admite el flujo sin conexión. **No hay backend definido, autenticación remota ni sincronización entre dispositivos.** Jefatura revisa las faenas cambiando de cuenta en el mismo dispositivo. Las cuentas, catálogos y reglas de salud son de demostración académica; requieren validación del cliente antes de un uso operativo.

Los archivos capturados se conservan en almacenamiento privado. Quitar una referencia del checklist no elimina inmediatamente el archivo, para evitar borrar una evidencia ya guardada antes de confirmar la edición. La limpieza de archivos huérfanos queda pendiente.

El formulario de cierre es una propuesta funcional para el MVP; sus campos deben ser revisados con el cliente. La entrega AVA, la validación de formularios y los ensayos individuales son tareas del equipo.

## Estructura del Proyecto

```
docs/
└── evidencias/
    └── clase-01/
        ├── Evidencia_Clase_01_MVP_Equipo_Codigines.docx
        └── Actividad_Clase_2_Flujo_UML_Diseno_Interfaces.docx
```

## Documentación

| Documento | Descripción |
|-----------|-------------|
| Evidencia_Clase_01_MVP_Equipo_Codigines.docx | Documento de evidencia del Producto Mínimo Viable (MVP) propuesto por el equipo |
| Actividad_Clase_2_Flujo_UML_Diseno_Interfaces.docx | Actividad de modelamiento UML y diseño de interfaces del sistema |

## Equipo de Desarrollo ("Los Codigines")

| Nombre | Rol |
|--------|-----|
| Alan Vidal | Líder Scrum |
| Álvaro Morales | Backend |
| Isidora Diaz | Backend |
| Francisco Molina | Frontend |

---

*Documentación generada en base a la definición de requerimientos del cliente AquaChile y arquitectura de diseño UI/UX.*

*Última actualización: 24 de octubre de 2026*
