package br.edu.unirv.lineup.ui.screens.lineup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoDirection
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.ui.theme.DangerZoneRed
import br.edu.unirv.lineup.data.model.PortCargoCatalogItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.PortSelect
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.theme.OperatingRed
import br.edu.unirv.lineup.ui.theme.ScheduledOrange
import kotlin.math.roundToInt

@Composable
fun ShipMooringDialog(
    initialBerthId: String,
    initialSchedule: ShipSchedule?,
    allBerths: List<Berth>,
    registeredVessels: List<RegisteredVessel>,
    cargos: List<PortCargoCatalogItem>,
    onDismiss: () -> Unit,
    onSave: (berthId: String, schedule: ShipSchedule) -> Unit,
    onDelete: ((scheduleId: Long) -> Unit)? = null,
    validationWarning: ((berthId: String, startDay: Double, endDay: Double, ignoreId: Long?) -> String?)? = null,
    saveError: String? = null,
    busy: Boolean = false
) {
    var selectedBerthId by remember { mutableStateOf(initialBerthId) }
    var shipName by remember { mutableStateOf(initialSchedule?.shipName ?: "") }
    var unloadingProduct by remember { mutableStateOf(initialSchedule?.unloadingProduct ?: "Nenhum (Lastro)") }
    var loadingProduct by remember { mutableStateOf(initialSchedule?.loadingProduct ?: "Soja em Grãos") }
    var tonnageText by remember { mutableStateOf((initialSchedule?.tonnage ?: 55000).toString()) }
    var startDay by remember { mutableDoubleStateOf(initialSchedule?.startDay ?: 2.0) }
    var cargoCode by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(initialSchedule?.status ?: "Programado") }

    var berthDropdownExpanded by remember { mutableStateOf(false) }
    var vesselDropdownExpanded by remember { mutableStateOf(false) }
    val tonnage = tonnageText.toIntOrNull()?.takeIf { it > 0 }

    val isEditing = initialSchedule != null && initialSchedule.id > 0
    val selectedBerth = allBerths.firstOrNull { it.id == selectedBerthId } ?: allBerths.firstOrNull()

    val ratedCargos = cargos.filter { it.rateTonsPerDay != null }
    val cargo = ratedCargos.firstOrNull { it.code == cargoCode }
        ?: ratedCargos.firstOrNull { it.name.equals(loadingProduct, true) }
        ?: ratedCargos.firstOrNull { it.name.equals(unloadingProduct, true) }
    val duration = tonnage?.let { cargo?.durationDays(it) }
    val endDay = duration?.let { startDay + it }

    // Validação reativa em tempo real (RN001 / RN002)
    val liveWarning = endDay?.let { validationWarning?.invoke(
        selectedBerthId,
        startDay,
        endDay,
        initialSchedule?.id
    ) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEditing) "Editar Atracação (${initialSchedule?.shipName})" else "Nova Atracação de Navio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Seleção de Berço
                Text("Berço Operacional:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(
                        onClick = { berthDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedBerth?.id} — ${selectedBerth?.name}",
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = berthDropdownExpanded,
                        onDismissRequest = { berthDropdownExpanded = false }
                    ) {
                        allBerths.forEach { b ->
                            DropdownMenuItem(
                                text = { Text("${b.id} — ${b.name} (${b.category.shortName})", fontSize = 12.sp) },
                                onClick = {
                                    selectedBerthId = b.id
                                    berthDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Nome do Navio / Seletor de Frota
                Text("Embarcação / Navio:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedTextField(
                        value = shipName,
                        onValueChange = { shipName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: MV Santos Star") },
                        trailingIcon = {
                            if (registeredVessels.isNotEmpty()) {
                                IconButton(onClick = { vesselDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar catálogo")
                                }
                            }
                        },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = vesselDropdownExpanded,
                        onDismissRequest = { vesselDropdownExpanded = false }
                    ) {
                        registeredVessels.take(30).forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(v.shipName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${v.imoCode} • ${v.vesselClass} • Calado: ${v.maxDraftMeters}", fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    shipName = v.shipName
                                    tonnageText = v.defaultTonnage.toString()
                                    if (v.defaultUnloadProduct.isNotBlank()) unloadingProduct = v.defaultUnloadProduct
                                    if (v.defaultLoadProduct.isNotBlank()) loadingProduct = v.defaultLoadProduct
                                    vesselDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Cargas e Tonelagem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unloadingProduct,
                        onValueChange = { unloadingProduct = it },
                        label = { Text("Descarga", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = loadingProduct,
                        onValueChange = { loadingProduct = it },
                        label = { Text("Embarque", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = tonnageText,
                    onValueChange = { tonnageText = it.filter { char -> char.isDigit() } },
                    isError = tonnage == null,
                    supportingText = { if (tonnage == null) Text("Informe uma tonelagem positiva válida.") },
                    label = { Text("Volume (Toneladas)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Intervalo de Dias (1 a 30)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Janela de Atracação (Mês):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (endDay == null) "Selecione a prancha para calcular o término."
                                    else "Dias %.1f a %.1f (%d dias)".format(startDay, endDay, duration),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }

                        Text("Dia de Início (Chegada): %.1f".format(startDay), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Slider(
                            value = startDay.toFloat(),
                            onValueChange = { newStart ->
                                startDay = (newStart * 2).roundToInt() / 2.0 // Passos de meio dia
                            },
                            valueRange = 1.0f..29.0f,
                            steps = 55
                        )

                        PortSelect("Prancha", cargo, ratedCargos,
                            { it?.let { item -> "${item.name} · ${item.rateLabel}" } ?: "Selecione" }) {
                            cargoCode = it?.code
                            if (initialSchedule == null && it != null) {
                                if (it.direction == CargoDirection.UNLOAD) unloadingProduct = it.name
                                else loadingProduct = it.name
                            }
                        }
                    }
                }

                // Status Operacional (RN005)
                Text("Status da Atracação (Cor Normatizada):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Operando", OperatingRed, "#e74c3c"),
                        Triple("Programado", ScheduledOrange, "#e67e22"),
                        Triple("Confirmado", HighTideGreen, "#2ecc71")
                    ).forEach { (label, color, hex) ->
                        val isSelected = status.equals(label, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { status = label }
                                .padding(vertical = 8.dp),
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                        .padding(end = 4.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Aviso de Validação (RN001 Anti-colisão / RN002 Maré Baixa)
                val warning = liveWarning ?: saveError
                if (warning != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DangerZoneRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerZoneRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerZoneRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = warning,
                                color = DangerZoneRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ton = tonnage ?: return@Button
                    val schedule = ShipSchedule(
                        id = initialSchedule?.id ?: 0L,
                        shipName = if (shipName.isNotBlank()) shipName.trim() else "Navio Não Identificado",
                        unloadingProduct = unloadingProduct.trim(),
                        loadingProduct = loadingProduct.trim(),
                        tonnage = ton,
                        startDay = startDay,
                        endDay = endDay ?: return@Button,
                        stripeColorHex = ShipSchedule.colorForStatus(status),
                        status = status
                    )
                    onSave(selectedBerthId, schedule)
                },
                enabled = !busy && shipName.isNotBlank() && tonnage != null && endDay != null && liveWarning == null
            ) {
                Text(if (isEditing) "Salvar Alterações" else "Confirmar Atracação")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerZoneRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Excluir", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                OutlinedButton(onClick = onDismiss, enabled = !busy) {
                    Text("Cancelar")
                }
            }
        }
    )
    if (confirmDelete && initialSchedule != null && onDelete != null) {
        ConfirmDeleteDialog(initialSchedule.shipName, busy, { confirmDelete = false }) {
            confirmDelete = false
            onDelete(initialSchedule.id)
        }
    }
}
