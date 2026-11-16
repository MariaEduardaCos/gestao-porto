package br.edu.unirv.lineup.ui.screens.lineup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import java.util.Locale

@Composable
internal fun NewBerthDialog(
    existingBerths: List<Berth>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Berth) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CargoCategory.SOLID_VEG) }
    var showCategories by remember { mutableStateOf(false) }

    val id = code.trim().uppercase(Locale.ROOT)
    val duplicate = existingBerths.any { it.id.equals(id, ignoreCase = true) }
    val draftNumber = draft.trim().replace(',', '.').toDoubleOrNull()
    val validDraft = draftNumber != null && draftNumber.isFinite() && draftNumber > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Berço") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Código do berço") },
                    supportingText = { if (duplicate) Text("Código já cadastrado.") },
                    isError = duplicate,
                    singleLine = true
                )
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(sector, { sector = it }, label = { Text("Setor") }, singleLine = true)
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Calado máximo (m)") },
                    isError = draft.isNotBlank() && !validDraft,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Categoria de carga")
                OutlinedButton(
                    onClick = { showCategories = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(category.shortName) }
                DropdownMenu(expanded = showCategories, onDismissRequest = { showCategories = false }) {
                    CargoCategory.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.shortName) }, onClick = {
                            category = option
                            showCategories = false
                        })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(Berth(id, name.trim(), sector.trim(), "${draft.trim()} m", category)) },
                enabled = !isSaving && id.isNotBlank() && !duplicate &&
                    name.isNotBlank() && sector.isNotBlank() && validDraft
            ) { Text("Cadastrar berço") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
