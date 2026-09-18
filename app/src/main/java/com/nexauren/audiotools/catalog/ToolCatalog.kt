package com.nexauren.audiotools.catalog

data class AudioTool(
    val id: String,
    val number: String,
    val title: String,
    val description: String,
    val input: String,
    val output: String,
    val category: String,
    val steps: List<String>
)

object ToolCatalog {
    val tools = listOf(
        AudioTool("cut","01","Cortar áudio","Escolha início e fim, pré-visualize e prepare o trecho para exportação.","MP3, WAV, AAC, FLAC, OGG, M4A","Formato escolhido","Edição",listOf("Escolha um ficheiro","Defina início e fim","Pré-visualize e exporte")),
        AudioTool("join","02","Juntar áudio","Una vários ficheiros, altere a ordem e prepare transições suaves.","MP3, WAV, AAC, FLAC, OGG, M4A","Um ficheiro final","Edição",listOf("Adicione vários ficheiros","Reordene e ajuste volumes","Exporte a sequência")),
        AudioTool("convert","03","Converter áudio","Converta formatos e prepare a qualidade de saída para diferentes usos.","MP3, WAV, AAC, FLAC, OGG, M4A","MP3, WAV, AAC, FLAC, OGG, M4A","Conversão",listOf("Escolha o áudio","Selecione formato e qualidade","Gere o novo ficheiro")),
        AudioTool("extract","04","Extrair áudio de vídeo","Retire a faixa de áudio de vídeos guardados no dispositivo.","Vídeos suportados pelo Android","Áudio em formato escolhido","Vídeo",listOf("Escolha um vídeo","Selecione o formato","Exporte o áudio")),
        AudioTool("compress","05","Comprimir áudio","Reduza o tamanho do ficheiro equilibrando espaço e qualidade.","Áudio suportado","Ficheiro otimizado","Otimização",listOf("Escolha o áudio","Defina a compressão","Compare e exporte")),
        AudioTool("speed","06","Alterar velocidade","Acelere ou abrande o áudio com controlo fino e opção de preservar o tom.","Áudio suportado","Áudio processado","Edição",listOf("Escolha o áudio","Defina a velocidade","Pré-visualize e exporte")),
        AudioTool("pitch","07","Alterar tom","Ajuste o tom do áudio em semitons.","Áudio suportado","Áudio processado","Edição",listOf("Escolha o áudio","Ajuste o tom","Pré-visualize e exporte")),
        AudioTool("normalize","08","Normalizar volume","Equilibre níveis de volume para uma reprodução mais consistente.","Áudio suportado","Áudio normalizado","Otimização",listOf("Escolha o áudio","Analise o nível","Aplique e exporte")),
        AudioTool("recorder","09","Gravador","Grave voz e ideias rapidamente com pausa, retomada e histórico local.","Microfone","Gravação de áudio","Gravação",listOf("Conceda acesso ao microfone","Grave e pause","Guarde a gravação")),
        AudioTool("editor","10","Editor de áudio","Área de edição com waveform, seleção, corte, silêncio e desfazer.","Áudio suportado","Projeto e áudio exportado","Edição avançada",listOf("Carregue o áudio","Edite a seleção","Aplique e exporte")),
        AudioTool("metadata","11","Editor de metadados","Edite título, artista, álbum, género, ano, faixa e capa.","MP3, M4A e compatíveis","Ficheiro com metadados","Biblioteca",listOf("Escolha o ficheiro","Edite os campos","Guarde uma nova versão")),
        AudioTool("mixer","12","Misturador de áudio","Combine várias faixas com volume individual, fades e organização.","Várias faixas","Uma mistura final","Edição avançada",listOf("Adicione as faixas","Ajuste níveis","Faça o mix e exporte")),
        AudioTool("silence","13","Remover silêncios","Detete intervalos de silêncio e prepare uma versão mais contínua.","Áudio suportado","Áudio processado","Otimização",listOf("Escolha o áudio","Defina o limite","Reveja e exporte")),
        AudioTool("analyzer","14","Analisar áudio","Veja duração, picos, propriedades e indicadores úteis.","Áudio suportado","Relatório de análise","Análise",listOf("Escolha o áudio","Analise as propriedades","Consulte o relatório"))
    )

    fun get(id: String): AudioTool? = tools.firstOrNull { it.id == id }
}