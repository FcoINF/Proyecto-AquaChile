# Estado de implementación y ramas

Este registro versionado sirve como planificación local trazable. Las responsabilidades humanas propuestas en el roadmap siguen sujetas al reparto del equipo; no se atribuyen estos commits a los integrantes como evidencia de participación individual.

## Ramas encadenadas

Base de trabajo: `feature/resumen-faena` en `6960fcf`. Las ramas `codex/` incluyen sus predecesoras porque las funcionalidades comparten persistencia y navegación. No se modificaron `main`, `develop` ni las ramas originales del equipo.

| Orden | Rama | Cambio principal / commit |
|---|---|---|
| 1 | `codex/roadmap-mvp` | Roadmap y rúbricas — `bbba613` |
| 2 | `codex/persistencia-faena` | Guardado confirmado, recuperación, identidad local y migraciones — `f3bd59b` |
| 3 | `codex/evidencias-fotograficas` | Cámara a archivo, referencias persistidas y checklist MVVM — `94ce282` |
| 4 | `codex/cierre-faena` | Registro posterior/cierre y Room v4 — `7dcbeb5` |
| 5 | `codex/validacion-mvp` | Restauración de cámara `0cbd0c4`, aislamiento de salud `3967dcf`, exclusión de caché local `434f985`, documentación y resultados |

`codex/validacion-mvp` contiene la versión consolidada. Para revisar por etapas, comparar cada rama con la anterior; para integrar todo, comparar la última con la base acordada por el equipo. No son cinco cambios independientes para aplicar en cualquier orden. La publicación de ramas no implica aprobación ni merge.

## Tareas

| ID | Funcionalidad | Estado | Evidencia / siguiente acción |
|---|---|---|---|
| MVP-01 | Guardar antes de navegar y reintentar | Implementado y probado | `GuardadoSeguroTest`, `f3bd59b` |
| MVP-02 | Reanudar expediente completo | Implementado y probado | `GuardadoSeguroTest`, `FaenaPersistenciaTest` |
| MVP-03 | Conservar datos al migrar | Implementado y probado | `MigracionesRoomTest`: 1/2/3 → 4 |
| MVP-04 | Evitar reasignación y modificación de faenas decididas | Implementado y probado | `AperturaFaenaTest`, `GuardadoSeguroTest` |
| MVP-05 | Foto real persistida y vista en detalle | Implementado; prueba de almacenamiento aprobada | `EvidenciasRoomTest`; falta ensayo de cámara física |
| MVP-06 | Fecha/hora del dispositivo visible y persistida | Implementado | Apertura, resumen, registro de cierre y pruebas de persistencia |
| MVP-07 | Estado de checklist y captura pendiente | Implementado y probado | `ChecklistViewModelTest` |
| MVP-08 | Aislar salud entre faenas | Implementado y probado en UI | `NavegacionFaenaUiTest` |
| MVP-09 | Registro posterior y cierre | Implementado y probado | `CierreFaenaTest`, `CierreFormularioUiTest`, `CicloFaenaRoomTest` |
| MVP-10 | README, roadmap y evidencia de ejecución | Actualizados | `VALIDACION_MVP.md`, JSON de resultados |
| MVP-11 | Formularios y reglas aprobados por cliente | Pendiente del equipo/cliente | Confirmar catálogos, campos y reglas; no presentar ejemplos como validación operativa |
| MVP-12 | Backend e identidad real | Pendiente de definición | El usuario confirmó que todavía no existe backend definido |
| MVP-13 | Sincronización entre dispositivos | Depende de MVP-12 | Definir servicio, cola, adjuntos, reintentos y conflictos |
| MVP-14 | Limpieza de fotos huérfanas | Pendiente | Eliminar solo archivos sin referencias confirmadas ni captura pendiente |
| MVP-15 | Evidencia colaborativa, defensa y AVA | Pendiente del equipo | Aportes reales por integrante, ensayo y entrega según calendario docente |

## Próximo hito

Revisar la rama consolidada y ejecutar el guion de demostración en un teléfono físico. Después, acordar el backend y el contrato de sincronización antes de iniciar MVP-12/MVP-13.
