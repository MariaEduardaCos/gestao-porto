package br.edu.unirv.lineup.ui.screens.port

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import br.edu.unirv.lineup.data.model.*
import br.edu.unirv.lineup.ui.components.*
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel

@Composable
fun PortBerthsScreen(vm: MainViewModel) {
    PortCatalog(vm, "Berços portuários", "Buscar nome, setor ou perfil", vm.allBerths, Berth::id,
        { it.name }, { listOf(it.id, it.name, it.sectorName, it.category.shortName) },
        { listOf(it.sectorName, "Calado · ${it.maxDraft}") }, { it.category.shortName },
        vm::deleteBerth) { item, close ->
        var id by remember { mutableStateOf(item?.id.orEmpty()) }
        var name by remember { mutableStateOf(item?.name.orEmpty()) }
        var sector by remember { mutableStateOf(item?.sectorName.orEmpty()) }
        var draft by remember { mutableStateOf(item?.maxDraft.orEmpty()) }
        var category by remember { mutableStateOf(item?.category ?: CargoCategory.SOLID_VEG) }
        PortEditor(if (item == null) "Novo berço" else "Editar berço", vm.isLoading,
            id.matches(Regex("[A-Za-z0-9_-]+")) && name.isNotBlank() && sector.isNotBlank() && validDraft(draft),
            vm.errorMessage, close, {
                vm.saveBerth(Berth(id.uppercase(), name.trim(), sector.trim(), draft.trim(), category,
                    item?.displayOrder ?: 0, item?.schedules ?: mutableListOf()), item != null, close)
            }) {
            PortField(id, { id = it.trim() }, "Código", item == null)
            PortField(name, { name = it }, "Nome")
            PortField(sector, { sector = it }, "Setor")
            PortField(draft, { draft = it }, "Calado máximo (m)")
            PortSelect("Perfil", category, CargoCategory.entries, { it.shortName }) { category = it }
        }
    }
}

@Composable
fun PortCargosScreen(vm: MainViewModel) {
    PortCatalog(vm, "Perfis de carga portuária", "Buscar código, produto ou categoria", vm.cargos,
        PortCargoCatalogItem::code, PortCargoCatalogItem::name,
        { listOf(it.code, it.name, it.category.shortName, it.direction.label) },
        { listOf("${it.code} · ${it.direction.label}", "Prancha · ${it.rateLabel}") },
        { it.category.shortName }, vm::deleteCargo) { item, close ->
        var code by remember { mutableStateOf(item?.code.orEmpty()) }
        var name by remember { mutableStateOf(item?.name.orEmpty()) }
        var rate by remember { mutableStateOf(item?.rateTonsPerDay?.toString().orEmpty()) }
        var category by remember { mutableStateOf(item?.category ?: CargoCategory.SOLID_VEG) }
        var direction by remember { mutableStateOf(item?.direction ?: CargoDirection.LOAD) }
        val tons = rate.toIntOrNull()?.takeIf { it > 0 }
        val otherRate = item?.otherRate?.takeIf { rate.isBlank() }
        PortEditor(if (item == null) "Nova carga" else "Editar carga", vm.isLoading,
            code.matches(Regex("CRG-[0-9]{2}")) && name.isNotBlank() && (tons != null || otherRate != null),
            vm.errorMessage, close, {
                vm.saveCargo(PortCargoCatalogItem(code, name.trim(), category, direction, tons, otherRate),
                    item != null, close)
            }) {
            PortField(code, { code = it.trim().uppercase() }, "Código CRG-xx", item == null)
            PortField(name, { name = it }, "Produto")
            PortSelect("Categoria", category, CargoCategory.entries, { it.shortName }) { category = it }
            PortSelect("Sentido", direction, CargoDirection.entries, { it.label }) { direction = it }
            PortField(rate, { rate = it.trim() }, otherRate?.let { "Prancha atual · $it" } ?: "Prancha (t/dia)")
        }
    }
}

@Composable
fun PortShipsScreen(vm: MainViewModel) {
    PortCatalog(vm, "Navios e embarcações registrados", "Buscar nome, IMO ou classe", vm.vessels,
        RegisteredVessel::imoCode, RegisteredVessel::shipName,
        { listOf(it.imoCode, it.shipName, it.vesselClass) },
        { listOf("${it.imoCode} · ${it.vesselClass}", "Calado · ${it.maxDraftMeters}",
            "Descarga · ${it.defaultUnloadProduct}", "Carga · ${it.defaultLoadProduct}", "${it.defaultTonnage} t") },
        null, vm::deleteVessel) { item, close ->
        var imo by remember { mutableStateOf(item?.imoCode.orEmpty()) }
        var name by remember { mutableStateOf(item?.shipName.orEmpty()) }
        var vesselClass by remember { mutableStateOf(item?.vesselClass.orEmpty()) }
        var draft by remember { mutableStateOf(item?.maxDraftMeters.orEmpty()) }
        var tonnage by remember { mutableStateOf(item?.defaultTonnage?.toString().orEmpty()) }
        var unload by remember { mutableStateOf(item?.defaultUnloadProduct.orEmpty()) }
        var load by remember { mutableStateOf(item?.defaultLoadProduct.orEmpty()) }
        val tons = tonnage.toIntOrNull()?.takeIf { it > 0 }
        PortEditor(if (item == null) "Novo navio" else "Editar navio", vm.isLoading,
            imo.matches(Regex("IMO-[0-9]{7}")) && name.isNotBlank() && vesselClass.isNotBlank() &&
                validDraft(draft) && tons != null, vm.errorMessage, close, {
                vm.saveVessel(RegisteredVessel(imo, name.trim(), vesselClass.trim(), draft.trim(),
                    checkNotNull(tons), unload.trim(), load.trim()), item != null, close)
            }) {
            PortField(imo, { imo = it.trim().uppercase() }, "IMO-xxxxxxx", item == null)
            PortField(name, { name = it }, "Nome")
            PortField(vesselClass, { vesselClass = it }, "Classe")
            PortField(draft, { draft = it }, "Calado máximo (m)")
            PortField(tonnage, { tonnage = it.trim() }, "Tonelagem padrão (t)")
            PortField(unload, { unload = it }, "Produto habitual de descarga")
            PortField(load, { load = it }, "Produto habitual de carga")
        }
    }
}

private fun validDraft(value: String): Boolean =
    value.trim().removeSuffix("m").trim().replace(',', '.').toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true

@Composable
private fun <T : Any> PortCatalog(
    vm: MainViewModel,
    title: String,
    searchLabel: String,
    rows: List<T>,
    id: (T) -> String,
    name: (T) -> String,
    searchFields: (T) -> List<String>,
    details: (T) -> List<String>,
    badge: ((T) -> String)?,
    delete: (String, () -> Unit) -> Unit,
    editor: @Composable (T?, () -> Unit) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<T?>(null) }
    var deleting by remember { mutableStateOf<T?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    PortList(title, query, { query = it }, searchLabel,
        rows.filter { matchesPortSearch(query, *searchFields(it).toTypedArray()) }, id,
        vm.isLoading, vm.errorMessage ?: vm.successMessage, {
            vm.clearMessages()
            editing = null
            showEditor = true
        }) { item ->
        PortCard(name(item), details(item), badge?.invoke(item), onEdit = {
            vm.clearMessages()
            editing = item
            showEditor = true
        }, onDelete = { vm.clearMessages(); deleting = item })
    }
    if (showEditor) editor(editing) { showEditor = false }
    deleting?.let { item ->
        ConfirmDeleteDialog(name(item), vm.isLoading, { deleting = null }) {
            deleting = null
            delete(id(item)) {}
        }
    }
}
