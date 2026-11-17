package br.edu.unirv.lineup.ui.screens.port

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import br.edu.unirv.lineup.data.model.TideMeasurement
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.ui.components.*
import br.edu.unirv.lineup.ui.screens.lineup.TideSelector
import br.edu.unirv.lineup.ui.theme.DangerZoneRed
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PortTidesScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<TideTable?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TideTable?>(null) }
    val active = vm.activeTideTable

    val rows = buildList<TideRow> {
        vm.tideTables.forEach { table ->
            val matchesMonth = matchesPortSearch(query, table.monthLabel, table.sourceFileName)
            val days = if (table.id == active?.id) {
                table.measurements.groupBy { it.dayNumber }.mapNotNull { (day, readings) ->
                    val blocked = table.isLowTide(day)
                    if (matchesMonth || matchesPortSearch(query, dayLabel(day), statusLabel(blocked))) {
                        TideRow.Day(day, blocked, readings)
                    } else null
                }
            } else emptyList()
            if (matchesMonth || days.isNotEmpty()) {
                add(TideRow.Month(table))
                addAll(days)
            }
        }
    }

    Column {
        if (active != null) {
            TideSelector(active, vm.tideTables, onSelectTide = { if (it != active.id) vm.switchTideTable(it) })
            Spacer(Modifier.height(8.dp))
        }
        PortList("Tábua de marés do Porto de Santos", query, { query = it },
            "Buscar mês, dia ou status", rows, TideRow::key, vm.isLoading,
            vm.errorMessage ?: vm.successMessage, {
                vm.clearMessages()
                editing = null
                showEditor = true
            }) { row ->
            when (row) {
                is TideRow.Month -> {
                    val table = row.table
                    PortCard(table.monthLabel, listOf("${table.totalMeasurements} medições · 30 min"),
                        if (table.activeMonth) "Ativa" else null, onEdit = {
                            vm.clearMessages()
                            editing = table
                            showEditor = true
                        }, onDelete = { vm.clearMessages(); deleting = table })
                }
                is TideRow.Day -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val color = if (row.blocked) DangerZoneRed else HighTideGreen
                        Text("${dayLabel(row.day)} · ${statusLabel(row.blocked)}", color = color,
                            style = MaterialTheme.typography.titleSmall)
                        Text("Mín. %.2f m · Máx. %.2f m · %d medições".format(
                            row.readings.minOf { it.heightMeters }, row.readings.maxOf { it.heightMeters }, row.readings.size
                        ), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (showEditor) TideEditor(vm, editing) { showEditor = false }
    deleting?.let { table ->
        ConfirmDeleteDialog(table.monthLabel, vm.isLoading, { deleting = null }) {
            deleting = null
            vm.deleteTide(table.id) {}
        }
    }
}

private sealed class TideRow(val key: String) {
    data class Month(val table: TideTable) : TideRow("month-${table.id}")
    data class Day(val day: Int, val blocked: Boolean, val readings: List<TideMeasurement>) : TideRow("day-$day")
}

private fun dayLabel(day: Int) = "Dia %02d".format(day)
private fun statusLabel(blocked: Boolean) =
    if (blocked) "Maré baixa · Bloqueado" else "Maré alta · Liberado"

@Composable
private fun TideEditor(vm: MainViewModel, table: TideTable?, close: () -> Unit) {
    var label by remember { mutableStateOf(table?.monthLabel.orEmpty()) }
    var csv by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf(table?.sourceFileName?.substringAfterLast('/').orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            reading = true
            error = null
            scope.launch {
                try {
                    val (file, content) = withContext(Dispatchers.IO) {
                        val file = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "mare.csv"
                        val content = resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            val buffer = CharArray(262_144)
                            var size = 0
                            while (size < buffer.size) {
                                val count = reader.read(buffer, size, buffer.size - size)
                                if (count < 0) break
                                size += count
                            }
                            require(size < buffer.size) { "O arquivo CSV é grande demais." }
                            String(buffer, 0, size)
                        } ?: error("Não foi possível abrir o CSV.")
                        file to content
                    }
                    source = file
                    csv = content
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    error = e.localizedMessage
                } finally {
                    reading = false
                }
            }
        }
    }
    PortEditor(if (table == null) "Nova tábua de marés" else "Editar tábua de marés",
        vm.isLoading || reading, label.isNotBlank() && (csv != null || table?.measurements?.isNotEmpty() == true),
        error ?: vm.errorMessage, close, {
            val data = csv ?: checkNotNull(table).measurements.joinToString("\n") { reading ->
                val status = if (reading.highTide) "ALTA" else "BAIXA"
                "${reading.dayNumber};${reading.timeStr};${reading.heightMeters};$status"
            }
            vm.saveTide(label.trim(), source, data, table, close)
        }) {
        PortField(label, { label = it }, "Mês de referência")
        OutlinedButton(onClick = { picker.launch("*/*") }, enabled = !reading && !vm.isLoading) {
            Text("Selecionar CSV")
        }
        if (source.isNotBlank()) Text(source)
        Text("Informe as 48 leituras de cada dia, em intervalos de 30 minutos.")
    }
}
