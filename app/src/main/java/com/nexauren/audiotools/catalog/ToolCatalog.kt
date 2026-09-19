package com.nexauren.audiotools.catalog

data class AudioTool(
    val id: String,
    val number: String,
    val title: String,
    val description: String,
    val detail: String,
    val input: String,
    val output: String,
    val category: String,
    val steps: List<String>
)

object ToolCatalog {
    val tools = listOf(
        AudioTool(
            id = "cut",
            number = "01",
            title = "Cortar áudio",
            description = "Escolhe um intervalo e cria um novo ficheiro com apenas o trecho que precisas.",
            detail = "Feito para cortes rápidos sem sair do telemóvel. A V1 exporta áudio AAC/M4A para uma nova cópia.",
            input = "M4A / MP4 com áudio AAC",
            output = "M4A",
            category = "Edição",
            steps = listOf(
                "Escolhe o ficheiro de áudio",
                "Define o início e o fim em segundos",
                "Exporta o trecho como uma nova cópia"
            )
        ),
        AudioTool(
            id = "recorder",
            number = "02",
            title = "Gravador",
            description = "Grava voz, ideias e takes com um toque e guarda tudo localmente.",
            detail = "A gravação usa o microfone do dispositivo e fica disponível imediatamente para reprodução.",
            input = "Microfone do dispositivo",
            output = "M4A / AAC",
            category = "Gravação",
            steps = listOf(
                "Concede acesso ao microfone",
                "Começa e termina a gravação",
                "Reproduz o take guardado"
            )
        ),
        AudioTool(
            id = "analyzer",
            number = "03",
            title = "Analisar áudio",
            description = "Consulta duração, formato, bitrate, canais e taxa de amostragem do ficheiro.",
            detail = "Uma leitura rápida das propriedades do áudio, sem enviar o ficheiro para a internet.",
            input = "Ficheiro de áudio compatível com Android",
            output = "Relatório técnico no ecrã",
            category = "Análise",
            steps = listOf(
                "Escolhe um ficheiro",
                "A aplicação lê as propriedades",
                "Consulta o resumo técnico"
            )
        )
    )

    fun get(id: String): AudioTool? = tools.firstOrNull { it.id == id }
}