package br.edu.unirv.lineup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.CargoCategory

@Composable
fun CategoryFilters(
    selectedCategories: List<CargoCategory>,
    onToggleCategory: (CargoCategory) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedCategories.isEmpty()

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChip(
                selected = isAllSelected,
                onClick = onClearAll,
                label = { Text("Todos os Berços (60)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        items(CargoCategory.values()) { category ->
            val isSelected = selectedCategories.contains(category)
            val badgeColor = try {
                Color(android.graphics.Color.parseColor(category.badgeColorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            FilterChip(
                selected = isSelected,
                onClick = { onToggleCategory(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Text(
                            text = "${category.shortName} (${category.officialBerthCount})",
                            fontSize = 12.sp
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = badgeColor.copy(alpha = 0.25f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
