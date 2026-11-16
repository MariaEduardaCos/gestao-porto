package br.edu.unirv.lineup.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.remote.BackendConfig
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import br.edu.unirv.lineup.ui.theme.ScheduledOrange
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf(viewModel.config.baseUrl) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título e Introdução
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = OceanPrimary)
                    Text("Configurações do Gêmeo Digital", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ajuste dinâmico de comunicação cliente-servidor (RF002), alternância entre dados mocados locais e servidor REST Spring Boot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Alternância do Modo Mocado (Requisito da 1ª Entrega)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Dados Mocados (Offline)", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (viewModel.isMockMode)
                                "Ativo — O aplicativo utiliza os 60 berços e 153 navios locais, permitindo testes completos e drag-and-drop sem precisar do backend."
                            else
                                "Inativo — O aplicativo tenta consumir as APIs REST do LineupBackend.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = viewModel.isMockMode,
                        onCheckedChange = { viewModel.toggleMockMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.loadAllData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Recarregar / Resetar Dados Locais", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        // Configuração de URL REST e Atalhos (RF002)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Endereço do LineupBackend (REST):", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        viewModel.setBackendUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("URL Base da API") }
                )

                Text("Atalhos rápidos:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            urlInput = BackendConfig.URL_EMULATOR
                            viewModel.setBackendUrl(BackendConfig.URL_EMULATOR)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("10.0.2.2 (Emulador)", fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = BackendConfig.URL_LOCALHOST
                            viewModel.setBackendUrl(BackendConfig.URL_LOCALHOST)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("localhost (USB)", fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = BackendConfig.URL_WIFI_SAMPLE
                            viewModel.setBackendUrl(BackendConfig.URL_WIFI_SAMPLE)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Wi-Fi Local", fontSize = 10.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch {
                            val ok = viewModel.repository.checkConnection()
                            isTesting = false
                            testResult = if (ok) {
                                true to "Conexão estabelecida com sucesso com o LineupBackend! (HTTP 200)"
                            } else {
                                false to "Não foi possível conectar a '$urlInput'. Verifique se o backend está executando."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Testar Conexão REST", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }

                // Resultado do Teste de Conexão
                testResult?.let { (success, message) ->
                    val color = if (success) HighTideGreen else Color(0xFFEF4444)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = color
                            )
                            Text(
                                text = message,
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
