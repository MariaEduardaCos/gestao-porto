package br.edu.unirv.lineup.data.repository

import android.content.Context
import br.edu.unirv.lineup.data.local.MockPortData
import br.edu.unirv.lineup.data.local.SantosPortDefaultData
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.PortCargoCatalogItem
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.remote.BackendApiClient
import br.edu.unirv.lineup.data.remote.BackendConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortRepository(
    private val context: Context,
    val config: BackendConfig,
    private val apiClient: BackendApiClient
) {
    private val gson = Gson()
    private val preferences = context.getSharedPreferences("port_mock_data", Context.MODE_PRIVATE)
    private val cargoType = object : TypeToken<List<PortCargoCatalogItem>>() {}.type
    private val tideType = object : TypeToken<List<TideTable>>() {}.type
    private var localCargos: List<PortCargoCatalogItem>
        get() = preferences.getString("cargos", null)?.let { gson.fromJson(it, cargoType) }
            ?: SantosPortDefaultData.cargos
        set(value) { preferences.edit().putString("cargos", gson.toJson(value)).apply() }
    private val localTides by lazy {
        val saved = preferences.getString("tides", null)?.let { gson.fromJson<List<TideTable>>(it, tideType) }
        (saved ?: SantosPortDefaultData.tides(context)).toMutableList()
    }

    private fun persistTides() {
        preferences.edit().putString("tides", gson.toJson(localTides)).apply()
    }

    suspend fun checkConnection(): Boolean = !config.isMockMode && apiClient.checkConnection()

    suspend fun getBerths(): List<Berth> = read(
        remote = apiClient::fetchAllBerths,
        local = { MockPortData.getBerths(context).map { berth ->
            berth.copy(schedules = berth.schedules.map { it.copy() }.toMutableList())
        } }
    )

    suspend fun getTideTables(): List<TideTable> = read(apiClient::fetchTideTables) { localTides.toList() }

    suspend fun getVessels(): List<RegisteredVessel> = read(apiClient::fetchVessels) {
        MockPortData.getVessels(context).toList()
    }

    suspend fun getCargos(): List<PortCargoCatalogItem> = read(apiClient::fetchCargos) { localCargos }

    private suspend fun <T> read(remote: suspend () -> T, local: () -> T): T =
        withContext(Dispatchers.IO) {
            if (!config.isMockMode) {
                try {
                    return@withContext remote()
                } catch (_: java.io.IOException) {
                    // Contingência de leitura quando o servidor estiver indisponível.
                }
            }
            local()
        }

    suspend fun setActiveTide(tideId: Long) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            localTides.replaceAll { it.copy(activeMonth = it.id == tideId) }
            persistTides()
        } else {
            apiClient.activateTideTable(tideId)
        }
    }

    suspend fun saveTide(table: TideTable, csv: String) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            val index = localTides.indexOfFirst { it.id == table.id }
            if (index >= 0) localTides[index] = table
            else localTides.add(table.copy(id = (localTides.maxOfOrNull { it.id } ?: 0) + 1))
            persistTides()
        } else {
            apiClient.importTide(table, csv)
        }
    }

    suspend fun deleteTide(id: Long) = withContext(Dispatchers.IO) {
        check(config.isMockMode) { "O servidor não oferece exclusão de tábuas de marés." }
        localTides.removeAll { it.id == id }
        if (localTides.none { it.activeMonth } && localTides.isNotEmpty()) {
            localTides[0] = localTides[0].copy(activeMonth = true)
        }
        persistTides()
    }

    suspend fun saveVessel(vessel: RegisteredVessel, editing: Boolean) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.saveVessel(context, vessel)
            MockPortData.persist(context)
        }
        else apiClient.saveCatalogItem("vessels", vessel.imoCode, editing, vessel)
    }

    suspend fun deleteVessel(imo: String) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.deleteVessel(context, imo)
            MockPortData.persist(context)
        }
        else apiClient.deleteCatalogItem("vessels", imo)
    }

    suspend fun saveCargo(cargo: PortCargoCatalogItem, editing: Boolean) = withContext(Dispatchers.IO) {
        if (config.isMockMode) localCargos = (localCargos.filterNot { it.code == cargo.code } + cargo).sortedBy { it.code }
        else apiClient.saveCargo(cargo, editing)
    }

    suspend fun deleteCargo(code: String) = withContext(Dispatchers.IO) {
        if (config.isMockMode) localCargos = localCargos.filterNot { it.code == code }
        else apiClient.deleteCatalogItem("cargos", code)
    }

    suspend fun addOrUpdateShip(berthId: String, schedule: ShipSchedule) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.addOrUpdateShip(berthId, schedule)
            MockPortData.persist(context)
        }
        else if (schedule.id <= 0) apiClient.createShipSchedule(berthId, schedule)
        else apiClient.updateShipSchedule(schedule.id, berthId, schedule)
    }

    suspend fun moveOrResizeShip(scheduleId: Long, startDay: Double, endDay: Double, targetBerthId: String?) =
        withContext(Dispatchers.IO) {
            if (config.isMockMode) {
            MockPortData.moveOrResizeShip(scheduleId, startDay, endDay, targetBerthId)
            MockPortData.persist(context)
        }
            else apiClient.moveOrResizeShip(scheduleId, startDay, endDay, targetBerthId)
        }

    suspend fun deleteShip(scheduleId: Long) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.deleteShip(scheduleId)
            MockPortData.persist(context)
        }
        else apiClient.deleteCatalogItem("schedules", scheduleId.toString())
    }

    suspend fun addOrUpdateBerth(berth: Berth, editing: Boolean = false) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.addOrUpdateBerth(berth)
            MockPortData.persist(context)
        }
        else apiClient.saveCatalogItem("berths", berth.id, editing, berth)
    }

    suspend fun deleteBerth(berthId: String) = withContext(Dispatchers.IO) {
        if (config.isMockMode) {
            MockPortData.deleteBerth(berthId)
            MockPortData.persist(context)
        }
        else apiClient.deleteCatalogItem("berths", berthId)
    }
}
