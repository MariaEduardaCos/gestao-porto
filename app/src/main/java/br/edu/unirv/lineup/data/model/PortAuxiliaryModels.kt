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

data class PortCargo(
    val productCode: String,
    val name: String,
    val category: CargoCategory,
    val unit: String = "t"
)
