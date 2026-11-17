package br.edu.unirv.lineup.data.local

import br.edu.unirv.lineup.data.model.TideMeasurement
import java.time.LocalTime

object TideCsvParser {
    fun parse(csv: String): List<TideMeasurement> {
        val readings = csv.lineSequence().map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .filterNot { it.startsWith("Dia;", true) || it.startsWith("Dia,", true) }
            .mapIndexed { index, line ->
                val columns = line.split(if (';' in line) ';' else ',').map(String::trim)
                val day = columns.getOrNull(0)?.toIntOrNull()
                val time = runCatching { LocalTime.parse(columns.getOrNull(1).orEmpty()) }.getOrNull()
                val height = columns.getOrNull(2)?.replace(',', '.')?.toDoubleOrNull()
                val status = columns.getOrNull(3)?.uppercase()
                require(day != null && day in 1..30 && time != null && time.minute % 30 == 0 &&
                    time.second == 0 && time.nano == 0 && height != null && height.isFinite() &&
                    status in setOf("ALTA", "BAIXA")) { "Medição inválida no registro ${index + 1}." }
                TideMeasurement(day, time.toString(), day + time.toSecondOfDay() / 86400.0, height, status == "ALTA")
            }.sortedBy { it.fractionalDay }.toList()
        require(readings.size == 30 * 48 && readings.distinctBy { it.fractionalDay }.size == readings.size) {
            "Informe 48 medições distintas por dia, do dia 1 ao dia 30."
        }
        return readings
    }
}
