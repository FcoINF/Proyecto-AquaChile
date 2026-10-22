# Plan de Implementación: Flujo Secuencial y Ordenado de Faena de Buceo en AquaFlow

Este plan detalla el orden lógico y secuencial correcto de las pantallas que componen una faena de buceo en **AquaFlow**, asegurando que la navegación sea intuitiva, que la **Apertura de Faena** utilice **checkboxes** para una gestión cómoda en terreno, y que los **Checklists DPR-24 y AST** sean visibles e interactivos.

## Orden Correcto del Proceso de Faena de Buceo (User Flow)

El flujo operativo estructurado para el Supervisor de Buceo es el siguiente:

```mermaid
graph TD
    A[1. Login / Selección de Rol] --> B[2. Dashboard Principal]
    B --> C[3. Apertura de Faena (con Checkboxes)]
    C --> D[4. Checklist DPR-24 (Equipamiento)]
    D --> E[5. Formulario AST (Seguridad y Riesgos)]
    E --> F[6. Encuesta de Salud Basal y Signos Vitales]
    F --> G[7. Autorización / Rechazo de Inmersión]
    G --> H[8. Cierre e Historial de Faenas]
```

## User Review Required

> [!IMPORTANT]
> - **Apertura de Faena**: Se rediseñará para incluir cuadrillas, equipos de superficie y condiciones iniciales mediante **checkboxes**, evitando entradas de texto excesivas en terreno.
> - **Checklists Obligatorios (DPR-24 y AST)**: Se mostrarán como pantallas intermedias obligatorias antes de permitir el paso a la encuesta de salud y autorización.

## Proposed Changes

### Pantallas y Navegación del Flujo ([app/src/main/java/com/duoc/aquaflow](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow))

#### [NEW] [LoginScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/LoginScreen.kt)
- Autenticación y selección de rol (Administrador / Supervisor de Buceo) con paleta Material 3 (`#F95716` / `#0B1723`).

#### [NEW] [DashboardScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/DashboardScreen.kt)
- Panel principal con acceso directo a iniciar una nueva faena.

#### [NEW] [AperturaFaenaScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/AperturaFaenaScreen.kt)
- Ingreso de centro de cultivo y selección de recursos/cuadrilla mediante **checkboxes ordenados**.

#### [NEW] [ChecklistDpr24Screen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/ChecklistDpr24Screen.kt)
- Verificación de equipamiento vital (compresor, rescate, aire) con checklist interactivo visible.

#### [NEW] [AstScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/AstScreen.kt)
- Análisis Seguro de Trabajo (AST) con verificación de riesgos y medidas de control por checkboxes.

#### [NEW] [HealthSurveyScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/HealthSurveyScreen.kt)
- Registro de signos vitales basales de los buzos.

#### [NEW] [AuthorisationScreen.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/ui/screens/AuthorisationScreen.kt)
- Consolidación y autorización / rechazo de la inmersión.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/W608-PCXX/StudioProjects/Proyecto-AquaChile/app/src/main/java/com/duoc/aquaflow/MainActivity.kt)
- Orquestador de navegación secuencial que conecta todas las pantallas anteriores.

## Verification Plan

### Automated Tests
- Compilación del proyecto con Gradle (`gradle_build`).

### Manual Verification
- Validación del flujo paso a paso en dispositivo/emulador:
  1. Apertura de faena con checkboxes operativos.
  2. Visualización correcta de los checklists DPR-24 y AST.
  3. Secuencia lógica hasta la autorización final.
