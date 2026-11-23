# Evidencia de integración de la API para EA4

Spring Boot 3.5.16 con H2 en memoria, datos ficticios y solicitudes procesadas por Spring MVC con su filtro de autenticación. La comprobación se ejecutó en el mismo proceso. Queda pendiente la demostración desde Android y la captura visual en Swagger.

| Operación | HTTP |
| --- | --- |
| Cargar Swagger UI | 200 |
| Consultar la definición OpenAPI | 200 |
| Consultar sesión sin credenciales | 401 |
| Consultar sesión de supervisor | 200 |
| Consultar sesión de jefe | 200 |
| Crear apertura como supervisor | 201 |
| Reenviar apertura con el mismo ID | 200 |
| Consultar la apertura como jefatura | 200 |
| Intentar editar la apertura como jefatura | 403 |
| Publicar cierre histórico como supervisor | 204 |
| Reenviar el mismo cierre | 204 |
| Consultar faenas como jefatura | 200 |

Jefatura recibió una sola faena con el ID enviado por supervisor y las mediciones del cierre.
