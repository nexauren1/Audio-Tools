package com.nexauren.audiotools.catalog

data class AudioTool(
    val id: String,
    val number: String
)

object ToolCatalog {
    val tools = listOf(
        AudioTool("cut", "01"),
        AudioTool("convert", "02"),
        AudioTool("extract", "03"),
        AudioTool("recorder", "04"),
        AudioTool("analyzer", "05"),
        AudioTool("pro-inspector", "06")
    )

    fun get(id: String): AudioTool? = tools.firstOrNull { it.id == id }
}
