package com.duoc.aquaflow.viewmodel

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.remote.PronosticoApi
import com.duoc.aquaflow.data.remote.PronosticoUbicacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class PronosticoUi(
    val cargando: Boolean = false,
    val datos: PronosticoUbicacion? = null,
    val mensaje: String? = null
)

class PronosticoUbicacionViewModel(
    private val contexto: Context,
    private val api: PronosticoApi
) : ViewModel() {
    private val _ui = MutableStateFlow(PronosticoUi())
    val ui = _ui.asStateFlow()

    fun permisoDenegado() {
        _ui.value = PronosticoUi(mensaje = "Permite la ubicación para ver el pronóstico local.")
    }

    fun consultar() {
        if (_ui.value.cargando) return
        _ui.value = PronosticoUi(cargando = true)
        viewModelScope.launch {
            _ui.value = try {
                val ubicacion = obtenerUbicacion()
                if (ubicacion == null) {
                    PronosticoUi(mensaje = "No se obtuvo la ubicación. Activa la ubicación del teléfono y reintenta.")
                } else {
                    val datos = api.consultar(ubicacion.latitude, ubicacion.longitude)
                    if (datos.current == null && datos.daily == null) PronosticoUi(mensaje = "Open-Meteo no devolvió un pronóstico.")
                    else PronosticoUi(datos = datos)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SecurityException) {
                PronosticoUi(mensaje = "Permite la ubicación para ver el pronóstico local.")
            } catch (_: Exception) {
                PronosticoUi(mensaje = "No se pudo consultar Open-Meteo. Revisa tu conexión y reintenta.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Location? {
        val gestor = contexto.getSystemService(LocationManager::class.java) ?: return null
        val precisa = ContextCompat.checkSelfPermission(contexto, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val proveedores = if (precisa) listOf("fused", LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf("fused", LocationManager.NETWORK_PROVIDER)
        for (proveedor in proveedores) {
            if (proveedor !in gestor.allProviders || !gestor.isProviderEnabled(proveedor)) continue
            val ubicacion = withTimeoutOrNull(6_000) {
                suspendCancellableCoroutine<Location?> { continuacion ->
                    val cancelacion = CancellationSignal()
                    continuacion.invokeOnCancellation { cancelacion.cancel() }
                    LocationManagerCompat.getCurrentLocation(gestor, proveedor, cancelacion, ContextCompat.getMainExecutor(contexto)) {
                        if (continuacion.isActive) continuacion.resume(it)
                    }
                }
            }
            if (ubicacion != null) return ubicacion
        }
        return null
    }
}
