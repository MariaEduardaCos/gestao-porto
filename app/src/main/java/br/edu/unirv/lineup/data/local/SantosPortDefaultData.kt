package br.edu.unirv.lineup.data.local

import android.content.Context
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.CargoDirection
import br.edu.unirv.lineup.data.model.PortCargoCatalogItem
import br.edu.unirv.lineup.data.model.TideTable

object SantosPortDefaultData {
    val cargos = listOf(
        PortCargoCatalogItem("CRG-01", "Soja em Grãos (Exportação)", CargoCategory.SOLID_VEG, CargoDirection.LOAD, 36000),
        PortCargoCatalogItem("CRG-02", "Milho Granel Exportação", CargoCategory.SOLID_VEG, CargoDirection.LOAD, 34000),
        PortCargoCatalogItem("CRG-03", "Açúcar VHP / Cristal", CargoCategory.SOLID_VEG, CargoDirection.LOAD, 28000),
        PortCargoCatalogItem("CRG-04", "Trigo & Malte Importado", CargoCategory.SOLID_VEG, CargoDirection.UNLOAD, 18500),
        PortCargoCatalogItem("CRG-05", "Cloreto de Potássio / MAP (Fertilizante)", CargoCategory.MINERAL, CargoDirection.UNLOAD, 16000),
        PortCargoCatalogItem("CRG-06", "Enxofre & Sal Marinho Granel", CargoCategory.MINERAL, CargoDirection.UNLOAD, 15000),
        PortCargoCatalogItem("CRG-07", "Contêineres Full / Reefer (TEUs)", CargoCategory.CONTAINERS, CargoDirection.BOTH, null, "2.800 mov/dia"),
        PortCargoCatalogItem("CRG-08", "Celulose Fibra Curta (Fardos)", CargoCategory.PULP, CargoDirection.LOAD, 22000),
        PortCargoCatalogItem("CRG-09", "Diesel S10 / Gasolina / GLP / Químicos", CargoCategory.LIQUID_CHEM, CargoDirection.BOTH, 24000),
        PortCargoCatalogItem("CRG-10", "Suco de Laranja Concentrado (FCOJ)", CargoCategory.CITRUS, CargoDirection.LOAD, 19000),
        PortCargoCatalogItem("CRG-11", "Bobinas, Chapas e Produtos Siderúrgicos", CargoCategory.STEEL, CargoDirection.BOTH, 12500),
        PortCargoCatalogItem("CRG-12", "Veículos Automotores & Máquinas (Ro-Ro)", CargoCategory.VEHICLES, CargoDirection.BOTH, null, "1.800 unid/dia"),
        PortCargoCatalogItem("CRG-13", "Carga de Projeto & Multicargas Geral", CargoCategory.MULTIPURPOSE, CargoDirection.BOTH, 8500),
        PortCargoCatalogItem("CRG-14", "Bagagem & Suprimento Cruzeiros (CONCAIS)", CargoCategory.PASSENGERS, CargoDirection.BOTH, null, "4.500 pax/dia"),
        PortCargoCatalogItem("CRG-15", "Tubos Flexíveis & Suprimento Offshore", CargoCategory.OFFSHORE, CargoDirection.BOTH, 6000)
    )

    fun tides(context: Context): List<TideTable> = listOf(
        tide(context, 1, "Novembro", "novembro"),
        tide(context, 2, "Setembro", "setembro"),
        tide(context, 3, "Junho", "junho")
    )

    private fun tide(context: Context, id: Long, month: String, fileMonth: String): TideTable {
        val file = "tides/mare_${fileMonth}_2026.csv"
        val readings = context.assets.open(file).bufferedReader().use { TideCsvParser.parse(it.readText()) }
        return TideTable(id, "$month / 2026 (30 Dias)", "$month / 2026", "/$file", id == 1L)
            .withMeasurements(readings)
    }
}
