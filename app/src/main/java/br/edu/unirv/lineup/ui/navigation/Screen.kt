package br.edu.unirv.lineup.ui.navigation

sealed class Screen(val title: String, val group: String, val route: String) {
    // Grupo Portos
    data object Lineup : Screen("Lineup de Navios", "PORTOS", "lineup")
    data object PortBerths : Screen("Gestão de Berços", "PORTOS", "port_berths")
    data object PortArrivals : Screen("Programação de Atracações", "PORTOS", "port_arrivals")
    data object PortVessels : Screen("Catálogo de Navios", "PORTOS", "port_vessels")
    data object PortCargos : Screen("Catálogo de Cargas", "PORTOS", "port_cargos")
    data object PortTides : Screen("Tábuas de Marés", "PORTOS", "port_tides")

    // Grupo Ferrovia
    data object RailPera : Screen("Pera Ferroviária (FIPS)", "FERROVIA", "rail_pera")
    data object RailBranches : Screen("Moegas e Ramais", "FERROVIA", "rail_branches")
    data object RailAxes : Screen("Eixos e Pátios da Serra", "FERROVIA", "rail_axes")
    data object RailLocomotives : Screen("Locomotivas", "FERROVIA", "rail_locomotives")
    data object RailWagons : Screen("Gestão de Vagões", "FERROVIA", "rail_wagons")
    data object RailAllocations : Screen("Alocações Trem-Vagão", "FERROVIA", "rail_allocations")

    // Grupo Rodovia
    data object HighwayGantt : Screen("Gantt Rodoviário (180 Docas)", "RODOVIA", "highway_gantt")
    data object HighwaySectors : Screen("Setores Portuários & Pátio", "RODOVIA", "highway_sectors")
    data object HighwayTrucks : Screen("Caminhões & Frotas", "RODOVIA", "highway_trucks")
    data object HighwayCargos : Screen("Cargas Rodoviárias", "RODOVIA", "highway_cargos")
    data object HighwayAppointments : Screen("Agendamentos (Janelas 15m)", "RODOVIA", "highway_appointments")

    // Grupo Sistema
    data object Settings : Screen("Configuração de Servidor REST", "SISTEMA", "settings")

    companion object {
        val portScreens: List<Screen> get() = listOf(Lineup, PortBerths, PortArrivals, PortVessels, PortCargos, PortTides)
        val railScreens: List<Screen> get() = listOf(RailPera, RailBranches, RailAxes, RailLocomotives, RailWagons, RailAllocations)
        val highwayScreens: List<Screen> get() = listOf(HighwayGantt, HighwaySectors, HighwayTrucks, HighwayCargos, HighwayAppointments)
    }
}
