# Diseño: meteorología en Apertura de faena

- **Objetivo:** destacar arriba de la pantalla de Apertura de faena una tarjeta de condiciones en pontón basada en la imagen compartida, para que la persona identifique fácilmente el estado de viento y marea.
- **Restricción:** el proyecto no contiene integración de telemetría, API ni mediciones meteorológicas. No mostrar valores ficticios ni afirmar que hay datos en tiempo real.
- **Diseño:** ubicar una tarjeta meteorológica antes de los datos de faena, con encabezado, indicador de disponibilidad y paneles para viento superficial y estado de marea. En ausencia de fuente de datos, mostrar “Sin medición disponible”. Colocar junto a esa tarjeta la confirmación manual “Mar y meteorología aptas”.
- **Flujo preservado:** la confirmación manual seguirá alimentando `condicionesListas` y las reglas existentes de apertura. “Condiciones iniciales” conservará los controles de embarcación y comunicaciones.
- **Alternativas:** conectar una API/sensores (no disponible en el proyecto) o usar valores de demostración (riesgo de confundirlos con datos reales). Se elige una tarjeta fiel al diseño con estado vacío veraz, hasta que haya una fuente de mediciones.
- **Alcance no incluido:** conectar telemetría, agregar permisos de ubicación/red, persistir mediciones o cambiar reglas de apertura.
- **Validación:** prueba de interfaz para la tarjeta, los estados sin datos y la confirmación manual, más las pruebas existentes de navegación y apertura.
