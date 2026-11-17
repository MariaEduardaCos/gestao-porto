package br.edu.unirv.lineup.data.model

data class RegisteredVessel(
    val imoCode: String,
    val shipName: String,
    val vesselClass: String,
    val maxDraftMeters: String,
    val defaultTonnage: Int,
    val defaultUnloadProduct: String,
    val defaultLoadProduct: String
)

enum class CargoDirection(val label: String) {
    LOAD("Carga"),
    UNLOAD("Descarga"),
    BOTH("Descarga e carga")
}

data class PortCargoCatalogItem(
    val code: String,
    val name: String,
    val category: CargoCategory,
    val direction: CargoDirection,
    val rateTonsPerDay: Int?,
    val otherRate: String? = null
) {
    val rateLabel: String get() = rateTonsPerDay?.let { "$it t/dia" } ?: otherRate.orEmpty()

    fun durationDays(tonnage: Int): Long? =
        rateTonsPerDay?.takeIf { it > 0 }?.let { (tonnage.toLong() + it - 1) / it }
}
