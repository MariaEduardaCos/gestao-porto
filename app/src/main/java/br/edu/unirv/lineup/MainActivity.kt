package br.edu.unirv.lineup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.ui.navigation.AppDrawer
import br.edu.unirv.lineup.ui.navigation.Screen
import br.edu.unirv.lineup.ui.screens.PlaceholderScreen
import br.edu.unirv.lineup.ui.screens.port.*
import br.edu.unirv.lineup.ui.screens.settings.SettingsScreen
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.theme.LineupTheme
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import br.edu.unirv.lineup.ui.theme.ScheduledOrange
import br.edu.unirv.lineup.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LineupTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentScreen = viewModel.currentScreen,
                            onSelectScreen = { screen ->
                                viewModel.currentScreen = screen
                                coroutineScope.launch { drawerState.close() }
                            },
                            viewModel = viewModel
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = viewModel.currentScreen.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "GÊMEO DIGITAL APS • ${viewModel.currentScreen.group}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Abrir menu"
                                        )
                                    }
                                },
                                actions = {
                                    // Mini pílula de status na TopBar
                                    TopBarStatusBadge(
                                        isOnline = viewModel.isOnline,
                                        isMock = viewModel.isMockMode,
                                        onClick = { viewModel.currentScreen = Screen.Settings }
                                    )

                                    IconButton(onClick = { viewModel.loadAllData() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Atualizar dados"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (viewModel.currentScreen) {
                                is Screen.Lineup -> LineupScreen(viewModel = viewModel)
                                is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                                Screen.PortBerths -> PortBerthsScreen(viewModel)
                                Screen.PortCargos -> PortCargosScreen(viewModel)
                                Screen.PortVessels -> PortShipsScreen(viewModel)
                                Screen.PortTides -> PortTidesScreen(viewModel)
                                Screen.PortArrivals -> PortArrivalsScreen(viewModel)
                                else -> PlaceholderScreen(
                                    screen = viewModel.currentScreen,
                                    onBackToLineup = { viewModel.currentScreen = Screen.Lineup }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarStatusBadge(
    isOnline: Boolean,
    isMock: Boolean,
    onClick: () -> Unit
) {
    val color = when {
        isOnline -> HighTideGreen
        isMock -> ScheduledOrange
        else -> Color.Gray
    }
    val label = when {
        isOnline -> "Online"
        isMock -> "Modo Local / Offline"
        else -> "Offline"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.18f),
        modifier = Modifier.padding(end = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
