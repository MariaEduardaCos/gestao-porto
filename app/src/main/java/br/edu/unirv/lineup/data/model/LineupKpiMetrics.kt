package br.edu.unirv.lineup.data.model

data class LineupKpiMetrics(
    val totalBerths: Int = 60,
    val visibleBerths: Int = 60,
    val totalScheduledShips: Int = 0,
    val totalTonnage: Long = 0L,
    val averageOccupancyPercent: Double = 0.0,
    val activeTideMonthKey: String = "Novembro / 2026 (30 Dias)",
    val lowTideDays: Set<Int> = emptySet()
)
