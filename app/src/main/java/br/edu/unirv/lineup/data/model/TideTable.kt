package br.edu.unirv.lineup.data.model

data class TideTable(
    val id: Long,
    val monthKey: String,
    val monthLabel: String,
    val sourceFileName: String = "",
    var activeMonth: Boolean = false,
    val totalMeasurements: Int = 1440,
    val lowTideDays: Set<Int> = emptySet()
) {
    fun isLowTide(day: Int): Boolean = lowTideDays.contains(day)

    fun findFirstLowTideDayInRange(startDay: Double, endDay: Double): Int? {
        val firstDay = startDay.toInt().coerceAtLeast(1)
        val lastDay = kotlin.math.max(firstDay, kotlin.math.ceil(endDay - 0.01).toInt()).coerceAtMost(30)
        for (d in firstDay..lastDay) {
            if (isLowTide(d)) return d
        }
        return null
    }
}
