# Presentación de condiciones climáticas

## Intención

Presentar la pantalla de apertura de faena con valores climáticos precargados y una apariencia profesional para una demostración del proyecto.

## Objetivos

- Mantener los valores de muestra actuales: viento de 12 nudos SO, límite operativo menor a 20 kts y marea favorable con reposo de marea alta a las 09:45.
- Retirar la insignia “DEMO” y el aviso rojo.
- Sustituirlos por una aclaración breve y neutra: “Valores referenciales de presentación; verificar en terreno antes de operar.”
- Destacar en negrita la etiqueta “LÍMITE OPERATIVO”.
- Mantener independiente y obligatoria la confirmación de verificación meteorológica real en terreno.

## Restricciones y no objetivos

- No hay una fuente de mediciones meteorológicas reales conectada; los valores seguirán siendo fijos y de presentación.
- No presentar los valores referenciales como mediciones verificadas ni permitir que reemplacen la confirmación real.
- No cambiar las reglas de autorización de apertura ni integrar telemetría.

## Enfoques considerados

1. **Retirar todas las indicaciones sobre los valores de muestra:** apariencia más limpia, pero haría indistinguibles los valores fijos de mediciones verificadas y podría inducir decisiones operativas equivocadas.
2. **Reemplazar la advertencia roja y el sello “DEMO” por una nota neutral y discreta:** conserva una presentación limpia y profesional, a la vez que informa que los valores son referenciales. **Propuesta recomendada** por equilibrar la presentación solicitada y la claridad sobre la falta de mediciones reales.
3. **Conectar una fuente meteorológica real:** ofrecería mediciones actuales, pero requiere una fuente, credenciales y cambios de integración que no existen en el alcance actual.

## Decisión e implementación

Se implementó el enfoque 2: la interfaz conserva los valores referenciales fijos, presenta una aclaración neutral y mantiene la verificación real obligatoria. La etiqueta “LÍMITE OPERATIVO” aparece en negrita.
