package com.duoc.aquaflow.model

data class CentroMaritimo(
    val id: String, val nombre: String, val sector: String, val latitud: Double, val longitud: Double
)

// Puntos marítimos de referencia para el catálogo ficticio de la aplicación.
val CENTROS_MARITIMOS = listOf(
    CentroMaritimo("CENTRO-1", "Centro Demo Aurora - Jaula 04", "Golfo de Ancud", -42.05, -73.05),
    CentroMaritimo("CENTRO-2", "Centro Demo Nébula", "Golfo Corcovado", -43.13, -73.35),
    CentroMaritimo("CENTRO-3", "Centro Demo Fiordo Esmeralda", "Golfo de Penas", -47.65, -75.12),
    CentroMaritimo("CENTRO-4", "Centro Demo Bahía Horizonte", "Mar de Chiloé occidental", -42.55, -74.38),
    CentroMaritimo("CENTRO-5", "Centro Demo Estero Boreal", "Boca del Guafo", -43.65, -74.05),
    CentroMaritimo("CENTRO-6", "Centro Demo Faro Sur", "Golfo de Arauco", -37.00, -73.62)
)

data class CondicionesMaritimas(
    val latitudModelo: Double,
    val longitudModelo: Double,
    val consultadaEn: Long,
    val horaModelo: Long,
    val alturaOla: Double?,
    val periodoOla: Double?,
    val direccionOla: Double?,
    val temperaturaSuperficial: Double?
)
