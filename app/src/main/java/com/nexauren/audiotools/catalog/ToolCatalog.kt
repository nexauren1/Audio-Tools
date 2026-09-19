package com.nexauren.audiotools.catalog

data class AudioTool(
    val id: String,
    val number: String,
    val requiredPlan: String = "FREE"
)

object ToolCatalog {
    val tools = listOf(
        AudioTool("cut", "01", "FREE"),
        AudioTool("convert", "02", "FREE"),
        AudioTool("extract", "03", "FREE"),
        AudioTool("recorder", "04", "FREE"),
        AudioTool("analyzer", "05", "FREE"),
        AudioTool("pro-inspector", "06", "PRO")
    )

    fun get(id: String): AudioTool? =
        tools.firstOrNull {
            it.id == id
        }
}
