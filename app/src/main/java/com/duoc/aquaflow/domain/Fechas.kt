package com.duoc.aquaflow.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formato y utilidades de fecha en zona local del dispositivo. */
object Fechas {
    private val locale = Locale("es", "CL")
    private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)

    fun fecha(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        formatoFecha.format(Instant.ofEpochMilli(millis).atZone(zona))

    fun fechaHora(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        formatoFechaHora.format(Instant.ofEpochMilli(millis).atZone(zona))

    fun fecha(fecha: LocalDate): String = formatoFecha.format(fecha)

    fun aLocalDate(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun inicioDelDia(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
        aLocalDate(millis, zona).atStartOfDay(zona).toInstant().toEpochMilli()
}
