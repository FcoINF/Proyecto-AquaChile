# Primera propuesta: estados de secciones y bloqueo de expedientes

Fecha: 25 de octubre de 2026. Rama: `feature/persistencia`.

## Alcance y revisión

La rama remota existía y coincidía con el avance local validado (`4befc6d`). El ZIP adjunto contiene una versión anterior sin estados explícitos de sección. Se conservó como referencia, sin reemplazar el repositorio ni el avance de cierre y fotografías.

El cambio pertenece a persistencia: checklist, AST y salud incorporan `estado` en su JSON guardado en Room. Al recuperarlos, se conserva un borrador explícito incluso si sus campos están llenos. Un registro marcado completo solo se considera completo si pasa las validaciones existentes. Un estado desconocido se trata como borrador. Una sección ausente o cuyo JSON no puede leerse queda sin registrar.

La apertura conserva sus columnas actuales; su estado se obtiene de esos datos y de las condiciones iniciales persistidas. No se duplican esos valores en una nueva columna. Room sigue en versión 4: no cambia el esquema SQL ni requiere una migración destructiva. Los JSON anteriores, sin `estado`, se clasifican al leer usando las mismas validaciones de los formularios.

## Comportamiento

- El resumen y el detalle muestran Sin registrar, Borrador o Completa por sección. Completitud y resultado de seguridad son conceptos independientes.
- Falta completar el expediente si la apertura es inválida, hay equipos pendientes, AST o salud no pasan sus validaciones actuales, alguna sección está ausente o marcada como borrador, o el checklist corresponde a otra faena.
- Autorizar y Rechazar permanecen desactivados cuando el expediente está incompleto o hay un guardado en curso. El ViewModel también rechaza la solicitud.
- Ambas operaciones del repositorio para guardar decisiones verifican el expediente persistido dentro de una transacción Room. La comprobación y el guardado no dejan un intervalo en el que otra edición pueda invalidar los antecedentes.
- Guardar la misma apertura conserva las secciones y sus estados; modificarla invalida sus secciones como antes. Una decisión existente no se sobrescribe.

## Límites de esta propuesta

Se mantienen los formularios y sus validaciones actuales: no se agregan respuestas de salud de tres estados, no se modifica el cálculo AST, no se cambian umbrales de salud ni la autorización con motivo de resultados que requieren revisión. Una salud crítica puede estar completa; conserva el tratamiento anterior. Los cambios específicos de respuestas y salud crítica quedan para los siguientes pasos.

Esta entrega agrega representación, persistencia, recuperación y bloqueo de estados; no incorpora nuevos botones para guardar formularios parciales de AST o salud ni persistencia automática de cada pulsación. Las decisiones históricas se conservan para auditoría.

## Validación

Los resultados ejecutados se registran en [Validación del MVP](VALIDACION_MVP.md). Incluyen pruebas de estados, compatibilidad de JSON, bloqueo de ambas vías del repositorio, reapertura de Room, transición de borrador a completa y botones de decisión en Compose, además de las regresiones existentes de salud, AST, migraciones, navegación y cierre.
