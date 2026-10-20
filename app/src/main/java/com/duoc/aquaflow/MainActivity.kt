package com.duoc.aquaflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duoc.aquaflow.navigation.AquaFlowNavHost
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaFlowTheme { AquaFlowNavHost() }
        }
    }
}