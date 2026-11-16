package br.edu.unirv.lineup.ui.screens.lineup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewBerthDialog by remember { mutableStateOf(false) }
    var showMooringDialog by remember { mutableStateOf(false) }
    var selectedBerthForMooring by remember { mutableStateOf("") }
    var selectedScheduleToEdit by remember { mutableStateOf<ShipSchedule?>(null) }

    // Mensagens reativas de erro ou sucesso
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Barra de Busca e Ação "+ Atracar Navio"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar berço, navio, carga...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpar busca",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )

                Button(
                    onClick = {
                        selectedBerthForMooring = (viewModel.pageBerths.firstOrNull() ?: viewModel.allBerths.first()).id
                        selectedScheduleToEdit = null
                        showMooringDialog = true
                    },
                    enabled = !viewModel.isLoading && viewModel.allBerths.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Atracar Navio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (viewModel.isMockMode) {
                OutlinedButton(
                    onClick = { showNewBerthDialog = true },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) { Text("+ Novo Berço") }
            }

            // 2. Seletor de Tábua de Marés (TideSelector RF003)
            TideSelector(
                activeTide = viewModel.activeTideTable,
                allTides = viewModel.tideTables,
                onSelectTide = { viewModel.switchTideTable(it) }
            )

            // 3. Linha de Métricas KPIs (KpiRow RF003)
            KpiRow(metrics = viewModel.metrics)

            // 4. Filtros de Categoria de Carga (11 Categorias)
            CategoryFilters(
                selectedCategories = viewModel.selectedCategories,
                onToggleCategory = { viewModel.toggleCategoryFilter(it) },
                onClearAll = { viewModel.clearCategoryFilters() }
            )

            // 5. Lista Virtualizada Paginada dos Berços (LazyColumn de 5 em 5)
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OceanPrimary)
                }
            } else if (viewModel.pageBerths.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Nenhum berço encontrado para os filtros selecionados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.pageBerths, key = { it.id }) { berth ->
                        BerthCard(
                            berth = berth,
                            activeTide = viewModel.activeTideTable,
                            onShipClick = { ship ->
                                selectedBerthForMooring = berth.id
                                selectedScheduleToEdit = ship
                                showMooringDialog = true
                            },
                            onMoveOrResizeShip = { scheduleId, newStart, newEnd ->
                                viewModel.moveOrResizeShip(scheduleId, newStart, newEnd, berth.id)
                            },
                            onConflictWarning = { msg ->
                                viewModel.errorMessage = msg
                            }
                        )
                    }
                }
            }

            // 6. Barra de Paginação (PaginationBar RF003: 5 por página, 1 a 12)
            PaginationBar(
                currentPage = viewModel.currentPage,
                totalPages = viewModel.totalPages,
                totalItems = viewModel.filteredBerths.size,
                onPrevious = { viewModel.previousPage() },
                onNext = { viewModel.nextPage() },
                onGoToPage = { viewModel.goToPage(it) }
            )
        }

        if (showNewBerthDialog) {
            NewBerthDialog(
                existingBerths = viewModel.allBerths,
                isSaving = viewModel.isLoading,
                onDismiss = { showNewBerthDialog = false },
                onSave = { berth -> viewModel.createLocalBerth(berth) { showNewBerthDialog = false } }
            )
        }

        // 7. Diálogo Modal de Atracação / Edição / Exclusão (ShipMooringDialog RF004)
        if (showMooringDialog) {
            ShipMooringDialog(
                initialBerthId = selectedBerthForMooring,
                initialSchedule = selectedScheduleToEdit,
                allBerths = viewModel.allBerths,
                registeredVessels = viewModel.vessels,
                onDismiss = { showMooringDialog = false },
                onSave = { berthId, schedule ->
                    viewModel.addOrUpdateShip(berthId, schedule) {
                        showMooringDialog = false
                    }
                },
                onDelete = { scheduleId ->
                    viewModel.deleteShip(scheduleId) {
                        showMooringDialog = false
                    }
                },
                validationWarning = { bId, sDay, eDay, ignoreId ->
                    viewModel.validateMooring(bId, sDay, eDay, ignoreId)
                }
            )
        }
    }
}

@Composable
private fun BerthCard(
    berth: Berth,
    activeTide: br.edu.unirv.lineup.data.model.TideTable?,
    onShipClick: (ShipSchedule) -> Unit,
    onMoveOrResizeShip: (scheduleId: Long, newStart: Double, newEnd: Double) -> Boolean,
    onConflictWarning: (String) -> Unit
) {
    val categoryBadgeColor = try {
        Color(android.graphics.Color.parseColor(berth.category.badgeColorHex))
    } catch (e: Exception) {
        OceanPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Cabeçalho do Berço
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryBadgeColor, CircleShape)
                    )
                    Text(
                        text = "${berth.id} — ${berth.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = categoryBadgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${berth.category.shortName} • Calado: ${berth.maxDraft}",
                        color = categoryBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${berth.sectorName} • ${berth.schedules.size} navios atracados/programados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Linha do tempo em Canvas com rolagem horizontal (30 dias)
            BerthTimelineCanvas(
                berth = berth,
                activeTide = activeTide,
                onShipClick = onShipClick,
                onMoveOrResize = onMoveOrResizeShip,
                onConflictWarning = onConflictWarning
            )
        }
    }
}
