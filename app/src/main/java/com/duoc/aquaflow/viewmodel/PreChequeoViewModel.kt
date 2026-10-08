package com.duoc.aquaflow.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.PreChequeo

class PreChequeoViewModel : ViewModel() {
    private val repository = PreChequeoRepository()

    var preChequeoState by mutableStateOf(PreChequeo())
        private set

    fun actualizarCentro(centro: String) {
        preChequeoState = preChequeoState.copy(centroCultivo = centro)
    }

}