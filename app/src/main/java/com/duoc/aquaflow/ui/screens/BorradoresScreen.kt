package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.theme.AquaAzul

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BorradoresScreen(
    faenas: List<FaenaGuardada>,
    cargando: Boolean,
    error: String?,
    onReintentar: () -> Unit,
    onEditarBorrador: (String) -> Unit,
    onVolver: () -> Unit
) {
    val borradores = faenas.filter { it.decision == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borradores por completar") },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Faenas guardadas", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Retoma la preparación donde la dejaste.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                cargando -> item {
                    AquaSectionCard(title = "Cargando borradores") {
                        CircularProgressIndicator()
                    }
                }
                error != null -> item {
                    AquaSectionCard(title = "No se pudieron cargar los borradores") {
                        Text(error)
                        OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
                    }
                }
                borradores.isEmpty() -> item {
                    AquaSectionCard(title = "Sin borradores pendientes") {
                        EmptyState(
                            titulo = "Todo al día",
                            texto = "Las faenas que guardes para continuar aparecerán aquí."
                        )
                    }
                }
                else -> items(borradores, key = { it.resumen.apertura.id }) { faena ->
                    val apertura = faena.resumen.apertura
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                apertura.centroCultivo.ifBlank { "Centro por completar" },
                                style = MaterialTheme.typography.titleMedium,
                                color = AquaAzul
                            )
                            Text("Código · ${apertura.id.take(8).uppercase()}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Buzo · ${apertura.buzoAsignado.ifBlank { "Por completar" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${apertura.fecha} · ${apertura.hora}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onEditarBorrador(apertura.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continuar borrador")
                            }
                        }
                    }
                }
            }
        }
    }
}
