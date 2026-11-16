package br.edu.unirv.lineup.data.remote

import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BackendApiClient(private val config: BackendConfig) {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${config.baseUrl}/api/port/tides/active")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchAllBerths(): List<Berth> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/berths")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Erro HTTP ${response.code}: ${response.message}")
            val body = response.body?.string() ?: throw Exception("Resposta vazia")
            val type = object : TypeToken<List<BerthResponseDto>>() {}.type
            val dtoList: List<BerthResponseDto> = gson.fromJson(body, type)

            dtoList.map { dto ->
                val cat = try {
                    CargoCategory.valueOf(dto.category)
                } catch (e: Exception) {
                    CargoCategory.SOLID_VEG
                }
                Berth(
                    id = dto.id,
                    name = dto.name,
                    sectorName = dto.sectorName ?: "Santos APS",
                    maxDraft = dto.maxDraft ?: "14.0m",
                    category = cat,
                    displayOrder = dto.displayOrder,
                    schedules = dto.schedules.map { sDto ->
                        ShipSchedule(
                            id = sDto.id,
                            shipName = sDto.shipName,
                            unloadingProduct = sDto.unloadingProduct ?: "Nenhum",
                            loadingProduct = sDto.loadingProduct ?: "Nenhum",
                            tonnage = sDto.tonnage,
                            startDay = sDto.startDay,
                            endDay = sDto.endDay,
                            stripeColorHex = sDto.stripeColorHex ?: ShipSchedule.colorForStatus(sDto.status),
                            status = sDto.status ?: "Programado"
                        )
                    }.toMutableList()
                )
            }
        }
    }

    suspend fun fetchTideTables(): List<TideTable> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/tides")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Erro HTTP ${response.code}")
            val body = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<List<TideSummaryDto>>() {}.type
            val list: List<TideSummaryDto> = gson.fromJson(body, type)
            list.map {
                TideTable(
                    id = it.id,
                    monthKey = it.monthKey,
                    monthLabel = it.monthLabel,
                    sourceFileName = it.sourceFileName ?: "",
                    activeMonth = it.activeMonth,
                    totalMeasurements = it.totalMeasurements,
                    lowTideDays = it.lowTideDays ?: emptySet()
                )
            }
        }
    }

    suspend fun activateTideTable(tideId: Long): Boolean = withContext(Dispatchers.IO) {
        val emptyBody = "".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/tides/$tideId/activate")
            .post(emptyBody)
            .build()
        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    suspend fun createShipSchedule(berthId: String, schedule: ShipSchedule): ShipSchedule = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "berthId" to berthId,
            "shipName" to schedule.shipName,
            "unloadingProduct" to schedule.unloadingProduct,
            "loadingProduct" to schedule.loadingProduct,
            "tonnage" to schedule.tonnage,
            "startDay" to schedule.startDay,
            "endDay" to schedule.endDay,
            "stripeColorHex" to schedule.stripeColorHex,
            "status" to schedule.status
        )
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/schedules")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: response.message
                throw Exception("Falha ao salvar atracação (${response.code}): $errorMsg")
            }
            val resBody = response.body?.string() ?: throw Exception("Resposta vazia")
            gson.fromJson(resBody, ShipSchedule::class.java)
        }
    }

    suspend fun updateShipSchedule(scheduleId: Long, berthId: String, schedule: ShipSchedule): ShipSchedule = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "berthId" to berthId,
            "shipName" to schedule.shipName,
            "unloadingProduct" to schedule.unloadingProduct,
            "loadingProduct" to schedule.loadingProduct,
            "tonnage" to schedule.tonnage,
            "startDay" to schedule.startDay,
            "endDay" to schedule.endDay,
            "stripeColorHex" to schedule.stripeColorHex,
            "status" to schedule.status
        )
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/schedules/$scheduleId")
            .put(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: response.message
                throw Exception("Falha ao atualizar atracação (${response.code}): $errorMsg")
            }
            val resBody = response.body?.string() ?: throw Exception("Resposta vazia")
            gson.fromJson(resBody, ShipSchedule::class.java)
        }
    }

    suspend fun moveOrResizeShip(scheduleId: Long, startDay: Double, endDay: Double, targetBerthId: String? = null): ShipSchedule = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "startDay" to startDay,
            "endDay" to endDay
        )
        if (targetBerthId != null) payload["targetBerthId"] = targetBerthId

        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/schedules/$scheduleId/window")
            .patch(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: response.message
                throw Exception("Falha ao mover/redimensionar (${response.code}): $errorMsg")
            }
            val resBody = response.body?.string() ?: throw Exception("Resposta vazia")
            gson.fromJson(resBody, ShipSchedule::class.java)
        }
    }

    suspend fun deleteShipSchedule(scheduleId: Long): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.baseUrl}/api/port/schedules/$scheduleId")
            .delete()
            .build()
        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    private data class BerthResponseDto(
        val id: String,
        val name: String,
        val sectorName: String?,
        val maxDraft: String?,
        val category: String,
        val displayOrder: Int,
        val schedules: List<ScheduleResponseDto> = emptyList()
    )

    private data class ScheduleResponseDto(
        val id: Long,
        val shipName: String,
        val unloadingProduct: String?,
        val loadingProduct: String?,
        val tonnage: Int,
        val startDay: Double,
        val endDay: Double,
        val stripeColorHex: String?,
        val status: String?
    )

    private data class TideSummaryDto(
        val id: Long,
        val monthKey: String,
        val monthLabel: String,
        val sourceFileName: String?,
        val activeMonth: Boolean,
        val totalMeasurements: Int,
        val lowTideDays: Set<Int>?
    )
}
