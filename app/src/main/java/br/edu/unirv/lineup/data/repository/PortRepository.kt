package br.edu.unirv.lineup.data.repository

import android.content.Context
import br.edu.unirv.lineup.data.local.MockPortData
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.remote.BackendApiClient
import br.edu.unirv.lineup.data.remote.BackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortRepository(
    private val context: Context,
    val config: BackendConfig,
    private val apiClient: BackendApiClient
) {

    suspend fun checkConnection(): Boolean {
        if (config.isMockMode) return false
        return apiClient.checkConnection()
    }

    suspend fun getBerths(): List<Berth> = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            return@withContext localBerths()
        }
        try {
            apiClient.fetchAllBerths()
        } catch (e: Exception) {
            // Contingência offline (RF001 / RN025)
            localBerths()
        }
    }

    private fun localBerths(): List<Berth> = MockPortData.getBerths(context).map { berth ->
        berth.copy(schedules = berth.schedules.map { it.copy() }.toMutableList())
    }

    suspend fun getTideTables(): List<TideTable> = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            return@withContext MockPortData.getTideTables()
        }
        try {
            val remote = apiClient.fetchTideTables()
            if (remote.isNotEmpty()) remote else MockPortData.getTideTables()
        } catch (e: Exception) {
            MockPortData.getTideTables()
        }
    }

    suspend fun setActiveTide(tideId: Long): Boolean = withContext(Dispatchers.IO) {
        MockPortData.setActiveTideTable(tideId)
        if (!config.isMockMode) {
            try {
                apiClient.activateTideTable(tideId)
            } catch (e: Exception) {
                // Mantém local
            }
        }
        true
    }

    suspend fun getVessels(): List<RegisteredVessel> = withContext(Dispatchers.IO) {
        MockPortData.getVessels(context)
    }

    suspend fun addOrUpdateShip(berthId: String, schedule: ShipSchedule): Berth = withContext(Dispatchers.IO) {
        // Sempre atualiza no Mock local para feedback imediato e consistência offline
        val updatedBerth = MockPortData.addOrUpdateShip(berthId, schedule)

        if (!config.isMockMode) {
            try {
                if (schedule.id <= 0) {
                    apiClient.createShipSchedule(berthId, schedule)
                } else {
                    apiClient.updateShipSchedule(schedule.id, berthId, schedule)
                }
            } catch (e: Exception) {
                // Erro propagado para o ViewModel se for regra de negócio ou conexão
                throw e
            }
        }
        updatedBerth
    }

    suspend fun moveOrResizeShip(
        scheduleId: Long,
        startDay: Double,
        endDay: Double,
        targetBerthId: String? = null
    ): ShipSchedule = withContext(Dispatchers.IO) {
        val updated = MockPortData.moveOrResizeShip(scheduleId, startDay, endDay, targetBerthId)

        if (!config.isMockMode) {
            try {
                apiClient.moveOrResizeShip(scheduleId, startDay, endDay, targetBerthId)
            } catch (e: Exception) {
                throw e
            }
        }
        updated
    }

    suspend fun deleteShip(scheduleId: Long): Boolean = withContext(Dispatchers.IO) {
        val localDeleted = MockPortData.deleteShip(scheduleId)
        if (!config.isMockMode) {
            try {
                apiClient.deleteShipSchedule(scheduleId)
            } catch (e: Exception) {
                // Erro ignorado se já removido localmente
            }
        }
        localDeleted
    }

    suspend fun addOrUpdateBerth(berth: Berth) = withContext(Dispatchers.IO) {
        MockPortData.addOrUpdateBerth(berth)
    }

    suspend fun deleteBerth(berthId: String): Boolean = withContext(Dispatchers.IO) {
        MockPortData.deleteBerth(berthId)
    }
}
