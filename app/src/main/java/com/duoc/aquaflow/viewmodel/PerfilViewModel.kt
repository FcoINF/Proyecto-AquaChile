package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.repository.AuthRepository
import com.duoc.aquaflow.repository.FaenaRepository
import com.duoc.aquaflow.repository.SyncGateway
import com.duoc.aquaflow.repository.SyncResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val auth: AuthRepository,
    faenas: FaenaRepository,
    private val sync: SyncGateway
) : ViewModel() {

    val pendientesSync: StateFlow<Int> = faenas.observarPendientesSync()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun sincronizar() {
        viewModelScope.launch {
            _mensaje.update {
                when (val r = sync.sincronizar()) {
                    SyncResultado.SinServidor ->
                        "No hay un servidor configurado. Los registros permanecen guardados en este dispositivo."
                    is SyncResultado.Exito -> "Se sincronizaron ${r.enviados} registros."
                    is SyncResultado.Error -> r.mensaje
                }
            }
        }
    }

    fun consumirMensaje() = _mensaje.update { null }

    fun cerrarSesion() {
        viewModelScope.launch { auth.logout() }
    }
}
