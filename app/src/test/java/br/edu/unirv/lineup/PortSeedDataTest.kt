package br.edu.unirv.lineup

import br.edu.unirv.lineup.data.local.TideCsvParser
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.CargoDirection
import br.edu.unirv.lineup.data.model.PortCargoCatalogItem
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.TideTable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PortSeedDataTest {
    private val assets = File("src/main/assets")

    @Test
    fun readsThirtyDaysOfHalfHourlyTidesWithTheExpectedCriticalDays() {
        val months = mapOf(
            "novembro" to setOf(11, 12, 15, 16, 23, 24, 30),
            "setembro" to setOf(5, 6, 13, 14, 21, 22, 29),
            "junho" to setOf(4, 5, 12, 13, 19, 20, 27, 28)
        )
        months.forEach { (month, criticalDays) ->
            val csv = File(assets, "tides/mare_${month}_2026.csv").readText()
            val readings = TideCsvParser.parse(csv)
            val table = TideTable(1, month, month).withMeasurements(readings)
            assertEquals(1440, table.totalMeasurements)
            assertEquals(criticalDays, table.lowTideDays)
            assertTrue(readings.groupBy { it.dayNumber }.values.all { it.size == 48 })
            assertTrue(readings.zipWithNext().all { (first, next) -> first.fractionalDay < next.fractionalDay })
            assertThrows(IllegalArgumentException::class.java) { TideCsvParser.parse(csv.trim().substringBeforeLast('\n')) }
        }
    }

    @Test
    fun initialArrivalsRespectTheirBerthAndTheActiveNovemberTide() {
        val berths: List<Berth> = Gson().fromJson(File(assets, "berths.json").readText(),
            object : TypeToken<List<Berth>>() {}.type)
        val vessels: List<RegisteredVessel> = Gson().fromJson(File(assets, "vessels.json").readText(),
            object : TypeToken<List<RegisteredVessel>>() {}.type)
        val tide = TideTable(1, "nov", "Novembro").withMeasurements(
            TideCsvParser.parse(File(assets, "tides/mare_novembro_2026.csv").readText())
        )
        assertEquals((1..60).toList(), berths.map { it.displayOrder }.sorted())
        assertEquals(60, berths.map { it.id }.distinct().size)
        assertEquals(152, vessels.map { it.imoCode }.distinct().size)
        assertTrue(vessels.all { Regex("IMO-[0-9]{7}").matches(it.imoCode) })
        assertEquals(153, berths.flatMap { it.schedules }.map { it.id }.distinct().size)
        for (berth in berths) {
            for (ship in berth.schedules) {
                assertFalse(berth.hasOverlap(ship.startDay, ship.endDay, ship.id))
                assertNull(tide.findFirstLowTideDayInRange(ship.startDay, ship.endDay))
            }
        }
    }

    @Test
    fun calculatesMooringDurationUsingCeilingWithoutOverflow() {
        val cargo = PortCargoCatalogItem("CRG-01", "Soja", CargoCategory.SOLID_VEG, CargoDirection.LOAD, 36000)
        assertEquals(1L, cargo.durationDays(36000))
        assertEquals(2L, cargo.durationDays(36001))
        assertEquals(59653L, cargo.durationDays(Int.MAX_VALUE))
        assertNull(cargo.copy(rateTonsPerDay = null, otherRate = "2800 mov/dia").durationDays(36000))
    }
}
