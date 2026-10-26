# Propuesta 1: borradores y secciones completas

Base: develop `868808c`. Rama de trabajo: `feature/persistencia`.

## Comportamiento

- Checklist, AST y salud guardan su contenido y una marca de confirmación independiente.
- Guardar borrador deja la sección pendiente, aunque todos sus campos tengan valores.
- Guardar para continuar valida el contenido y confirma la sección después de persistir correctamente.
- El resumen y el detalle muestran Sin registrar, Borrador o Completa. La apertura se evalúa con sus reglas de campos y condiciones.
- Autorizar y rechazar requieren apertura válida, las tres secciones confirmadas y contenido válido según las reglas actuales. La comprobación se repite en una transacción de Room sobre el expediente guardado.
- Reabrir una sección y guardarla como borrador retira su confirmación. Cambiar la apertura invalida las secciones y sus marcas; guardarla sin cambios las conserva.

## Actualización de Room

Room v6 agrega `checklistCompleto`, `astCompleto` y `saludCompleta`, sin borrar datos.
Las faenas antiguas sin decisión necesitan confirmar nuevamente sus secciones: las versiones anteriores no distinguían borrador de formulario terminado.
Los expedientes ya decididos conservan su decisión y sus datos; las marcas de las secciones existentes se mantienen como confirmadas para su consulta histórica.

## Verificación

- `EstadoSeccionesTest`: borradores válidos, contenido inválido, confirmación, invalidación, recuperación y fallo de escritura.
- `BorradoresRoomTest`: reapertura real en disco y migración de v5 con registros pendientes y decididos.
- `MigracionesRoomTest`: apertura de bases de versiones 1 a 5 en v6.
- `ResumenBorradorUiTest`: estados visibles y botones de decisión deshabilitados.
- Regresión del ciclo de faena, fotografías y pruebas unitarias existentes.

## Alcance de este cambio

Las siguientes propuestas se ejecutan por separado: conservar todas las respuestas de salud, bloquear salud crítica, mantener respuestas pendientes del AST, notas obligatorias del checklist, roles y envío formal a revisión. Este cambio no redefine esas reglas ni cambia el ciclo de estados de la faena.
