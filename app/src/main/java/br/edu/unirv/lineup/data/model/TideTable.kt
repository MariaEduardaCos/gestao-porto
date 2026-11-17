package br.edu.unirv.lineup.data.model

data class TideMeasurement(
    val dayNumber: Int,
    val timeStr: String,
    val fractionalDay: Double,
    val heightMeters: Double,
    val highTide: Boolean
)

data class TideTable(
    val id: Long,
    val monthKey: String,
    val monthLabel: String,
    val sourceFileName: String = "",
    val activeMonth: Boolean = false,
    val totalMeasurements: Int = 1440,
    val lowTideDays: Set<Int> = emptySet(),
    val measurements: List<TideMeasurement> = emptyList()
) {
    fun isLowTide(day: Int): Boolean = day in lowTideDays

    fun findFirstLowTideDayInRange(startDay: Double, endDay: Double): Int? {
        val firstDay = startDay.toInt().coerceAtLeast(1)
        val lastDay = kotlin.math.max(firstDay, kotlin.math.ceil(endDay - 0.01).toInt()).coerceAtMost(30)
        return (firstDay..lastDay).firstOrNull(::isLowTide)
    }

    fun withMeasurements(readings: List<TideMeasurement>) = copy(
        measurements = readings,
        totalMeasurements = readings.size,
        lowTideDays = readings.filterNot { it.highTide }.map { it.dayNumber }.toSet()
    )
}
