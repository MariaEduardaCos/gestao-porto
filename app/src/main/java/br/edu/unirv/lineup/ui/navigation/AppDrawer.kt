package br.edu.unirv.lineup.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import br.edu.unirv.lineup.ui.theme.ScheduledOrange
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel

@Composable
fun AppDrawer(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // Cabeçalho do Menu com Pílula de Conexão (RF001)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = OceanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "GÊMEO DIGITAL APS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Porto de Santos • Multimodal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pílula Dinâmica de Status da Conexão (RF001)
                StatusPill(
                    isOnline = viewModel.isOnline,
                    isMock = viewModel.isMockMode,
                    onClick = { onSelectScreen(Screen.Settings) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Itens da Gaveta de Navegação (com rolagem vertical)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // GRUPO PORTOS
                DrawerGroupHeader(title = "PORTOS (MODAL MARÍTIMO)")

                Screen.portScreens.forEach { screen ->
                    DrawerItem(
                        screen = screen,
                        icon = iconForScreen(screen),
                        isSelected = currentScreen == screen,
                        onClick = { onSelectScreen(screen) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GRUPO FERROVIA
                DrawerGroupHeader(title = "FERROVIA (FIPS & SERRA)")

                Screen.railScreens.forEach { screen ->
                    DrawerItem(
                        screen = screen,
                        icon = iconForScreen(screen),
                        isSelected = currentScreen == screen,
                        onClick = { onSelectScreen(screen) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GRUPO RODOVIA
                DrawerGroupHeader(title = "RODOVIA (GATES & 180 DOCAS)")

                Screen.highwayScreens.forEach { screen ->
                    DrawerItem(
                        screen = screen,
                        icon = iconForScreen(screen),
                        isSelected = currentScreen == screen,
                        onClick = { onSelectScreen(screen) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Rodapé com atalhos fixos (RF001)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelectScreen(Screen.RailWagons) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Vagões", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                }

                OutlinedButton(
                    onClick = { onSelectScreen(Screen.Settings) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Config", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    isOnline: Boolean,
    isMock: Boolean,
    onClick: () -> Unit
) {
    val pillBg = when {
        isOnline -> HighTideGreen.copy(alpha = 0.18f)
        isMock -> ScheduledOrange.copy(alpha = 0.18f)
        else -> Color.Gray.copy(alpha = 0.18f)
    }
    val pillColor = when {
        isOnline -> HighTideGreen
        isMock -> ScheduledOrange
        else -> Color.Gray
    }
    val pillText = when {
        isOnline -> "API Online (LineupBackend)"
        isMock -> "Modo Local (Dados Mocados)"
        else -> "Modo Local / Offline"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pillBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(pillColor, CircleShape)
            )
            Text(
                text = pillText,
                color = pillColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DrawerGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = OceanPrimary,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerItem(
    screen: Screen,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        label = { Text(screen.title, fontSize = 13.sp) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = OceanPrimary.copy(alpha = 0.2f),
            selectedIconColor = OceanPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun iconForScreen(screen: Screen): ImageVector {
    return when (screen) {
        is Screen.Lineup -> Icons.Default.DateRange
        is Screen.PortBerths -> Icons.Default.Place
        is Screen.PortArrivals -> Icons.Default.LocationOn
        is Screen.PortVessels -> Icons.Default.PlayArrow
        is Screen.PortCargos -> Icons.Default.ShoppingCart
        is Screen.PortTides -> Icons.Default.Refresh
        is Screen.RailPera -> Icons.Default.Build
        is Screen.RailBranches -> Icons.Default.Share
        is Screen.RailAxes -> Icons.AutoMirrored.Filled.List
        is Screen.RailLocomotives -> Icons.Default.Build
        is Screen.RailWagons -> Icons.Default.ShoppingCart
        is Screen.RailAllocations -> Icons.Default.CheckCircle
        is Screen.HighwayGantt -> Icons.Default.DateRange
        is Screen.HighwaySectors -> Icons.Default.Place
        is Screen.HighwayTrucks -> Icons.Default.ShoppingCart
        is Screen.HighwayCargos -> Icons.AutoMirrored.Filled.List
        is Screen.HighwayAppointments -> Icons.Default.DateRange
        is Screen.Settings -> Icons.Default.Settings
        else -> Icons.Default.Info
    }
}
