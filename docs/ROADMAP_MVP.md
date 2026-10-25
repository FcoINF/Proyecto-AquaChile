# Roadmap AquaFlow: MVP y evaluaciones 2 y 3

Fecha de elaboración: 24 de octubre de 2026.

Objetivo: demostrar un proceso de pre-chequeo de buceo completo, trazable y persistente, alineado con la rúbrica DSY1105, y luego completar el alcance del MVP declarado en el proyecto.

## Actualización de ejecución

Se implementaron el guardado confirmado, la recuperación completa de secciones, protección de expedientes decididos, fotos persistentes, fecha/hora visible y cierre local con Room v4. Ver [validación](VALIDACION_MVP.md) y [estado de tareas y ramas](PLANIFICACION_MVP.md).

El usuario confirmó que todavía no hay backend definido. Identidad remota y sincronización quedan pendientes. Las tablas de diagnóstico y tiempos que siguen documentan la planificación inicial; no deben interpretarse como el estado actual de lo implementado.

## 1. Alcance y criterio de planificación

La Evaluación 2 es un encargo grupal (8% de la asignatura); la Evaluación 3 es una defensa individual del mismo avance (32%). Ambas cubren contenidos hasta la clase 11. La pauta no exige terminar el producto completo: exige un caso de uso real, ejecutable y explicable.

Se proponen cinco semanas relativas para preparar la entrega, coherentes con el tiempo asignado al encargo. No son fechas oficiales: deben ajustarse al plazo de AVA. La defensa se programa en las semanas académicas 11 y 12, con máximo de 15 minutos por estudiante.

**Entrega evaluable:** supervisor ingresa, crea una faena, completa DPR-24, salud y AST, captura evidencia, guarda sin conexión y consulta el expediente después de reiniciar. Jefatura revisa y registra autorización o rechazo con motivo. El historial permite buscar, filtrar y abrir el detalle.

**MVP completo del proyecto:** agrega sincronización posterior, acceso entre dispositivos con identidad real y registro posterior/cierre. Se mantienen como hitos explícitos después de estabilizar la entrega evaluable; no se consideran terminados por tener Room o un login de demostración.

Los formularios y las reglas operativas deben contrastarse con los requerimientos y documentos del cliente. Los valores de ejemplo del código no constituyen validación de AquaChile. La app registra y apoya la revisión de jefatura; no se propone una autorización automática como sustituto de esa revisión.

## 2. Punto de partida verificado

Diagnóstico por lectura del código local y del README. No se compiló ni se ejecutaron pruebas para elaborar este roadmap; las cifras de pruebas del README corresponden a avances anteriores.

| Área | Evidencia actual | Trabajo pendiente |
|---|---|---|
| UI y navegación | Compose, Material 3 y rutas de apertura, checklist, salud, AST, resumen e historial | Recorrer el flujo completo en dispositivo, corregir retrocesos y mensajes desactualizados |
| Estado y validación | ViewModels, StateFlow y reglas de dominio | Checklist mantiene estado en `remember`; trasladar estado y reglas al ViewModel y comprobar restauración |
| Persistencia | Room versión 3; apertura y secciones serializadas; historial y decisión conectados | Confirmar guardados antes de navegar, recuperar secciones al retomar y verificar migraciones |
| Fotografías | `TakePicturePreview()` muestra una captura | Los bitmaps viven en memoria; se guardan nombres construidos, no archivos reales. Ante error se agrega una imagen simulada |
| Segundo recurso | `FaenaViewModel` obtiene fecha/hora del dispositivo con `Date` y las incorpora a la apertura | Demostrar resultado visible y conservado. La pauta menciona fecha/hora expresamente como recurso válido |
| Roles | Dos cuentas locales de demostración y consultas filtradas por supervisor | Revisar identidad frente a supervisor editable; reforzar controles de decisión en lógica/datos |
| Sincronización | Se declara en el README | No se identificó implementación de sincronización remota en el código inspeccionado |
| Cierre | Existe el estado `CERRADA` | No se identificó un flujo funcional para registrar el cierre |
| Documentación | README, documentos de clases y commits locales | README mezcla etapas anteriores y menciona Room v2; alinear con versión 3 y comportamiento actual. No se verificó tablero ni GitHub remoto |

Hallazgos a resolver antes de la demostración:

- `Ruta.kt` navega inmediatamente después de iniciar guardados asíncronos de checklist, salud y AST. Un fallo puede quedar fuera de la pantalla donde se originó.
- `recuperarApertura()` reconstruye solo la apertura, aunque el detalle sí puede leer las secciones guardadas: consultar un expediente y reanudarlo son funciones distintas.
- La comparación de apertura en `PreChequeoRepository` no incluye los tres indicadores iniciales incorporados en v3; alinear la invalidación de secciones entre memoria y almacenamiento.
- Room configura recuperación destructiva y captura excepciones genéricas en migraciones. Verificar conservación de datos al actualizar, evitando ocultar fallos de esquema.
- El DAO devuelve cuántas filas cambió al guardar una decisión, pero el repositorio ignora ese resultado. Una operación sin cambios debe distinguirse de un guardado exitoso.

## 3. Roadmap de la entrega evaluable

Responsables propuestos según los roles del README: Alan Vidal, coordinación; Álvaro Morales e Isidora Diaz, lógica y datos; Francisco Molina, interfaz. Son una propuesta de reparto, no asignaciones confirmadas. Cada integrante debe comprender el flujo completo para su defensa.

| Hito | Periodo estimado | Prioridad | Responsable propuesto | Dependencia | Resultado verificable |
|---|---|---|---|---|---|
| H0. Acordar alcance y organizar evidencias | Semana 1, inicio | P0 | Alan + equipo | Ninguna | Caso de uso, tablero y criterios de aceptación definidos |
| H1. Estabilizar guardado y recuperación | Semana 1 y primera mitad de 2 | P0 | Álvaro + Isidora | H0 | Faena recuperable sin pérdida, con errores visibles |
| H2. Completar recursos del dispositivo | Semana 2 | P0 | Francisco + Isidora | Contrato de datos de H1 | Foto real persistente y fecha/hora demostrables |
| H3. Cerrar flujo, validaciones e historial | Semana 3 | P0 | Francisco + Álvaro | H1 y H2 | Caso de uso completo con autorización/rechazo y consultas |
| H4. Verificar y preparar entrega | Semana 4 | P0 | Equipo; Alan consolida | H3 | Versión ejecutable, pruebas y evidencias trazables |
| H5. Ensayar defensa y cerrar versión | Semana 5, antes del cierre AVA | P0 | Cada integrante | H4 | Ensayo de 15 minutos y versión identificada para entrega |

P0 = necesario para cerrar la entrega evaluable. Si hay menos tiempo, reducir alcance adicional y distribuir trabajo; conservar las pruebas de persistencia, ambos recursos y el ensayo individual.

### H0. Acordar alcance y organizar evidencias

1. Confirmar con la documentación del cliente los campos de apertura, DPR-24, AST y salud; separar los datos de demostración de los formularios validados.
2. Fijar el recorrido de evaluación: ingreso → apertura → checklist → salud → AST → resumen → revisión de jefatura → historial/detalle.
3. Crear tarjetas en Trello o equivalente con ID, responsable, dependencia, indicador, aceptación y enlace a evidencia/commit.
4. Reservar un dispositivo o emulador operativo con cámara; resolver autorización ADB antes de la última semana.
5. Usar registros ficticios identificables para la demostración y acordar qué queda para el MVP completo.

**Salida:** todos conocen el flujo que se entregará y existe una tarea rastreable por cada brecha.

### H1. Estabilizar guardado y recuperación

1. Adoptar para cada sección el patrón de apertura: estado guardando → éxito o error → navegación solo después del éxito. Bloquear envíos repetidos mientras se guarda y permitir reintentar.
2. Recuperar el expediente completo al reanudar: apertura, checklist, AST, salud y decisión; no reconstruirlo como una faena vacía.
3. Unificar reglas de edición: una faena en preparación puede corregirse; una ya decidida no debe perder o cambiar silenciosamente su decisión.
4. Incluir las condiciones iniciales en la comparación de cambios y comprobar la invalidación coherente de secciones.
5. Verificar migraciones 1→2→3 y 2→3 con registros existentes; evitar borrados como solución a errores de esquema.
6. Comprobar que el supervisor solo accede a sus registros y que la identidad de acceso no depende de un campo editable del formulario.
7. Validar permisos y resultado del guardado de decisión; informar si la faena no existe o ya fue decidida.

**Aceptación:** guardar una faena, cerrar el proceso, abrir nuevamente y consultar todos sus datos con el mismo ID. Un fallo de escritura no permite avanzar como si se hubiera guardado. Actualizar la base conserva registros anteriores.

### H2. Completar dos recursos del dispositivo

**Recurso 1: cámara.** Guardar la captura en un archivo real asociado a la faena; persistir su referencia y cargar la miniatura en checklist y detalle. Quitar la imagen simulada del flujo real y mostrar un mensaje cuando la cámara no esté disponible. Resolver cancelación y permisos/acceso necesarios para el mecanismo elegido. El estado y las referencias deben sobrevivir a recreación de pantalla y reinicio.

**Recurso 2: fecha/hora del dispositivo.** Aprovechar la implementación existente: mostrar la fecha y hora obtenidas al abrir la faena, guardarlas y recuperarlas en el detalle. Explicar su procedencia y el tratamiento de edición, si se permite. No basta con señalar un timestamp interno que el evaluador no puede observar.

No es necesario agregar GPS, galería o conectividad para alcanzar dos recursos: la pauta incluye fecha/hora. Un indicador de conexión puede incorporarse después junto con sincronización, si aporta al alcance acordado.

**Aceptación:** capturar una foto reconocible, guardarla, reiniciar y verla en el expediente correcto; crear una apertura y demostrar fecha/hora real visible y persistida. Cancelar cámara no agrega evidencia inexistente.

### H3. Cerrar el caso de uso

1. Revisar campos obligatorios, espacios vacíos, formatos y mensajes de error; diferenciar guardar borrador de continuar con datos válidos.
2. Mantener reglas en dominio/ViewModel y pantallas como representación del estado; corregir el estado local del checklist.
3. Confirmar conservación de datos al volver de una pantalla y al recuperar una faena.
4. Mostrar progreso/secciones pendientes y errores de guardado donde el usuario pueda actuar.
5. Permitir revisión por jefatura con resumen completo; verificar rechazo con motivo y restricciones de autorización según reglas acordadas.
6. Probar historial con datos reales de Room: búsqueda por datos del proyecto, filtro por estado, detalle y estados sin registros/sin coincidencias/error.
7. Corregir textos obsoletos y evitar mostrar contadores o estados de demostración como información real.

**Aceptación:** demostrar una faena autorizada y otra rechazada, localizar ambas mediante búsqueda/filtros y abrir sus datos y evidencias después de reiniciar. El supervisor no puede ejecutar decisiones reservadas a jefatura.

### H4. Verificación y entrega grupal

Ejecutar con el JDK configurado para el proyecto y registrar resultado, fecha y commit:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

La última orden necesita emulador o dispositivo conectado y autorizado. Actualizar/agregar pruebas focalizadas para fallos de guardado, recuperación completa, reglas de decisión, aislamiento por usuario y migraciones. No dar por vigentes los totales históricos del README sin ejecutar la versión a entregar.

Prueba manual obligatoria del equipo:

| Caso | Resultado esperado |
|---|---|
| Datos incompletos o inválidos | Error visible; no avanza; conserva lo ingresado |
| Guardado fallido y reintento | No comunica éxito falso ni duplica la faena |
| Uso en modo avión | Registro y consulta local funcionan |
| Reinicio de proceso | Expediente y foto siguen disponibles |
| Cámara cancelada/no disponible | Mensaje coherente; no aparece una foto ficticia |
| Buscar/filtrar/volver del detalle | Resultados correctos y filtros conservados |
| Cambio de rol y acceso por ID | Se respeta la visibilidad y capacidad de decisión |
| Actualización desde esquema anterior | No se pierden registros |

Actualizar README con instalación/ejecución, integrantes, funcionalidades reales, cuentas de demostración, limitaciones, recursos integrados y resultados de prueba. Adjuntar capturas o video breve del flujo, reporte de pruebas y enlaces a tarjetas/commits. Cada integrante debe registrar sus aportes técnicos reales; no fabricar participación.

**Aceptación:** otra persona del equipo puede abrir y ejecutar la versión siguiendo el README, y cada criterio tiene evidencia localizable.

### H5. Defensa individual y cierre de versión

Cada estudiante ensaya sobre la misma versión entregable:

| Tiempo | Actividad |
|---|---|
| 0–2 min | Ejecutar y explicar el problema VcM y el proceso elegido |
| 2–5 min | Mostrar navegación y formulario con entrada válida e inválida |
| 5–7 min | Explicar ViewModel/StateFlow y demostrar búsqueda o filtro |
| 7–10 min | Guardar/consultar, comprobar permanencia y explicar Entity → DAO → Database → Repository; ubicar aportes en Git |
| 10–12 min | Ejecutar cámara y fecha/hora, mostrar sus resultados y explicar acceso/permisos |
| 12–15 min | Hacer y comprobar una modificación pequeña solicitada |

Practicar individualmente cambios como exigir longitud mínima en un campo descriptivo, ajustar un mensaje o modificar un filtro, preservando MVVM. Cada integrante debe saber encontrar los archivos y comprobar el resultado sin asistencia. La pauta indica que no se permite IA durante la defensa y que no se requieren diapositivas; esto es una condición de esa evaluación, no una prohibición de preparar este roadmap.

Antes de cerrar AVA, identificar el commit final, preparar el código fuente y comprobar la ejecución desde esa versión. Un integrante realiza la entrega grupal y cada estudiante atiende sus indicaciones individuales. Según la pauta, no modificar el repositorio entre cierre y evaluación salvo indicación expresa del docente. Hacer los ensayos de modificación antes del cierre; durante la defensa seguir la instrucción del docente.

**Aceptación:** cada integrante completa el ensayo en 15 minutos, explica las capas reales y realiza un cambio funcional sin romper el flujo.

## 4. Trazabilidad con la rúbrica

Los identificadores tienen descripciones diferentes entre las dos evaluaciones; por eso se presentan por separado. Los porcentajes son pesos internos de cada rúbrica, no una nota estimada del proyecto.

### Evaluación 2: entrega grupal (8% de la asignatura)

| Indicador | Peso | Hitos | Evidencia para aspirar al nivel de 100% |
|---|---:|---|---|
| IE 2.1.1 Interfaz y navegación | 15% | H3, H4 | Flujo real completo con Material 3 |
| IE 2.1.2 Formularios y validación visual | 15% | H1, H3 | Campos obligatorios, error y confirmación comprensibles |
| IE 2.2.1 Estado, ViewModel y lógica | 10% | H1, H3 | StateFlow actualiza Compose; reglas separadas de UI |
| IE 2.2.2 Listas, búsqueda, filtros y detalle | 10% | H3, H4 | Historial sobre Room conectado al expediente |
| IE 2.3.1 MVVM y Room | 15% | H1, H4 | Guardar, consultar y reiniciar conservando información |
| IE 2.3.2 GitHub, documentación y planificación | 20% | H0, H4, H5 | Repositorio, README, aportes técnicos y tareas trazables |
| IE 2.4.1 Dos recursos del dispositivo | 15% | H2, H4 | Cámara y fecha/hora ejecutadas con resultados visibles |

### Evaluación 3: defensa individual (32% de la asignatura)

| Indicador | Peso | Preparación y evidencia individual |
|---|---:|---|
| IE 2.1.1 Explica y modifica validaciones | 20% | Localizar regla, explicar error, cambiar y probar |
| IE 2.1.2 Domina UI y navegación | 7% | Recorrer pantallas y justificar relación con el problema |
| IE 2.2.1 Modifica componente o función | 22% | Cambio en vivo útil y ejecutable, manteniendo MVVM |
| IE 2.2.2 Explica StateFlow, listas y filtros | 7% | Seguir una acción desde usuario hasta estado y resultado |
| IE 2.3.1 Demuestra Room en vivo | 18% | Guardar, consultar, reiniciar y explicar capas |
| IE 2.3.2 Explica MVVM y colaboración | 7% | Relacionar arquitectura, commits propios y planificación |
| IE 2.4.1 Explica recursos nativos y seguridad | 7% | Explicar contratos/API, acceso y resultado de ambos recursos |
| IE 2.4.2 Ejecuta recursos nativos en UI | 12% | Ejecutar ambos y mostrar valor dentro del flujo |

La modificación de validaciones y de componentes suma 42% de la defensa. Reservar tiempo de práctica individual es parte del roadmap, no una actividad opcional al terminar de programar.

## 5. Hitos para completar el MVP declarado

Estos hitos forman parte del producto propuesto en el README, aunque no son requisitos explícitos de las evaluaciones 2 y 3. Su calendario debe acordarse después de medir el trabajo de H1–H4 y conocer la fecha de la EFT.

| Hito | Prioridad | Trabajo y dependencia | Criterio de salida |
|---|---|---|---|
| H6. Registro posterior y cierre | P1 | Tras H3: definir datos con cliente, agregar modelo/persistencia, transición autorizada → cerrada y vista de detalle | Cierre con datos y responsable conservados tras reiniciar; no cerrar una faena rechazada |
| H7. Identidad real y contrato remoto | P1 | Tras H4 y definición con cliente: usuarios, roles, servicio/base remota y reglas de acceso; acordar acceso sin conexión | Dos cuentas reales con permisos verificables; contrato de datos y política offline definidos |
| H8. Sincronización y fotos entre dispositivos | P1 | Tras H7: cola local, IDs estables, reintentos, adjuntos, estados pendiente/sincronizado/error y resolución de conflictos | Registrar offline, reconectar y consultar desde otro dispositivo sin duplicados ni decisiones sobrescritas |
| H9. Validación integral del MVP | P1 | Tras H6 y H8: piloto con escenarios acordados, revisión de formularios y correcciones | Supervisor registra; jefatura recibe/revisa; cierre e historial completos; aceptación documentada |

P1 = necesario para declarar completo el MVP original, pero posterior al avance académico priorizado. Analítica avanzada, reportes complejos y mejoras visuales no esenciales quedan como P2, después de estos hitos.

Para H6–H9 se propone mantener coordinación en Alan, interfaz en Francisco y distribuir datos, identidad y sincronización entre Álvaro e Isidora, con revisión cruzada del equipo.

## 6. Lista de cierre

### Entrega evaluable lista

- [ ] Proyecto ejecutable desde el commit a entregar.
- [ ] Caso de uso completo, con datos válidos e inválidos.
- [ ] ViewModel/StateFlow y reglas separados de la UI.
- [ ] Faena y foto persistentes después de reiniciar.
- [ ] Cámara y fecha/hora demostradas en el proceso real.
- [ ] Historial, búsqueda, filtro y detalle con datos propios.
- [ ] Migraciones y errores de guardado verificados.
- [ ] README, tablero y participación técnica trazables.
- [ ] Cada integrante ensayó explicación y modificación en vivo.
- [ ] Entrega y versión final identificadas según AVA.

### MVP original completo

- [ ] Registro posterior y cierre persistentes.
- [ ] Identidad y permisos reales.
- [ ] Sincronización posterior, incluidas evidencias fotográficas.
- [ ] Flujo entre dispositivos validado sin duplicados ni pérdida.
- [ ] Formularios y comportamiento aceptados por el cliente.

## Fuentes y límites

- `C:/Users/Carlos/Downloads/Rubricas_Evaluaciones_2_y_3_DSY1105_VCM.pdf`: páginas 1–3 para alcance, condiciones y rúbrica de E2; páginas 5–7 para E3 y defensa.
- `README.md`: alcance declarado del MVP, contexto del problema y roles del equipo.
- Código local: `navigation/Ruta.kt`, `viewmodel/FaenaViewModel.kt`, `viewmodel/SesionViewModel.kt`, `viewmodel/DetalleAperturaViewModel.kt`, `ui/screens/ChecklistScreen.kt`, `data/local/AquaFlowDatabase.kt`, `data/local/AperturaEntity.kt`, `data/local/PreChequeoDao.kt` y `data/repository/PreChequeoRepository.kt`, bajo `app/src/main/java/com/duoc/aquaflow/`.
- La lectura del código permite identificar implementación y riesgos, pero no certifica funcionamiento ni cumplimiento evaluativo. La aceptación final depende de ejecutar los escenarios y demostrar las evidencias.
