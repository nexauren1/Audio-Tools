package com.nexauren.audiotools.catalog

data class AudioTool(
    val id: String,
    val number: String
)

object ToolCatalog {
    val tools = listOf(
        AudioTool("cut", "01"),
        AudioTool("convert", "02"),
        AudioTool("recorder", "03"),
        AudioTool("analyzer", "04")
    )

    fun get(id: String): AudioTool? = tools.firstOrNull { it.id == id }
}
