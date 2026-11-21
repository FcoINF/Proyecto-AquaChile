package com.duoc.aquaflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.duoc.aquaflow.navigation.AquaFlowNavHost
import com.duoc.aquaflow.ui.components.deseleccionarAlTocarODeslizar
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaFlowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .deseleccionarAlTocarODeslizar()
                ) {
                    AquaFlowNavHost(backendUrl = BuildConfig.API_URL)
                }
            }
        }
    }
}