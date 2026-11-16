package br.edu.unirv.lineup.ui.screens.lineup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.LineupKpiMetrics
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import br.edu.unirv.lineup.ui.theme.ScheduledOrange
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun KpiRow(
    metrics: LineupKpiMetrics,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("pt", "BR"))
    val formattedTonnage = numberFormat.format(metrics.totalTonnage)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            title = "Berços",
            value = "${metrics.visibleBerths}/${metrics.totalBerths}",
            subtitle = if (metrics.visibleBerths < metrics.totalBerths) "Filtrados" else "Total Ativo",
            modifier = Modifier.weight(1f),
            accentColor = OceanPrimary
        )

        KpiCard(
            title = "Navios",
            value = metrics.totalScheduledShips.toString(),
            subtitle = "Programados",
            modifier = Modifier.weight(1f),
            accentColor = ScheduledOrange
        )

        KpiCard(
            title = "Carga Total",
            value = "${metrics.totalTonnage / 1000}k",
            subtitle = "Toneladas",
            modifier = Modifier.weight(1f),
            accentColor = HighTideGreen
        )

        KpiCard(
            title = "Ocupação",
            value = "${metrics.averageOccupancyPercent}%",
            subtitle = "Média Mensal",
            modifier = Modifier.weight(1f),
            accentColor = OceanPrimary
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
