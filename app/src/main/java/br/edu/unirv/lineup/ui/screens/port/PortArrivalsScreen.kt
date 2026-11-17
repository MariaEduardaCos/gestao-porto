package br.edu.unirv.lineup.ui.screens.port

import android.graphics.Color.parseColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.ui.components.*
import br.edu.unirv.lineup.ui.components.ShipMooringDialog
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel

@Composable
fun PortArrivalsScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var berthId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ShipSchedule?>(null) }
    var deleting by remember { mutableStateOf<ShipSchedule?>(null) }
    val rows = vm.allBerths.flatMap { berth -> berth.schedules.map { berth to it } }
        .filter { (berth, ship) ->
            matchesPortSearch(query, berth.name, berth.id, berth.sectorName, ship.shipName,
                ship.loadingProduct, ship.unloadingProduct, ship.status)
        }.sortedWith(compareBy<Pair<Berth, ShipSchedule>> { it.second.startDay }.thenBy { it.first.displayOrder })

    PortList(vm.activeTideTable?.let { "Atracações de ${it.monthLabel}" } ?: "Programação geral", query, { query = it }, "Buscar navio, berço, carga ou status",
        rows, { it.second.id }, vm.isLoading, vm.errorMessage ?: vm.successMessage, {
            vm.clearMessages()
            editing = null
            berthId = vm.allBerths.firstOrNull()?.id
        }) { (berth, ship) ->
        PortCard(ship.shipName, listOf("${berth.name} · ${berth.sectorName}",
            "Dias %.1f a %.1f · %d t".format(ship.startDay, ship.endDay, ship.tonnage),
            "Descarga · ${ship.unloadingProduct}", "Carga · ${ship.loadingProduct}"),
            ship.status, Color(parseColor(ShipSchedule.colorForStatus(ship.status))),
            onEdit = { vm.clearMessages(); editing = ship; berthId = berth.id },
            onDelete = { vm.clearMessages(); deleting = ship })
    }

    berthId?.let { id ->
        ShipMooringDialog(
            initialBerthId = id,
            initialSchedule = editing,
            allBerths = vm.allBerths,
            registeredVessels = vm.vessels,
            cargos = vm.cargos,
            onDismiss = { berthId = null },
            onSave = { berth, schedule -> vm.addOrUpdateShip(berth, schedule) { berthId = null } },
            validationWarning = vm::validateMooring,
            saveError = vm.errorMessage,
            busy = vm.isLoading
        )
    }
    deleting?.let { ship ->
        ConfirmDeleteDialog(ship.shipName, vm.isLoading, { deleting = null }) {
            deleting = null
            vm.deleteShip(ship.id) {}
        }
    }
}
