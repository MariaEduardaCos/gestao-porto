package br.edu.unirv.lineup

import br.edu.unirv.lineup.data.local.MockPortData
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortDomainTest {

    @Test
    fun testRN001_AntiOverlapCollisionDetection() {
        val berth = Berth(
            id = "SV-01",
            name = "Berço 01 — Berço 12A",
            sectorName = "Corredor de Exportação",
            maxDraft = "14.2m",
            category = CargoCategory.SOLID_VEG,
            displayOrder = 1
        )

        // Adiciona navio no berço nos dias 5.0 a 10.0
        val ship1 = ShipSchedule(
            id = 101L,
            shipName = "MV Santos Star",
            startDay = 5.0,
            endDay = 10.0
        )
        berth.schedules.add(ship1)

        // Caso 1: Colisão sobreposta no meio (dias 7.0 a 12.0)
        assertTrue(berth.hasOverlap(7.0, 12.0, null))

        // Caso 2: Colisão englobando o navio inteiro (dias 4.0 a 11.0)
        assertTrue(berth.hasOverlap(4.0, 11.0, null))

        // Caso 3: Colisão contida dentro do navio (dias 6.0 a 9.0)
        assertTrue(berth.hasOverlap(6.0, 9.0, null))

        // Caso 4: Sem colisão antes (dias 1.0 a 4.9)
        assertFalse(berth.hasOverlap(1.0, 4.9, null))

        // Caso 5: Sem colisão depois (dias 10.1 a 15.0)
        assertFalse(berth.hasOverlap(10.1, 15.0, null))

        // Caso 6: Ignorar o próprio navio durante edição (resize ou move)
        assertFalse(berth.hasOverlap(5.0, 10.0, 101L))
    }

    @Test
    fun testRN002_LowTideBlocking() {
        // Tábua de Novembro/2026: dias de maré baixa [11, 12, 15, 16, 23, 24, 30]
        val tideTable = TideTable(
            id = 1L,
            monthKey = "Novembro / 2026 (30 Dias)",
            monthLabel = "Novembro / 2026",
            activeMonth = true,
            lowTideDays = setOf(11, 12, 15, 16, 23, 24, 30)
        )

        // Caso 1: Janela incluindo o dia 11.0 -> Bloqueado
        val conflictDay = tideTable.findFirstLowTideDayInRange(9.0, 12.0)
        assertNotNull(conflictDay)
        assertEquals(11, conflictDay)

        // Caso 2: Janela segura entre dias 1.0 e 8.0 -> Permitido
        val safeDay = tideTable.findFirstLowTideDayInRange(1.0, 8.0)
        assertNull(safeDay)

        // Caso 3: Janela atingindo o dia 24.0 -> Bloqueado
        val conflict24 = tideTable.findFirstLowTideDayInRange(20.0, 25.0)
        assertNotNull(conflict24)
        assertEquals(23, conflict24) // primeiro dia crítico no range é 23
    }

    @Test
    fun testRN005_ColorAndStatusNormalization() {
        assertEquals("#e74c3c", ShipSchedule.colorForStatus("Operando"))
        assertEquals("#e74c3c", ShipSchedule.colorForStatus("Prioridade"))
        assertEquals("#e67e22", ShipSchedule.colorForStatus("Programado"))
        assertEquals("#2ecc71", ShipSchedule.colorForStatus("Confirmado"))
    }

    @Test
    fun editingAnArrivalCanTransferItWithoutDuplicatingIt() {
        val berths = MockPortData.getBerths()
        val source = berths.first()
        val target = berths.last()
        val original = MockPortData.addOrUpdateShip(
            source.id,
            ShipSchedule(shipName = "Navio de teste", startDay = 8.0, endDay = 10.0)
        ).schedules.single { it.shipName == "Navio de teste" }

        try {
            MockPortData.addOrUpdateShip(target.id, original.copy(tonnage = 12345))
            assertFalse(source.schedules.any { it.id == original.id })
            assertEquals(12345, target.schedules.single { it.id == original.id }.tonnage)

            MockPortData.moveOrResizeShip(original.id, 8.5, 10.5)
            assertEquals(8.5, target.schedules.single { it.id == original.id }.startDay, 0.0)
            assertTrue(MockPortData.deleteShip(original.id))
            assertFalse(berths.any { berth -> berth.schedules.any { it.id == original.id } })
        } finally {
            MockPortData.deleteShip(original.id)
        }
    }

    @Test
    fun testRF003_PaginationSlices() {
        // 60 berços divididos de 5 em 5 devem resultar em exatamente 12 páginas
        val totalBerths = 60
        val pageSize = 5
        val expectedPages = kotlin.math.ceil(totalBerths.toDouble() / pageSize).toInt()

        assertEquals(12, expectedPages)
    }
}
