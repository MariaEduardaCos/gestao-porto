package br.edu.unirv.lineup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentPage > 1,
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Página anterior"
            )
            Text(
                text = "Anterior",
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 12.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Pág. $currentPage de $totalPages",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp
            )
            Text(
                text = "($totalItems berços)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        FilledTonalButton(
            onClick = onNext,
            enabled = currentPage < totalPages
        ) {
            Text(
                text = "Próxima",
                modifier = Modifier.padding(end = 4.dp),
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Próxima página"
            )
        }
    }
}
