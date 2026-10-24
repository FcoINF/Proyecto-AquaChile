package com.duoc.aquaflow.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaNaranja
import com.duoc.aquaflow.viewmodel.SesionViewModel

@Composable
fun LoginScreen(sesion: SesionViewModel, onIngresoExitoso: () -> Unit) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }
    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatusBarMode = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (previousStatusBarMode != null) {
                controller?.isAppearanceLightStatusBars = previousStatusBarMode
            }
        }
    }

    var nombre by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaAzul)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AquaChile",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "AQUAFLOW  ·  OPERACIONES SEGURAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFC6D3DC)
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = AquaNaranja,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "CONTROL DE FAENAS",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AquaAzul
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bienvenido", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Ingresa para continuar con tu jornada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            error = false
                        },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clave,
                        onValueChange = {
                            clave = it
                            error = false
                        },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error) {
                        Text(
                            text = "Usuario o contraseña incorrectos",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (sesion.ingresar(nombre, clave)) {
                                onIngresoExitoso()
                            } else {
                                error = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Iniciar sesión")
                    }

                    Text(
                        "Demo: supervisor / jefe · Clave: 1234",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Seguridad y bienestar en cada inmersión",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC6D3DC)
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AquaFlowTheme {
        LoginScreen(
            sesion = SesionViewModel(),
            onIngresoExitoso = {}
        )
    }
}
