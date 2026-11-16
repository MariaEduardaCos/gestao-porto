package br.edu.unirv.lineup.data.model

data class ShipSchedule(
    val id: Long = 0,
    var shipName: String,
    var unloadingProduct: String = "Nenhum (Lastro)",
    var loadingProduct: String = "Nenhum",
    var tonnage: Int = 50000,
    var startDay: Double,
    var endDay: Double,
    var stripeColorHex: String = "#e67e22",
    var status: String = "Programado"
) {
    val durationDays: Double get() = endDay - startDay

    companion object {
        fun colorForStatus(status: String?): String {
            return when (status?.trim()?.lowercase()) {
                "operando", "prioridade", "operando / prioridade" -> "#e74c3c" // Vermelho RN005
                "programado" -> "#e67e22" // Laranja RN005
                "confirmado" -> "#2ecc71" // Verde RN005
                else -> "#3b82f6"
            }
        }
    }
}
