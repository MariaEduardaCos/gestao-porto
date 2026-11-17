package br.edu.unirv.lineup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.Normalizer

fun matchesPortSearch(query: String, vararg fields: String): Boolean {
    fun normalize(text: String) = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    val term = normalize(query.trim())
    return fields.any { normalize(it).contains(term) }
}

@Composable
fun <T> PortList(
    title: String,
    query: String,
    onQuery: (String) -> Unit,
    searchLabel: String,
    rows: List<T>,
    key: (T) -> Any,
    busy: Boolean,
    feedback: String?,
    onNew: () -> Unit,
    content: @Composable (T) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("${rows.size} registros", style = MaterialTheme.typography.labelMedium)
            }
            Button(onClick = onNew, enabled = !busy) { Text("Novo +") }
        }
        PortField(query, onQuery, searchLabel)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (feedback != null) Text(feedback, style = MaterialTheme.typography.bodySmall)
        if (rows.isEmpty() && !busy) Text("Nenhum registro encontrado.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = key) { content(it) }
        }
    }
}

@Composable
fun PortCard(
    name: String,
    details: List<String>,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (badge != null) {
                    Surface(color = badgeColor.copy(alpha = 0.13f), shape = MaterialTheme.shapes.small) {
                        Text(badge, Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar $name", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Excluir $name", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun PortField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) },
        enabled = enabled, singleLine = true)
}

@Composable
fun <T> PortSelect(label: String, selected: T, options: List<T>, display: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label · ${display(selected)}")
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = {
                    expanded = false
                    onSelect(option)
                })
            }
        }
    }
}

@Composable
fun PortEditor(
    title: String,
    busy: Boolean,
    allowed: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields()
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onSave, enabled = allowed && !busy) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}
