# Posicionamiento y jerarquía de botones

Revisión de `feature/persistencia`, sobre `641abd9`. Skill aplicada: `ui-aesthetics`. Alcance: composición, orden, proximidad, ancho y separación. No se cambian colores, bordes, sombras, tipografía, reglas, textos de acciones ni permisos.

## Flujo de lectura antes del ajuste

La aplicación se lee de arriba hacia abajo: navegación y título → campos y secciones → resultados/errores → acciones. El inicio ya presenta Nueva faena junto a su contexto y las tarjetas del historial mantienen Ver detalle dentro de cada faena.

- **Jerarquía, impacto medio:** DPR-24, AST y salud presentan Guardar borrador antes de la acción que completa o continúa el paso. Ambos controles ocupan todo el ancho. La primera acción encontrada permite abandonar el progreso en lugar de completarlo.
- **Orden, impacto alto:** el detalle presenta la decisión de jefatura antes de las tareas. El lector puede llegar a Autorizar/Rechazar antes de revisar todos los antecedentes.
- **Proximidad, impacto medio:** Limpiar filtros está fuera del bloque de filtros; Quitar comparte una fila ajustada con la confirmación de la tarea. En pantallas estrechas se debilita la relación entre campo, verificación y eliminación.
- **Competencia, impacto medio:** Volver al historial ocupa el mismo ancho que Iniciar faena o Cerrar faena, aunque su función es navegación secundaria.

## Tres variantes

### A. Acción principal primero

```text
Campos y comprobaciones
Resultado o error
[       GUARDAR Y CONTINUAR       ]
                  [Guardar borrador]
```

La acción que completa el paso aparece inmediatamente después de la validación y ocupa todo el ancho disponible. La alternativa conserva su altura táctil y se ajusta a su contenido, alineada al final. Separación de 8 dp entre acciones. Aplicada a DPR-24, salud, AST y tareas. En DPR-24 se mantiene el pie existente bajo la lista; en formularios desplazables las acciones continúan dentro del documento, sin superponerse al teclado o a los campos.

### B. Acciones secundarias agrupadas

```text
Formulario
[       CONTINUAR AL CHECKLIST    ]
[Guardar borrador] [Recuperar última]
```

La navegación inversa permanece en la cabecera y las alternativas se agrupan debajo de la acción principal. La apertura ya sigue esta composición y se conserva. En los demás formularios hay una sola secundaria, alineada al final bajo la primaria. Volver al historial conserva su botón, pero deja de ocupar todo el ancho y se alinea al final del contenido. No se ocultan acciones tras menús ni se agregan nuevos toques.

### C. Acciones próximas al contexto

```text
Tarea [Descripción........................]
[✓] Preparación verificada         [Quitar]
[Agregar tarea]

Fecha / Centro o buzo / Estado
                           [Limpiar filtros]

Todos los antecedentes, incluidas tareas
Motivo [..................................]
[       Autorizar       ][     Rechazar     ]
```

Cada tarea agrupa su campo, confirmación y eliminación con 4 dp internos, manteniendo separación entre tareas. La etiqueta de confirmación puede ocupar varias líneas sin desplazar Quitar fuera del ancho disponible. Agregar tarea permanece al terminar la lista. Limpiar filtros se sitúa dentro del bloque al que afecta. La decisión pasa debajo del checklist de tareas; su motivo y ambos botones permanecen juntos.

## Selección aplicada

Se combinan las tres variantes según la función de la pantalla; no se añade un selector de layouts a la aplicación. Dirección: lectura secuencial y acciones inequívocas mediante geometría. Nivel de reescritura: reorganización moderada de bloques de acciones, sin reconstruir pantallas.

Autorizar y Rechazar conservan el mismo espacio. Una decisión de seguridad no se trata como una acción comercial que deba favorecerse. Iniciar permanece junto al estado de autorización y Cerrar permanece junto a la encuesta de salida y su confirmación. No se agrega un Guardar por campo: los formularios se guardan como secciones completas o borradores, y presentar guardado individual sería engañoso.

## Comprobación

Se conserva el contenido y todos los callbacks de los controles. Los cambios de código se limitan a seis pantallas Compose. Se ejecutan las pruebas de regresión y navegación existentes; los resultados se registran en `VALIDACION_BOTONES.md`.

[Comparación visual de las tres variantes](botones-layout.html). Es un esquema de composición en escala de grises, no una captura de la aplicación ni un cambio de su paleta.
