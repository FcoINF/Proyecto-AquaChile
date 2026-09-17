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
- **Modo Offline Activo**: Guardado local en el dispositivo (SQLite Room DB) previendo la falta de conexión en mar abierto, con capacidad de sincronización posterior
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

- **Frontend y Diseño de Interfaces**: Material Design 3, Jetpack Compose
- **Prototipado e IA**: Google Stitch, Google AI Studio, Figma
- **Almacenamiento Local**: SQLite / Room DB (arquitectura que soporta trabajo desconectado de la red)

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

*Última actualización: Septiembre 2026*