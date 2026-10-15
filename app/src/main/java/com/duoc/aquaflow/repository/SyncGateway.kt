package com.duoc.aquaflow.repository

/** Resultado de intentar sincronizar los registros locales con un servidor. */
sealed interface SyncResultado {
    /** No hay servidor configurado: los datos siguen guardados solo en el dispositivo. */
    data object SinServidor : SyncResultado
    data class Exito(val enviados: Int) : SyncResultado
    data class Error(val mensaje: String) : SyncResultado
}

/**
 * Punto de extensión para una futura sincronización con un backend. El MVP funciona 100% local:
 * no existe servidor, por lo que la implementación actual informa [SyncResultado.SinServidor].
 */
interface SyncGateway {
    suspend fun sincronizar(): SyncResultado
}

object NotConfiguredSyncGateway : SyncGateway {
    override suspend fun sincronizar(): SyncResultado = SyncResultado.SinServidor
}
