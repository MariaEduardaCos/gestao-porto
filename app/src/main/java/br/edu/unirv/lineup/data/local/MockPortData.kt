package br.edu.unirv.lineup.data.local

import android.content.Context
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MockPortData {

    private val gson = Gson()
    private var inMemoryBerths: MutableList<Berth>? = null
    private var inMemoryVessels: MutableList<RegisteredVessel>? = null
    private var inMemoryTides: MutableList<TideTable>? = null
    private var nextScheduleId: Long = 1000L

    fun getTideTables(): List<TideTable> {
        if (inMemoryTides == null) {
            inMemoryTides = mutableListOf(
                TideTable(
                    id = 1L,
                    monthKey = "Novembro / 2026 (30 Dias)",
                    monthLabel = "Novembro / 2026",
                    sourceFileName = "/tides/mare_novembro_2026.csv",
                    activeMonth = true,
                    totalMeasurements = 1440,
                    lowTideDays = setOf(11, 12, 15, 16, 23, 24, 30)
                ),
                TideTable(
                    id = 2L,
                    monthKey = "Setembro / 2026 (30 Dias)",
                    monthLabel = "Setembro / 2026",
                    sourceFileName = "/tides/mare_setembro_2026.csv",
                    activeMonth = false,
                    totalMeasurements = 1440,
                    lowTideDays = setOf(5, 6, 13, 14, 21, 22, 29)
                ),
                TideTable(
                    id = 3L,
                    monthKey = "Junho / 2026 (30 Dias)",
                    monthLabel = "Junho / 2026",
                    sourceFileName = "/tides/mare_junho_2026.csv",
                    activeMonth = false,
                    totalMeasurements = 1440,
                    lowTideDays = setOf(4, 5, 12, 13, 19, 20, 27, 28)
                )
            )
        }
        return inMemoryTides!!
    }

    fun getActiveTideTable(): TideTable {
        return getTideTables().firstOrNull { it.activeMonth } ?: getTideTables().first()
    }

    fun setActiveTideTable(id: Long) {
        val list = inMemoryTides ?: getTideTables().toMutableList()
        list.forEach { it.activeMonth = (it.id == id) }
        inMemoryTides = list
    }

    fun getBerths(context: Context? = null): List<Berth> {
        if (inMemoryBerths != null) {
            return inMemoryBerths!!
        }

        if (context != null) {
            try {
                val jsonString = context.assets.open("berths.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<BerthJsonDto>>() {}.type
                val dtoList: List<BerthJsonDto> = gson.fromJson(jsonString, type)

                val berths = dtoList.map { dto ->
                    val cat = try {
                        CargoCategory.valueOf(dto.category)
                    } catch (e: Exception) {
                        CargoCategory.SOLID_VEG
                    }
                    val schedules = dto.schedules.map { sDto ->
                        ShipSchedule(
                            id = sDto.id,
                            shipName = sDto.shipName,
                            unloadingProduct = sDto.unloadingProduct,
                            loadingProduct = sDto.loadingProduct,
                            tonnage = sDto.tonnage,
                            startDay = sDto.startDay,
                            endDay = sDto.endDay,
                            stripeColorHex = if (sDto.stripeColorHex.isNotBlank()) sDto.stripeColorHex else ShipSchedule.colorForStatus(sDto.status),
                            status = sDto.status
                        )
                    }.toMutableList()

                    Berth(
                        id = dto.id,
                        name = dto.name,
                        sectorName = dto.sectorName,
                        maxDraft = dto.maxDraft,
                        category = cat,
                        displayOrder = dto.displayOrder,
                        schedules = schedules
                    )
                }.toMutableList()

                inMemoryBerths = berths
                return berths
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback programático caso assets não estejam acessíveis
        val fallback = generateFallbackBerths()
        inMemoryBerths = fallback
        return fallback
    }

    fun getVessels(context: Context? = null): List<RegisteredVessel> {
        if (inMemoryVessels != null) return inMemoryVessels!!
        if (context != null) {
            try {
                val jsonString = context.assets.open("vessels.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<RegisteredVessel>>() {}.type
                val list: List<RegisteredVessel> = gson.fromJson(jsonString, type)
                inMemoryVessels = list.toMutableList()
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    fun addOrUpdateShip(berthId: String, schedule: ShipSchedule): Berth {
        val berths = inMemoryBerths ?: throw IllegalStateException("Berths not initialized")
        val berth = berths.firstOrNull { it.id == berthId }
            ?: throw IllegalArgumentException("Berço $berthId não encontrado")
        val previousBerth = berths.firstOrNull { candidate ->
            candidate.schedules.any { it.id == schedule.id }
        }

        val scheduleToSave = if (schedule.id <= 0) {
            schedule.copy(id = nextScheduleId++)
        } else {
            schedule
        }

        if (previousBerth != null && previousBerth.id != berth.id) {
            previousBerth.schedules.removeAll { it.id == scheduleToSave.id }
        }
        val existingIdx = berth.schedules.indexOfFirst { it.id == scheduleToSave.id }
        if (existingIdx >= 0) {
            berth.schedules[existingIdx] = scheduleToSave
        } else {
            berth.schedules.add(scheduleToSave)
        }
        berth.schedules.sortBy { it.startDay }
        return berth
    }

    fun moveOrResizeShip(scheduleId: Long, startDay: Double, endDay: Double, targetBerthId: String? = null): ShipSchedule {
        val berths = inMemoryBerths ?: throw IllegalStateException("Berths not initialized")

        var sourceBerth: Berth? = null
        var schedule: ShipSchedule? = null

        for (b in berths) {
            val found = b.schedules.firstOrNull { it.id == scheduleId }
            if (found != null) {
                sourceBerth = b
                schedule = found
                break
            }
        }

        if (schedule == null || sourceBerth == null) {
            throw IllegalArgumentException("Atracação #$scheduleId não encontrada")
        }

        val targetBerth = if (targetBerthId != null && targetBerthId != sourceBerth.id) {
            berths.firstOrNull { it.id == targetBerthId } ?: sourceBerth
        } else {
            sourceBerth
        }

        if (targetBerth.id != sourceBerth.id) {
            sourceBerth.schedules.remove(schedule)
            targetBerth.schedules.add(schedule)
        }

        schedule.startDay = startDay
        schedule.endDay = endDay
        targetBerth.schedules.sortBy { it.startDay }

        return schedule
    }

    fun deleteShip(scheduleId: Long): Boolean {
        val berths = inMemoryBerths ?: return false
        for (b in berths) {
            val removed = b.schedules.removeIf { it.id == scheduleId }
            if (removed) return true
        }
        return false
    }

    fun addOrUpdateBerth(berth: Berth) {
        val list = inMemoryBerths ?: return
        val idx = list.indexOfFirst { it.id == berth.id }
        if (idx >= 0) {
            list[idx] = berth
        } else {
            list.add(berth)
        }
        list.sortBy { it.displayOrder }
    }

    fun deleteBerth(berthId: String): Boolean {
        val list = inMemoryBerths ?: return false
        return list.removeIf { it.id == berthId }
    }

    private fun generateFallbackBerths(): MutableList<Berth> {
        val list = mutableListOf<Berth>()
        for (i in 1..60) {
            val id = "B-%02d".format(i)
            val cat = CargoCategory.values()[(i - 1) % CargoCategory.values().size]
            val b = Berth(
                id = id,
                name = "Berço %02d".format(i),
                sectorName = "Margem Direita / Porto de Santos",
                maxDraft = "14.2m",
                category = cat,
                displayOrder = i,
                schedules = mutableListOf()
            )
            if (i % 2 == 0) {
                b.schedules.add(
                    ShipSchedule(
                        id = i.toLong(),
                        shipName = "MV Santos Star %d".format(i),
                        unloadingProduct = "Lastro",
                        loadingProduct = "Carga Geral",
                        tonnage = 45000,
                        startDay = 2.0,
                        endDay = 7.0,
                        stripeColorHex = "#e74c3c",
                        status = "Operando"
                    )
                )
            }
            list.add(b)
        }
        return list
    }

    private data class BerthJsonDto(
        val id: String,
        val name: String,
        val sectorName: String,
        val maxDraft: String,
        val category: String,
        val displayOrder: Int,
        val schedules: List<ScheduleJsonDto> = emptyList()
    )

    private data class ScheduleJsonDto(
        val id: Long,
        val shipName: String,
        val unloadingProduct: String,
        val loadingProduct: String,
        val tonnage: Int,
        val startDay: Double,
        val endDay: Double,
        val stripeColorHex: String,
        val status: String
    )
}
