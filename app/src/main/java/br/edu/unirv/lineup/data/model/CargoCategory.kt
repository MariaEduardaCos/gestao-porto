package br.edu.unirv.lineup.data.model

enum class CargoCategory(
    val fullName: String,
    val shortName: String,
    val officialBerthCount: Int,
    val badgeColorHex: String
) {
    SOLID_VEG(
        "Granéis sólidos – Vegetais (soja, milho, farelo, trigo)",
        "Granéis Sólidos Vegetais",
        14,
        "#16a34a"
    ),
    CONTAINERS(
        "Carga Geral – Contêineres",
        "Contêineres",
        10,
        "#0284c7"
    ),
    PULP(
        "Carga geral – Celulose",
        "Celulose",
        9,
        "#0d9488"
    ),
    LIQUID_CHEM(
        "Granéis líquidos – Químicos/Combustíveis",
        "Líquidos Químicos/Combustíveis",
        8,
        "#ea580c"
    ),
    STEEL(
        "Carga geral – Produtos siderúrgicos",
        "Produtos Siderúrgicos",
        5,
        "#475569"
    ),
    MINERAL(
        "Granéis sólidos – Minerais de desembarque (fertilizantes, enxofre, sal)",
        "Granéis Minerais",
        4,
        "#ca8a04"
    ),
    CITRUS(
        "Granéis líquidos – Sucos cítricos",
        "Sucos Cítricos",
        3,
        "#f59e0b"
    ),
    MULTIPURPOSE(
        "Berços públicos multipropósito",
        "Multipropósito",
        3,
        "#6366f1"
    ),
    VEHICLES(
        "Carga Geral – Veículos",
        "Veículos (Ro-Ro)",
        2,
        "#8b5cf6"
    ),
    PASSENGERS(
        "Passageiros",
        "Passageiros (Cruzeiros)",
        1,
        "#ec4899"
    ),
    OFFSHORE(
        "Carga offshore",
        "Carga Offshore",
        1,
        "#06b6d4"
    )
}
