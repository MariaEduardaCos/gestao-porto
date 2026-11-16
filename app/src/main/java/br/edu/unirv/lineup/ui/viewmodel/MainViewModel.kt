package br.edu.unirv.lineup.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.LineupKpiMetrics
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.remote.BackendApiClient
import br.edu.unirv.lineup.data.remote.BackendConfig
import br.edu.unirv.lineup.data.repository.PortRepository
import br.edu.unirv.lineup.ui.navigation.Screen
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val config = BackendConfig(application)
    private val apiClient = BackendApiClient(config)
    val repository = PortRepository(application, config, apiClient)

    // Estado Geral da UI
    var currentScreen by mutableStateOf<Screen>(Screen.Lineup)
    var isOnline by mutableStateOf(false)
    var isMockMode by mutableStateOf(config.isMockMode)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Dados Carregados
    var allBerths by mutableStateOf<List<Berth>>(emptyList())
        private set
    private var isSavingSchedule = false
    var tideTables = mutableStateListOf<TideTable>()
    var activeTideTable by mutableStateOf<TideTable?>(null)
    var vessels = mutableStateListOf<RegisteredVessel>()

    // Filtros e Paginação (RF003)
    var selectedCategories = mutableStateListOf<CargoCategory>()
    var searchQuery by mutableStateOf("")
    var currentPage by mutableIntStateOf(1)
    val pageSize = 5 // 5 por página (60 berços = 12 páginas, conforme Tabela 11 e Figura 7)

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Checa status de conexão
                isOnline = repository.checkConnection()
                isMockMode = config.isMockMode

                // Carrega tábuas de marés
                val tides = repository.getTideTables()
                tideTables.clear()
                tideTables.addAll(tides)
                activeTideTable = tides.firstOrNull { it.activeMonth } ?: tides.firstOrNull()

                // Carrega catálogo de navios
                val vList = repository.getVessels()
                vessels.clear()
                vessels.addAll(vList)

                // Carrega berços e suas programações
                refreshBerths()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // Filtros e Busca
    val filteredBerths: List<Berth>
        get() {
            val query = searchQuery.trim().lowercase()
            return allBerths.filter { berth ->
                val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(berth.category)
                val matchesSearch = query.isEmpty() ||
                        berth.id.lowercase().contains(query) ||
                        berth.name.lowercase().contains(query) ||
                        berth.sectorName.lowercase().contains(query) ||
                        berth.category.shortName.lowercase().contains(query) ||
                        berth.schedules.any { s ->
                            s.shipName.lowercase().contains(query) ||
                            s.loadingProduct.lowercase().contains(query) ||
                            s.unloadingProduct.lowercase().contains(query) ||
                            s.status.lowercase().contains(query)
                        }
                matchesCategory && matchesSearch
            }
        }

    val totalPages: Int
        get() = max(1, ceil(filteredBerths.size.toDouble() / pageSize).toInt())

    val pageBerths: List<Berth>
        get() {
            val safePage = currentPage.coerceIn(1, totalPages)
            val fromIndex = (safePage - 1) * pageSize
            val toIndex = min(fromIndex + pageSize, filteredBerths.size)
            return if (fromIndex < filteredBerths.size) {
                filteredBerths.subList(fromIndex, toIndex)
            } else {
                emptyList()
            }
        }

    // Cálculo dinâmico dos KPIs (KpiRow RF003)
    val metrics: LineupKpiMetrics
        get() {
            val visible = filteredBerths
            var totalShips = 0
            var totalTonnage = 0L
            var totalOccupiedDays = 0.0

            for (b in visible) {
                for (s in b.schedules) {
                    totalShips++
                    totalTonnage += s.tonnage
                    totalOccupiedDays += max(0.0, s.durationDays)
                }
            }

            val maxPossibleDays = max(1, visible.size) * 30.0
            val occupancyPercent = if (visible.isEmpty()) 0.0 else min(100.0, (totalOccupiedDays / maxPossibleDays) * 100.0)

            return LineupKpiMetrics(
                totalBerths = allBerths.size,
                visibleBerths = visible.size,
                totalScheduledShips = totalShips,
                totalTonnage = totalTonnage,
                averageOccupancyPercent = (occupancyPercent * 10.0).roundToInt() / 10.0,
                activeTideMonthKey = activeTideTable?.monthKey ?: "Novembro / 2026 (30 Dias)",
                lowTideDays = activeTideTable?.lowTideDays ?: emptySet()
            )
        }

    fun toggleCategoryFilter(cat: CargoCategory) {
        if (selectedCategories.contains(cat)) {
            selectedCategories.remove(cat)
        } else {
            selectedCategories.add(cat)
        }
        currentPage = 1
    }

    fun clearCategoryFilters() {
        selectedCategories.clear()
        currentPage = 1
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        currentPage = 1
    }

    fun goToPage(page: Int) {
        currentPage = page.coerceIn(1, totalPages)
    }

    fun nextPage() {
        if (currentPage < totalPages) currentPage++
    }

    fun previousPage() {
        if (currentPage > 1) currentPage--
    }

    fun switchTideTable(tideId: Long) {
        viewModelScope.launch {
            repository.setActiveTide(tideId)
            tideTables.forEach { it.activeMonth = (it.id == tideId) }
            activeTideTable = tideTables.firstOrNull { it.id == tideId }
            successMessage = "Tábua de marés alterada para ${activeTideTable?.monthLabel}"
        }
    }

    // Validação de Regras de Negócio (RN001 e RN002)
    fun validateMooring(
        berthId: String,
        startDay: Double,
        endDay: Double,
        ignoreScheduleId: Long? = null
    ): String? {
        if (startDay < 1.0 || endDay > 30.0 || endDay <= startDay) {
            return "Janela inválida: A atracação deve estar entre os dias 1 e 30, e o fim (dia %.1f) deve ser maior que o início (dia %.1f).".format(endDay, startDay)
        }

        val berth = allBerths.firstOrNull { it.id == berthId }
            ?: return "Berço $berthId não encontrado."

        // RN001: Anti-sobreposição
        val conflict = berth.findConflictingShip(startDay, endDay, ignoreScheduleId)
        if (conflict != null) {
            return "RN001 — Conflito no berço %s: O navio '%s' já está ocupando o período de %.1f a %.1f.".format(
                berth.name, conflict.shipName, conflict.startDay, conflict.endDay
            )
        }

        // RN002: Bloqueio hidrodinâmico em dias de maré baixa
        val activeTide = activeTideTable
        if (activeTide != null) {
            val lowDay = activeTide.findFirstLowTideDayInRange(startDay, endDay)
            if (lowDay != null) {
                return "RN002 — Bloqueio de Maré Baixa: No Dia %02d a maré está baixa na tábua '%s'. Proibido atracar ou permanecer nesta data.".format(
                    lowDay, activeTide.monthLabel
                )
            }
        }

        return null
    }

    private suspend fun refreshBerths() {
        allBerths = repository.getBerths()
        currentPage = currentPage.coerceIn(1, totalPages)
    }

    fun createLocalBerth(berth: Berth, onSuccess: () -> Unit) {
        if (!isMockMode || isLoading) return
        if (allBerths.any { it.id.equals(berth.id, ignoreCase = true) }) {
            errorMessage = "Já existe um berço com o código ${berth.id}."
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val lastOrder = allBerths.maxOfOrNull { it.displayOrder } ?: 0
                repository.addOrUpdateBerth(berth.copy(displayOrder = lastOrder + 1))
                refreshBerths()
                selectedCategories.clear()
                searchQuery = berth.id
                currentPage = 1
                successMessage = "Berço ${berth.id} cadastrado."
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Falha ao cadastrar berço: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun addOrUpdateShip(berthId: String, schedule: ShipSchedule, onSuccess: () -> Unit) {
        if (isLoading || isSavingSchedule) return
        val validationError = validateMooring(berthId, schedule.startDay, schedule.endDay, if (schedule.id > 0) schedule.id else null)
        if (validationError != null) {
            errorMessage = validationError
            return
        }

        viewModelScope.launch {
            isLoading = true
            try {
                repository.addOrUpdateShip(berthId, schedule)
                refreshBerths()
                successMessage = "Navio '${schedule.shipName}' salvo com sucesso!"
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Falha ao salvar atracação: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun moveOrResizeShip(
        scheduleId: Long,
        newStart: Double,
        newEnd: Double,
        targetBerthId: String? = null
    ): Boolean {
        if (isLoading || isSavingSchedule) return false
        // Encontra o berço atual
        val currentBerth = allBerths.firstOrNull { b -> b.schedules.any { it.id == scheduleId } }
            ?: return false
        val berthId = targetBerthId ?: currentBerth.id

        val validationError = validateMooring(berthId, newStart, newEnd, scheduleId)
        if (validationError != null) {
            errorMessage = validationError
            return false
        }

        isSavingSchedule = true
        viewModelScope.launch {
            try {
                repository.moveOrResizeShip(scheduleId, newStart, newEnd, targetBerthId)
                // Recarrega berths
                refreshBerths()
                successMessage = "Janela atualizada: Dias %.1f a %.1f".format(newStart, newEnd)
            } catch (e: Exception) {
                errorMessage = "Erro ao mover navio: ${e.localizedMessage}"
            } finally {
                isSavingSchedule = false
            }
        }
        return true
    }

    fun deleteShip(scheduleId: Long, onSuccess: () -> Unit) {
        if (isLoading || isSavingSchedule) return
        viewModelScope.launch {
            isLoading = true
            try {
                repository.deleteShip(scheduleId)
                refreshBerths()
                successMessage = "Atracação excluída com sucesso!"
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Erro ao excluir: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // Configurações do Servidor REST (RF002)
    fun setBackendUrl(url: String) {
        config.baseUrl = url
        viewModelScope.launch {
            isOnline = repository.checkConnection()
        }
    }

    fun toggleMockMode(isMock: Boolean) {
        config.isMockMode = isMock
        this.isMockMode = isMock
        loadAllData()
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
