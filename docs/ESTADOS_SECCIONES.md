# Estados de secciones y bloqueo de expedientes

Fecha: 25 de octubre de 2026. Rama de entrega: `feature/persistencia`. Base de integración: `origin/develop` en `868808c`.

## Base vigente

La propuesta se integra sobre el contenido actual de `develop`, incluyendo Room v5 y su migración 4→5, apertura con condiciones sin responder, presentación de meteorología, nuevas interfaces de checklist y salud, botones de borrador, edición por ID desde historial y `DetalleFaenaScreen`. Se conserva el historial Git mediante una integración de ramas, sin reescribir commits publicados.

Las reglas de salud y AST son exactamente las de esta base. En particular, AST permite pasos vacíos y exige las preguntas críticas confirmadas; esta entrega no restaura la lógica anterior ni agrega nuevas reglas para salud crítica o respuestas.

## Guardado y recuperación

Checklist, AST y salud guardan `estado` en su JSON de Room. Los botones Guardar borrador de los tres formularios y sus operaciones en `FaenaViewModel` marcan BORRADOR aunque todos los campos estén llenos. Guardar y continuar / Guardar / Guardar AST producen registros completos si superan las validaciones vigentes. Los borradores no permiten doble envío mientras se guardan ni edición de una faena ya decidida.

Al recuperar por supervisor o por ID se conserva ese estado. Una marca COMPLETA no basta si los datos son inválidos; un estado desconocido se trata como borrador. Un JSON ilegible o una sección ausente queda sin registrar. Se conservan categorías, badges y fotografías del checklist de develop.

La apertura mantiene sus columnas de Room v5 y obtiene su estado de esos datos y de sus condiciones iniciales. Cargarla por ID no la confirma si sigue incompleta. Esta propuesta no agrega una versión SQL ni altera las migraciones de develop.

## Compatibilidad con registros anteriores

Develop también guardaba borradores sin un indicador de estado. Por ello, los JSON sin `estado` se recuperan como BORRADOR: tener campos llenos no demuestra que el usuario confirmó la sección. El supervisor debe abrir y confirmar esas secciones antes de una nueva decisión. No se eliminan sus datos ni se modifica una decisión o cierre histórico.

## Bloqueo y presentación

- Resumen y nuevo detalle de historial muestran Sin registrar, Borrador o Completa. Completitud y resultado de seguridad son independientes.
- Un expediente incompleto no permite Autorizar ni Rechazar. Ambos botones quedan desactivados también durante el guardado; el ViewModel y el repositorio vuelven a comprobar la condición.
- En persistencia, lectura, validación y guardado de la decisión se ejecutan en una sola transacción Room, protegiendo frente a ediciones concurrentes.
- No se muestran semáforos de secciones incompletas como si fueran definitivos. Las métricas de salud del detalle requieren una encuesta completa.
- Guardar la misma apertura conserva las secciones; modificarla las invalida como en develop. Las decisiones no se sobrescriben.

## Validación

Ver [Validación del MVP](VALIDACION_MVP.md) para los resultados de esta integración. Se comprueban borradores con datos completos, transición a completa, recuperación por ID y reapertura de Room, compatibilidad conservadora de JSON anterior, categorías del checklist, migraciones hasta v5 y bloqueo de decisiones en ambas pantallas. Se ejecutan también las pruebas de apertura, salud y navegación de develop.
