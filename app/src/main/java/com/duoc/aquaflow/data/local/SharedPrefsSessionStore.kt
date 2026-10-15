package com.duoc.aquaflow.data.local

import android.content.Context
import com.duoc.aquaflow.repository.SessionStore

class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs = context.applicationContext.getSharedPreferences("aquaflow_session", Context.MODE_PRIVATE)

    override fun leerUsuarioId(): Long? =
        if (prefs.contains(CLAVE_USUARIO)) prefs.getLong(CLAVE_USUARIO, -1L) else null

    override fun guardar(usuarioId: Long) {
        prefs.edit().putLong(CLAVE_USUARIO, usuarioId).apply()
    }

    override fun limpiar() {
        prefs.edit().remove(CLAVE_USUARIO).apply()
    }

    private companion object {
        const val CLAVE_USUARIO = "usuario_id"
    }
}
