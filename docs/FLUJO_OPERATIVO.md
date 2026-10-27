# Flujo operativo completo

Implementación del 26 de octubre de 2026 en `feature/persistencia`, conservando `develop` (`868808c`) y la integración de estados de sección.

## Recorrido

1. El supervisor abre una faena, selecciona centro y buzo y confirma las condiciones iniciales.
2. Completa DPR-24 de equipos y adjunta fotografías con la captura existente. Se conserva el criterio actual de fotografías opcionales.
3. Registra la encuesta de salud inicial y completa el AST con las reglas vigentes de develop.
4. Completa un checklist independiente de tareas: agrega descripciones concretas y confirma su preparación. Todas las tareas deben tener descripción y estar verificadas; debe existir al menos una. Puede guardar un borrador para continuar después.
5. El resumen queda disponible para jefatura. El supervisor no puede decidir ni iniciar mientras no exista autorización. Jefatura no puede decidir un expediente incompleto, incluyendo el nuevo checklist de tareas.
6. En el detalle del historial, el supervisor propietario pulsa **Iniciar faena**. Se registra su identidad y fecha/hora del dispositivo. La faena pasa de Autorizada a En ejecución. La operación es transaccional y no se admite dos veces.
7. Al finalizar, el supervisor propietario registra duración, observaciones, revisión de salida y una segunda encuesta de salud. Debe confirmar el cierre. No se permite cerrar antes de iniciar, sin salud de salida válida ni sobrescribir un cierre.
8. El historial conserva las faenas propias del supervisor y permite combinar búsqueda por centro o buzo, fecha exacta `dd/MM/aaaa` y estado. Limpiar filtros restablece todos los criterios.

Las reglas de valores y clasificación de salud y las reglas AST permanecen intactas. La encuesta de salida usa la misma lista actual de preguntas y validación de signos. La salud inicial no se reemplaza por los datos posteriores. La segunda encuesta queda vinculada a la misma faena y buzo, con responsable y hora del cierre.

## Persistencia

Room v6 agrega `tareasJson` e `inicioJson` mediante migración 5→6. Las migraciones anteriores se conservan. La salud de salida se guarda dentro de `cierreJson`, separada de `encuestaJson` (salud inicial). Reabrir la base conserva tareas, inicio, ambas encuestas, decisión y cierre.

Los expedientes y cierres anteriores se mantienen. No se inventa un inicio ni una encuesta posterior para cierres históricos. Un expediente antiguo sin tareas sigue incompleto para nuevas operaciones. Si ya fue decidido, se conserva inmutable para auditoría y se debe registrar una nueva faena con el flujo completo; no se altera una autorización pasada para agregar antecedentes posteriores.

## Alcance local

Se conserva la arquitectura local: supervisor y jefe cambian de cuenta en el mismo dispositivo. No se agregó backend, sincronización entre teléfonos ni notificaciones remotas. Los catálogos y reglas continúan siendo los del proyecto académico.

## Pruebas

La evidencia de ejecución se registra en [Validación del MVP](VALIDACION_MVP.md). Incluye validación de tareas y restauración del borrador, filtro combinado por fecha, bloqueo por rol/propiedad/estado, cierre con salud separada, inicio concurrente, reapertura de Room, migraciones 1–5 a v6 y pruebas de pantalla de tareas, inicio y cierre.
