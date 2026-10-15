# Supuestos y valores provisionales

Documento para validar con AquaChile. Cada punto indica **dónde se cambia** en el código.

## 1. Valores provisionales (no oficiales)

| Valor | Por defecto | Dónde |
|-------|-------------|-------|
| Presión sistólica de referencia | 90-139 mmHg | `ReferenciasSignosVitales` en `domain/config/ReglasConfig.kt` |
| Presión diastólica de referencia | 60-89 mmHg | ídem |
| Saturación mínima de referencia | 95 % | ídem |
| Temperatura de referencia | 35,5-37,5 °C | ídem |
| ¿Signos fuera de referencia bloquean la autorización? | No (solo avisan) | `bloquearAutorizacionPorSignos` |
| Profundidad máxima planificada | 30 m (maqueta AST: "Límite 30m") | `profundidadMaxPlanificadaM` |
| ¿Se exige foto en equipos con falla? | No | `exigirFotoEnFallas` |
| Largo mínimo del motivo de rechazo/cancelación | 10 caracteres | `largoMinimoMotivo` |
| Intentos de PIN antes de bloquear / duración | 5 / 30 s | `maxIntentosLogin`, `bloqueoLoginSegundos` |

Los **límites de digitación** (`LimitesDigitacion`) solo detectan errores de tipeo (p. ej. SpO2 de 150); no son criterios clínicos.

**La aplicación no decide la aptitud médica.** Solo registra, avisa y bloquea la autorización cuando el propio buzo declara una condición desfavorable (declaración DS 752) o el equipo no cumple; la decisión final siempre es de la jefatura.

## 2. Contenido de catálogos a confirmar (`domain/catalog/Catalogos.kt`)

- Los 4 equipos DPR-24 y sus descripciones salen de la maqueta; podría haber más equipos en el R-DPR-24 real.
- Preguntas de gating AST y de declaración DS 752: textos de la maqueta.
- Riesgos críticos 2 y 3 (`AST_R_SUMINISTRO`, `AST_R_AMBIENTE`) y las **tipologías de tarea** son propuestas: la maqueta solo muestra el primer riesgo ("0/3 verificados").

## 3. Supuestos de proceso

1. **Quién autoriza:** la jefatura (Jefe de Centro / Prevención). El supervisor firma y envía a revisión; no puede autorizar su propia faena.
2. **Doble firma con PIN:** el supervisor firma al enviar y la jefatura confirma con su PIN al autorizar (la maqueta muestra "Punto de control seguro").
3. **Cancelación:** se agregó el estado `CANCELADA` para que una faena mal abierta no deje al buzo bloqueado.
4. **Un buzo, una faena activa:** no se puede abrir otra mientras exista una en pre-chequeo, pendiente o autorizada.
5. **Certificación del buzo:** no se asigna si vence antes de la fecha de la faena (la maqueta muestra la vigencia DIRECTEMAR).
6. **Datos de inmersión:** se ingresan manualmente (no hay integración con el instrumento).
7. **Sincronización:** el MVP es local; el servidor queda para una fase posterior (`SyncGateway`).
8. **Un borrador puede estar incompleto**, pero no se puede firmar/enviar hasta que los tres formularios estén completos.
9. **Bloqueos de seguridad** que impiden autorizar (la faena sí puede rechazarse): equipo "No cumple", respuesta "NO" en el gating del AST, declaración de salud desfavorable.
