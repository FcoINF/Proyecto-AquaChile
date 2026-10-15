package com.duoc.aquaflow.di

import android.content.Context
import com.duoc.aquaflow.data.files.EvidenciaStorage
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.local.SharedPrefsSessionStore
import com.duoc.aquaflow.data.seed.DatabaseSeeder
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.repository.AuthRepository
import com.duoc.aquaflow.repository.CatalogoRepository
import com.duoc.aquaflow.repository.FaenaRepository
import com.duoc.aquaflow.repository.NotConfiguredSyncGateway
import com.duoc.aquaflow.repository.RoomAuthRepository
import com.duoc.aquaflow.repository.RoomCatalogoRepository
import com.duoc.aquaflow.repository.RoomFaenaRepository
import com.duoc.aquaflow.repository.RoomUsuarioRepository
import com.duoc.aquaflow.repository.SyncGateway
import com.duoc.aquaflow.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contenedor de dependencias manual. Para el tamaño del proyecto evita agregar un framework de
 * inyección; todas las dependencias se construyen aquí y se entregan a los ViewModels por constructor.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Reglas operacionales (los valores provisionales se editan en [ReglasConfig]). */
    val reglas = ReglasConfig()

    private val database = AquaFlowDatabase.build(appContext)
    private val seeder = DatabaseSeeder(database)

    val evidenciaStorage = EvidenciaStorage(appContext)

    val authRepository: AuthRepository = RoomAuthRepository(
        usuarioDao = database.usuarioDao(),
        sessionStore = SharedPrefsSessionStore(appContext),
        esperarSemillas = seeder::ensureSeeded,
        config = reglas
    )
    val usuarioRepository: UsuarioRepository = RoomUsuarioRepository(database.usuarioDao())
    val catalogoRepository: CatalogoRepository = RoomCatalogoRepository(database.centroDao(), database.buzoDao())
    val faenaRepository: FaenaRepository = RoomFaenaRepository(database, evidenciaStorage, reglas)
    val syncGateway: SyncGateway = NotConfiguredSyncGateway

    init {
        // Carga los datos iniciales en segundo plano para que los catálogos estén listos al abrir la app.
        // Incluye faenas de ejemplo cerradas/rechazada para que el historial tenga contenido demo.
        appScope.launch { seeder.ensureSeeded(incluirFaenasDemo = true) }
    }
}
