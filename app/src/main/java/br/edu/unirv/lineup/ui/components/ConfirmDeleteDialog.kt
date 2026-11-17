package br.edu.unirv.lineup.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDeleteDialog(name: String, busy: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Confirmar exclusão") },
        text = { Text("Excluir «$name»?") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}
