package br.edu.unirv.lineup.data.model

data class Berth(
    val id: String,
    var name: String,
    var sectorName: String,
    var maxDraft: String,
    var category: CargoCategory,
    var displayOrder: Int = 0,
    val schedules: MutableList<ShipSchedule> = mutableListOf()
) {
    fun findConflictingShip(candidateStart: Double, candidateEnd: Double, ignoreScheduleId: Long?): ShipSchedule? {
        for (other in schedules) {
            if (ignoreScheduleId != null && ignoreScheduleId == other.id) {
                continue
            }
            if (candidateStart < other.endDay && candidateEnd > other.startDay) {
                return other
            }
        }
        return null
    }

    fun hasOverlap(candidateStart: Double, candidateEnd: Double, ignoreScheduleId: Long?): Boolean {
        return findConflictingShip(candidateStart, candidateEnd, ignoreScheduleId) != null
    }
}
