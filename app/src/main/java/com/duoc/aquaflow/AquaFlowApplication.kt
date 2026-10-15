package com.duoc.aquaflow

import android.app.Application
import com.duoc.aquaflow.di.AppContainer

class AquaFlowApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
