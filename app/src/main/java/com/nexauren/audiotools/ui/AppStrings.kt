package com.nexauren.audiotools.ui

import android.content.Context

data class ToolCopy(
    val title: String,
    val description: String,
    val detail: String,
    val category: String,
    val input: String,
    val output: String,
    val steps: List<String>
)

object LanguageManager {
    private const val PREFS = "settings"
    private const val KEY = "language"
    val codes = listOf("pt", "en", "fr", "es", "de")

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "pt") ?: "pt"

    fun set(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, code).apply()
    }

    fun name(code: String): String = when (code) {
        "en" -> "English"
        "fr" -> "Français"
        "es" -> "Español"
        "de" -> "Deutsch"
        else -> "Português"
    }
}

object AppStrings {
    private val ui = mapOf(
        "pt" to mapOf(
            "menu" to "Menu", "settings" to "Definições", "tools" to "Ferramentas", "updates" to "Atualizações", "about" to "Sobre",
            "workspace" to "WORKSPACE", "choose" to "Escolhe o que precisas", "workspace_desc" to "Ferramentas rápidas para editar, converter, gravar e analisar áudio.",
            "active" to "ATIVAS", "audio_workspace" to "AUDIO WORKSPACE", "local" to "LOCAL", "edit" to "Editar", "record" to "Gravar", "analyze" to "Analisar", "convert" to "Converter",
            "open_tools" to "Abrir ferramentas", "quick_flow" to "FLUXO RÁPIDO", "flow_title" to "Escolher  →  processar  →  guardar",
            "open" to "Abrir", "process" to "Processar", "save" to "Guardar", "local_processing" to "Processamento local", "local_desc" to "Os ficheiros são trabalhados no dispositivo.",
            "back" to "Voltar", "how" to "Como funciona", "compatibility" to "Compatibilidade", "input" to "Entrada", "output" to "Saída", "ready" to "Pronto",
            "choose_audio" to "Escolher áudio", "cut_action" to "Cortar e guardar", "cut_bad" to "Escolhe um intervalo válido dentro da duração do áudio.",
            "cut_info" to "Arrasta as pegas para definir exatamente onde começa e termina o corte.",
            "cut_precise" to "CORTE DE PRECISÃO", "original" to "▶ Ouvir original", "result" to "▶ Reproduzir resultado",
            "cutting" to "A cortar o áudio…", "cut_ok" to "Pronto", "cut_error" to "Não foi possível cortar. Nesta versão, o cortador requer M4A/AAC.",
            "permission_mic" to "O microfone é necessário para gravar.", "record_ready" to "Pronto para gravar.", "record_start" to "●  Começar gravação", "record_stop" to "■  Parar gravação",
            "record_play" to "▶ Reproduzir última gravação", "recording_saved" to "Gravação guardada no dispositivo.", "recording_error" to "Não foi possível iniciar a gravação.",
            "record_short" to "A gravação foi demasiado curta. Tenta novamente.", "live" to "LIVE", "monitoring" to "MONITORIZAÇÃO EM TEMPO REAL",
            "report" to "RELATÓRIO TÉCNICO", "select_for_report" to "Seleciona um áudio para obter o relatório.", "analyze_action" to "Analisar áudio",
            "analysis_error" to "Não foi possível analisar este ficheiro.", "playing" to "A reproduzir.", "completed" to "Reprodução concluída.",
            "not_available" to "O ficheiro já não está disponível.", "selected" to "Selecionado", "analyzing" to "A analisar",
            "duration" to "Duração", "format" to "Formato", "bitrate" to "Bitrate", "channels" to "Canais", "sample_rate" to "Amostragem",
            "converter" to "Conversor", "converter_pill" to "CONVERSÃO LOCAL", "converter_desc" to "Converte entre WAV e M4A/AAC diretamente no telemóvel.",
            "source_format" to "Formato de origem", "target_format" to "Formato de saída", "convert_action" to "Converter e guardar",
            "converted" to "Conversão concluída", "conversion_error" to "Não foi possível converter este ficheiro. Use WAV ou M4A/AAC.",
            "supported" to "Suportado: WAV ↔ M4A/AAC", "converter_tip" to "Escolhe o formato de saída. A aplicação processa o áudio localmente.",
            "audio_file" to "Ficheiro de áudio", "select_source" to "Escolher ficheiro", "conversion_progress" to "A converter…",
            "language" to "Idioma", "language_desc" to "Traduz toda a interface do Audio Tools.", "choose_language" to "Escolher idioma",
            "update_section" to "ATUALIZAÇÕES", "installed" to "Versão instalada", "check_update" to "Verificar atualizações",
            "install_permission" to "Permissão para instalar atualizações", "auto_check" to "Verificar ao abrir as definições",
            "already_latest" to "Já tens a versão mais recente.", "update_available" to "Atualização disponível", "download_install" to "Baixar e instalar",
            "download_started" to "Download iniciado. Quando terminar, o instalador do Android será aberto.", "release_missing" to "Há um novo release, mas ainda não existe um APK anexado.",
            "installer_permission" to "Primeiro permite instalações desta aplicação.", "permission_active" to "A permissão já está ativa.",
            "checking" to "A verificar…", "download_error" to "Não foi possível descarregar a atualização.", "release_open" to "Abrir release",
            "app_section" to "APP", "about_title" to "Sobre o Audio Tools", "about_desc" to "Ferramentas de áudio focadas, rápidas e locais, com módulos independentes para crescer sem complicar.",
            "about_head" to "Feito para trabalhar com áudio.", "about_body" to "Corta, converte, grava e analisa sem enviar os teus ficheiros para um serviço de processamento externo.",
            "four_tools" to "4 ferramentas funcionais", "version" to "Versão", "back_action" to "Voltar",
            "edit_tool" to "EDITOR", "convert_tool" to "CONVERSOR", "record_tool" to "GRAVAÇÃO", "analyze_tool" to "ANÁLISE"
        ),
        "en" to mapOf(
            "menu" to "Menu", "settings" to "Settings", "tools" to "Tools", "updates" to "Updates", "about" to "About",
            "workspace" to "WORKSPACE", "choose" to "Choose what you need", "workspace_desc" to "Fast tools to edit, convert, record and analyze audio.",
            "active" to "ACTIVE", "audio_workspace" to "AUDIO WORKSPACE", "local" to "LOCAL", "edit" to "Edit", "record" to "Record", "analyze" to "Analyze", "convert" to "Convert",
            "open_tools" to "Open tools", "quick_flow" to "QUICK FLOW", "flow_title" to "Choose  →  process  →  save", "open" to "Open", "process" to "Process", "save" to "Save", "local_processing" to "Local processing", "local_desc" to "Files are processed on the device.",
            "back" to "Back", "how" to "How it works", "compatibility" to "Compatibility", "input" to "Input", "output" to "Output", "ready" to "Ready",
            "choose_audio" to "Choose audio", "cut_action" to "Cut and save", "cut_bad" to "Choose a valid interval inside the audio duration.", "cut_info" to "Drag the handles to set exactly where the cut starts and ends.",
            "cut_precise" to "PRECISION CUT", "original" to "▶ Play original", "result" to "▶ Play result", "cutting" to "Cutting audio…", "cut_ok" to "Ready", "cut_error" to "Could not cut the file. This cutter currently requires M4A/AAC.",
            "permission_mic" to "Microphone access is required to record.", "record_ready" to "Ready to record.", "record_start" to "●  Start recording", "record_stop" to "■  Stop recording",
            "record_play" to "▶ Play latest recording", "recording_saved" to "Recording saved on the device.", "recording_error" to "Could not start recording.", "record_short" to "Recording was too short. Try again.",
            "live" to "LIVE", "monitoring" to "REAL-TIME MONITOR", "report" to "TECHNICAL REPORT", "select_for_report" to "Select audio to generate the report.", "analyze_action" to "Analyze audio",
            "analysis_error" to "Could not analyze this file.", "playing" to "Playing.", "completed" to "Playback finished.", "not_available" to "The file is no longer available.",
            "selected" to "Selected", "analyzing" to "Analyzing", "duration" to "Duration", "format" to "Format", "bitrate" to "Bitrate", "channels" to "Channels", "sample_rate" to "Sample rate",
            "converter" to "Converter", "converter_pill" to "LOCAL CONVERSION", "converter_desc" to "Convert between WAV and M4A/AAC directly on your phone.", "source_format" to "Source format", "target_format" to "Output format",
            "convert_action" to "Convert and save", "converted" to "Conversion complete", "conversion_error" to "Could not convert this file. Use WAV or M4A/AAC.", "supported" to "Supported: WAV ↔ M4A/AAC",
            "converter_tip" to "Choose an output format. Audio is processed locally.", "audio_file" to "Audio file", "select_source" to "Choose file", "conversion_progress" to "Converting…",
            "language" to "Language", "language_desc" to "Translate the entire Audio Tools interface.", "choose_language" to "Choose language",
            "update_section" to "UPDATES", "installed" to "Installed version", "check_update" to "Check for updates", "install_permission" to "Permission to install updates",
            "auto_check" to "Check when opening settings", "already_latest" to "You already have the latest version.", "update_available" to "Update available", "download_install" to "Download and install",
            "download_started" to "Download started. When it finishes, the Android installer will open.", "release_missing" to "A new release exists, but no APK is attached yet.",
            "installer_permission" to "First allow this app to install updates.", "permission_active" to "Permission is already enabled.", "checking" to "Checking…", "download_error" to "Could not download the update.",
            "release_open" to "Open release", "app_section" to "APP", "about_title" to "About Audio Tools", "about_desc" to "Focused, fast local audio tools with independent modules designed to grow without becoming complicated.",
            "about_head" to "Made for working with audio.", "about_body" to "Cut, convert, record and analyze without sending your files to an external processing service.", "four_tools" to "4 functional tools", "version" to "Version", "back_action" to "Back",
            "edit_tool" to "EDITOR", "convert_tool" to "CONVERTER", "record_tool" to "RECORDING", "analyze_tool" to "ANALYSIS"
        ),
        "fr" to mapOf(
            "menu" to "Menu", "settings" to "Réglages", "tools" to "Outils", "updates" to "Mises à jour", "about" to "À propos",
            "workspace" to "ESPACE DE TRAVAIL", "choose" to "Choisis ce dont tu as besoin", "workspace_desc" to "Outils rapides pour éditer, convertir, enregistrer et analyser l’audio.",
            "active" to "ACTIFS", "audio_workspace" to "ESPACE AUDIO", "local" to "LOCAL", "edit" to "Éditer", "record" to "Enregistrer", "analyze" to "Analyser", "convert" to "Convertir",
            "open_tools" to "Ouvrir les outils", "quick_flow" to "FLUX RAPIDE", "flow_title" to "Choisir  →  traiter  →  enregistrer", "open" to "Ouvrir", "process" to "Traiter", "save" to "Enregistrer", "local_processing" to "Traitement local", "local_desc" to "Les fichiers sont traités sur l’appareil.",
            "back" to "Retour", "how" to "Fonctionnement", "compatibility" to "Compatibilité", "input" to "Entrée", "output" to "Sortie", "ready" to "Prêt",
            "choose_audio" to "Choisir un audio", "cut_action" to "Couper et enregistrer", "cut_bad" to "Choisis un intervalle valide dans la durée du fichier.", "cut_info" to "Fais glisser les poignées pour définir précisément le début et la fin.",
            "cut_precise" to "COUPE DE PRÉCISION", "original" to "▶ Lire l’original", "result" to "▶ Lire le résultat", "cutting" to "Découpe de l’audio…", "cut_ok" to "Prêt", "cut_error" to "Impossible de couper. Cette version nécessite M4A/AAC.",
            "permission_mic" to "Le microphone est nécessaire pour enregistrer.", "record_ready" to "Prêt à enregistrer.", "record_start" to "●  Démarrer l’enregistrement", "record_stop" to "■  Arrêter l’enregistrement",
            "record_play" to "▶ Lire le dernier enregistrement", "recording_saved" to "Enregistrement sauvegardé sur l’appareil.", "recording_error" to "Impossible de démarrer l’enregistrement.", "record_short" to "Enregistrement trop court. Réessaie.",
            "live" to "LIVE", "monitoring" to "MONITORING EN TEMPS RÉEL", "report" to "RAPPORT TECHNIQUE", "select_for_report" to "Sélectionne un audio pour générer le rapport.", "analyze_action" to "Analyser l’audio",
            "analysis_error" to "Impossible d’analyser ce fichier.", "playing" to "Lecture en cours.", "completed" to "Lecture terminée.", "not_available" to "Le fichier n’est plus disponible.", "selected" to "Sélectionné", "analyzing" to "Analyse",
            "duration" to "Durée", "format" to "Format", "bitrate" to "Bitrate", "channels" to "Canaux", "sample_rate" to "Échantillonnage",
            "converter" to "Convertisseur", "converter_pill" to "CONVERSION LOCALE", "converter_desc" to "Convertis WAV et M4A/AAC directement sur ton téléphone.", "source_format" to "Format source", "target_format" to "Format de sortie",
            "convert_action" to "Convertir et enregistrer", "converted" to "Conversion terminée", "conversion_error" to "Conversion impossible. Utilise WAV ou M4A/AAC.", "supported" to "Prise en charge : WAV ↔ M4A/AAC",
            "converter_tip" to "Choisis un format de sortie. Le traitement reste local.", "audio_file" to "Fichier audio", "select_source" to "Choisir un fichier", "conversion_progress" to "Conversion…",
            "language" to "Langue", "language_desc" to "Traduit toute l’interface Audio Tools.", "choose_language" to "Choisir la langue",
            "update_section" to "MISES À JOUR", "installed" to "Version installée", "check_update" to "Vérifier les mises à jour", "install_permission" to "Autoriser l’installation des mises à jour",
            "auto_check" to "Vérifier à l’ouverture des réglages", "already_latest" to "Tu utilises déjà la dernière version.", "update_available" to "Mise à jour disponible", "download_install" to "Télécharger et installer",
            "download_started" to "Téléchargement lancé. À la fin, l’installateur Android s’ouvrira.", "release_missing" to "Une nouvelle release existe, mais aucun APK n’est attaché.",
            "installer_permission" to "Autorise d’abord les installations pour cette application.", "permission_active" to "L’autorisation est déjà active.", "checking" to "Vérification…", "download_error" to "Impossible de télécharger la mise à jour.",
            "release_open" to "Ouvrir la release", "app_section" to "APP", "about_title" to "À propos d’Audio Tools", "about_desc" to "Des outils audio locaux, rapides et ciblés, conçus pour évoluer sans devenir compliqués.",
            "about_head" to "Pensé pour travailler avec l’audio.", "about_body" to "Coupe, convertis, enregistre et analyse sans envoyer tes fichiers vers un service de traitement externe.", "four_tools" to "4 outils fonctionnels", "version" to "Version", "back_action" to "Retour",
            "edit_tool" to "ÉDITEUR", "convert_tool" to "CONVERTISSEUR", "record_tool" to "ENREGISTREMENT", "analyze_tool" to "ANALYSE"
        ),
        "es" to mapOf(
            "menu" to "Menú", "settings" to "Ajustes", "tools" to "Herramientas", "updates" to "Actualizaciones", "about" to "Acerca de",
            "workspace" to "ESPACIO", "choose" to "Elige lo que necesitas", "workspace_desc" to "Herramientas rápidas para editar, convertir, grabar y analizar audio.",
            "active" to "ACTIVAS", "audio_workspace" to "ESPACIO DE AUDIO", "local" to "LOCAL", "edit" to "Editar", "record" to "Grabar", "analyze" to "Analizar", "convert" to "Convertir",
            "open_tools" to "Abrir herramientas", "quick_flow" to "FLUJO RÁPIDO", "flow_title" to "Elegir  →  procesar  →  guardar", "open" to "Abrir", "process" to "Procesar", "save" to "Guardar", "local_processing" to "Procesamiento local", "local_desc" to "Los archivos se procesan en el dispositivo.",
            "back" to "Volver", "how" to "Cómo funciona", "compatibility" to "Compatibilidad", "input" to "Entrada", "output" to "Salida", "ready" to "Listo",
            "choose_audio" to "Elegir audio", "cut_action" to "Cortar y guardar", "cut_bad" to "Elige un intervalo válido dentro de la duración del audio.", "cut_info" to "Arrastra los controles para definir exactamente el inicio y el final.",
            "cut_precise" to "CORTE DE PRECISIÓN", "original" to "▶ Escuchar original", "result" to "▶ Reproducir resultado", "cutting" to "Cortando audio…", "cut_ok" to "Listo", "cut_error" to "No se pudo cortar. Esta versión requiere M4A/AAC.",
            "permission_mic" to "Se necesita el micrófono para grabar.", "record_ready" to "Listo para grabar.", "record_start" to "●  Empezar grabación", "record_stop" to "■  Detener grabación",
            "record_play" to "▶ Reproducir última grabación", "recording_saved" to "Grabación guardada en el dispositivo.", "recording_error" to "No se pudo iniciar la grabación.", "record_short" to "La grabación fue demasiado corta. Inténtalo de nuevo.",
            "live" to "LIVE", "monitoring" to "MONITOR EN TIEMPO REAL", "report" to "INFORME TÉCNICO", "select_for_report" to "Selecciona un audio para generar el informe.", "analyze_action" to "Analizar audio",
            "analysis_error" to "No se pudo analizar este archivo.", "playing" to "Reproduciendo.", "completed" to "Reproducción terminada.", "not_available" to "El archivo ya no está disponible.", "selected" to "Seleccionado", "analyzing" to "Analizando",
            "duration" to "Duración", "format" to "Formato", "bitrate" to "Bitrate", "channels" to "Canales", "sample_rate" to "Muestreo",
            "converter" to "Conversor", "converter_pill" to "CONVERSIÓN LOCAL", "converter_desc" to "Convierte entre WAV y M4A/AAC directamente en tu teléfono.", "source_format" to "Formato de origen", "target_format" to "Formato de salida",
            "convert_action" to "Convertir y guardar", "converted" to "Conversión completada", "conversion_error" to "No se pudo convertir. Usa WAV o M4A/AAC.", "supported" to "Compatible: WAV ↔ M4A/AAC",
            "converter_tip" to "Elige un formato de salida. El audio se procesa localmente.", "audio_file" to "Archivo de audio", "select_source" to "Elegir archivo", "conversion_progress" to "Convirtiendo…",
            "language" to "Idioma", "language_desc" to "Traduce toda la interfaz de Audio Tools.", "choose_language" to "Elegir idioma",
            "update_section" to "ACTUALIZACIONES", "installed" to "Versión instalada", "check_update" to "Buscar actualizaciones", "install_permission" to "Permiso para instalar actualizaciones",
            "auto_check" to "Comprobar al abrir ajustes", "already_latest" to "Ya tienes la versión más reciente.", "update_available" to "Actualización disponible", "download_install" to "Descargar e instalar",
            "download_started" to "Descarga iniciada. Al terminar, se abrirá el instalador de Android.", "release_missing" to "Existe una release nueva, pero todavía no hay APK adjunto.",
            "installer_permission" to "Primero permite que esta aplicación instale actualizaciones.", "permission_active" to "El permiso ya está activo.", "checking" to "Comprobando…", "download_error" to "No se pudo descargar la actualización.",
            "release_open" to "Abrir release", "app_section" to "APP", "about_title" to "Acerca de Audio Tools", "about_desc" to "Herramientas de audio locales, rápidas y enfocadas, diseñadas para crecer sin complicarse.",
            "about_head" to "Hecho para trabajar con audio.", "about_body" to "Corta, convierte, graba y analiza sin enviar tus archivos a un servicio de procesamiento externo.", "four_tools" to "4 herramientas funcionales", "version" to "Versión", "back_action" to "Volver",
            "edit_tool" to "EDITOR", "convert_tool" to "CONVERSOR", "record_tool" to "GRABACIÓN", "analyze_tool" to "ANÁLISIS"
        ),
        "de" to mapOf(
            "menu" to "Menü", "settings" to "Einstellungen", "tools" to "Werkzeuge", "updates" to "Updates", "about" to "Über",
            "workspace" to "WORKSPACE", "choose" to "Wähle, was du brauchst", "workspace_desc" to "Schnelle Werkzeuge zum Bearbeiten, Konvertieren, Aufnehmen und Analysieren von Audio.",
            "active" to "AKTIV", "audio_workspace" to "AUDIO WORKSPACE", "local" to "LOKAL", "edit" to "Bearbeiten", "record" to "Aufnehmen", "analyze" to "Analysieren", "convert" to "Konvertieren",
            "open_tools" to "Werkzeuge öffnen", "quick_flow" to "SCHNELLER ABLAUF", "flow_title" to "Wählen  →  verarbeiten  →  speichern", "open" to "Öffnen", "process" to "Verarbeiten", "save" to "Speichern", "local_processing" to "Lokale Verarbeitung", "local_desc" to "Dateien werden auf dem Gerät verarbeitet.",
            "back" to "Zurück", "how" to "So funktioniert es", "compatibility" to "Kompatibilität", "input" to "Eingabe", "output" to "Ausgabe", "ready" to "Bereit",
            "choose_audio" to "Audio auswählen", "cut_action" to "Schneiden und speichern", "cut_bad" to "Wähle einen gültigen Bereich innerhalb der Audiodauer.", "cut_info" to "Ziehe die Griffe, um Anfang und Ende exakt festzulegen.",
            "cut_precise" to "PRÄZISIONSSCHNITT", "original" to "▶ Original abspielen", "result" to "▶ Ergebnis abspielen", "cutting" to "Audio wird geschnitten…", "cut_ok" to "Bereit", "cut_error" to "Schnitt fehlgeschlagen. Diese Version benötigt M4A/AAC.",
            "permission_mic" to "Das Mikrofon wird zum Aufnehmen benötigt.", "record_ready" to "Bereit zur Aufnahme.", "record_start" to "●  Aufnahme starten", "record_stop" to "■  Aufnahme stoppen",
            "record_play" to "▶ Letzte Aufnahme abspielen", "recording_saved" to "Aufnahme auf dem Gerät gespeichert.", "recording_error" to "Aufnahme konnte nicht gestartet werden.", "record_short" to "Die Aufnahme war zu kurz. Versuche es erneut.",
            "live" to "LIVE", "monitoring" to "ECHTZEIT-MONITOR", "report" to "TECHNISCHER BERICHT", "select_for_report" to "Wähle eine Audiodatei für den Bericht.", "analyze_action" to "Audio analysieren",
            "analysis_error" to "Datei konnte nicht analysiert werden.", "playing" to "Wiedergabe.", "completed" to "Wiedergabe abgeschlossen.", "not_available" to "Datei ist nicht mehr verfügbar.", "selected" to "Ausgewählt", "analyzing" to "Analyse",
            "duration" to "Dauer", "format" to "Format", "bitrate" to "Bitrate", "channels" to "Kanäle", "sample_rate" to "Abtastrate",
            "converter" to "Konverter", "converter_pill" to "LOKALE KONVERTIERUNG", "converter_desc" to "Wandle WAV und M4A/AAC direkt auf deinem Handy um.", "source_format" to "Quellformat", "target_format" to "Ausgabeformat",
            "convert_action" to "Konvertieren und speichern", "converted" to "Konvertierung abgeschlossen", "conversion_error" to "Konvertierung fehlgeschlagen. Nutze WAV oder M4A/AAC.", "supported" to "Unterstützt: WAV ↔ M4A/AAC",
            "converter_tip" to "Wähle ein Ausgabeformat. Audio wird lokal verarbeitet.", "audio_file" to "Audiodatei", "select_source" to "Datei auswählen", "conversion_progress" to "Konvertiere…",
            "language" to "Sprache", "language_desc" to "Übersetzt die gesamte Audio Tools-Oberfläche.", "choose_language" to "Sprache wählen",
            "update_section" to "UPDATES", "installed" to "Installierte Version", "check_update" to "Nach Updates suchen", "install_permission" to "Berechtigung für Updates",
            "auto_check" to "Beim Öffnen der Einstellungen prüfen", "already_latest" to "Du hast bereits die neueste Version.", "update_available" to "Update verfügbar", "download_install" to "Herunterladen und installieren",
            "download_started" to "Download gestartet. Nach Abschluss wird der Android-Installer geöffnet.", "release_missing" to "Ein neues Release existiert, aber noch kein APK ist angehängt.",
            "installer_permission" to "Erlaube zuerst dieser App, Updates zu installieren.", "permission_active" to "Berechtigung ist bereits aktiv.", "checking" to "Prüfe…", "download_error" to "Update konnte nicht heruntergeladen werden.",
            "release_open" to "Release öffnen", "app_section" to "APP", "about_title" to "Über Audio Tools", "about_desc" to "Fokussierte, schnelle lokale Audio-Werkzeuge mit unabhängigen Modulen für zukünftiges Wachstum.",
            "about_head" to "Für die Arbeit mit Audio gemacht.", "about_body" to "Schneide, konvertiere, nimm auf und analysiere, ohne deine Dateien an einen externen Verarbeitungsdienst zu senden.", "four_tools" to "4 funktionale Werkzeuge", "version" to "Version", "back_action" to "Zurück",
            "edit_tool" to "EDITOR", "convert_tool" to "KONVERTER", "record_tool" to "AUFNAHME", "analyze_tool" to "ANALYSE"
        )
    )

    fun t(context: Context, key: String): String =
        ui[LanguageManager.get(context)]?.get(key)
            ?: ui["pt"]?.get(key)
            ?: key

    fun tool(context: Context, id: String): ToolCopy {
        return when (id) {
            "cut" -> ToolCopy(
                when (LanguageManager.get(context)) { "en" -> "Cut audio"; "fr" -> "Couper l’audio"; "es" -> "Cortar audio"; "de" -> "Audio schneiden"; else -> "Cortar áudio" },
                when (LanguageManager.get(context)) {
                    "en" -> "Set a range visually, then export a clean M4A copy."
                    "fr" -> "Définis une plage visuellement puis exporte une copie M4A."
                    "es" -> "Define un intervalo visual y exporta una copia M4A."
                    "de" -> "Lege den Bereich visuell fest und exportiere eine M4A-Kopie."
                    else -> "Define um intervalo visual e exporta uma cópia M4A."
                },
                when (LanguageManager.get(context)) {
                    "en" -> "A focused editor with draggable handles and exact start/end values."
                    "fr" -> "Un éditeur simple avec poignées glissables et valeurs exactes."
                    "es" -> "Un editor sencillo con controles arrastrables y valores exactos."
                    "de" -> "Ein einfacher Editor mit ziehbaren Griffen und exakten Start-/Endwerten."
                    else -> "Um editor simples com pegas arrastáveis e valores exatos de início/fim."
                },
                t(context, "edit_tool"), "M4A / AAC", "M4A",
                when (LanguageManager.get(context)) {
                    "en" -> listOf("Choose an audio file", "Drag the start and end handles", "Fine-tune the values if needed", "Export the selected section")
                    "fr" -> listOf("Choisis un fichier audio", "Fais glisser les poignées", "Ajuste les valeurs si nécessaire", "Exporte la sélection")
                    "es" -> listOf("Elige un archivo de audio", "Arrastra los controles", "Ajusta los valores si es necesario", "Exporta la selección")
                    "de" -> listOf("Audiodatei auswählen", "Start- und Endgriff ziehen", "Werte bei Bedarf feinjustieren", "Auswahl exportieren")
                    else -> listOf("Escolhe um ficheiro de áudio", "Arrasta as pegas de início e fim", "Ajusta os valores se precisares", "Exporta o trecho selecionado")
                }
            )
            "convert" -> ToolCopy(
                t(context, "converter"), t(context, "converter_desc"),
                t(context, "converter_tip"), t(context, "convert_tool"),
                "WAV / M4A / AAC", "WAV / M4A (AAC)",
                when (LanguageManager.get(context)) {
                    "en" -> listOf("Choose a WAV or M4A/AAC file", "Select the output format", "Convert locally", "Save the new file")
                    "fr" -> listOf("Choisis un fichier WAV ou M4A/AAC", "Choisis le format de sortie", "Convertis localement", "Enregistre le nouveau fichier")
                    "es" -> listOf("Elige un archivo WAV o M4A/AAC", "Selecciona el formato de salida", "Convierte localmente", "Guarda el nuevo archivo")
                    "de" -> listOf("WAV oder M4A/AAC auswählen", "Ausgabeformat wählen", "Lokal konvertieren", "Neue Datei speichern")
                    else -> listOf("Escolhe WAV ou M4A/AAC", "Seleciona o formato de saída", "Converte localmente", "Guarda o novo ficheiro")
                }
            )
            "recorder" -> ToolCopy(
                if (LanguageManager.get(context) == "pt") "Gravador" else when (LanguageManager.get(context)) { "en" -> "Recorder"; "fr" -> "Enregistreur"; "es" -> "Grabadora"; else -> "Recorder" },
                if (LanguageManager.get(context) == "pt") "Grava voz, ideias, podcasts e takes com medição em tempo real." else when (LanguageManager.get(context)) {
                    "en" -> "Record voice, ideas, podcasts and takes with real-time monitoring."
                    "fr" -> "Enregistre voix, idées, podcasts et prises avec monitoring en temps réel."
                    "es" -> "Graba voz, ideas, podcasts y tomas con monitorización en tiempo real."
                    else -> "Nimm Sprache, Ideen, Podcasts und Takes mit Echtzeit-Monitoring auf."
                },
                if (LanguageManager.get(context) == "pt") "Gravador local M4A/AAC com medidor vivo e reprodução imediata." else when (LanguageManager.get(context)) {
                    "en" -> "Local M4A/AAC recorder with a live meter and instant playback."
                    "fr" -> "Enregistreur M4A/AAC local avec vumètre en direct et lecture instantanée."
                    "es" -> "Grabadora M4A/AAC local con medidor en vivo y reproducción inmediata."
                    else -> "Lokaler M4A/AAC-Recorder mit Live-Meter und direkter Wiedergabe."
                },
                if (LanguageManager.get(context) == "pt") "GRAVAÇÃO" else t(context, "record_tool"), "Microfone", "M4A / AAC",
                when (LanguageManager.get(context)) {
                    "en" -> listOf("Allow microphone access", "Start recording", "Watch the live meter", "Stop and replay")
                    "fr" -> listOf("Autorise le micro", "Démarre l’enregistrement", "Observe le vumètre", "Arrête et écoute")
                    "es" -> listOf("Permite el micrófono", "Empieza a grabar", "Observa el medidor", "Detén y reproduce")
                    "de" -> listOf("Mikrofon erlauben", "Aufnahme starten", "Live-Meter beobachten", "Stoppen und abspielen")
                    else -> listOf("Concede acesso ao microfone", "Começa a gravação", "Acompanha o medidor", "Para e reproduz")
                }
            )
            else -> ToolCopy(
                if (LanguageManager.get(context) == "pt") "Analisar áudio" else when (LanguageManager.get(context)) { "en" -> "Analyze audio"; "fr" -> "Analyser l’audio"; "es" -> "Analizar audio"; else -> "Audio analysieren" },
                if (LanguageManager.get(context) == "pt") "Consulta duração, formato, bitrate, canais e taxa de amostragem." else when (LanguageManager.get(context)) {
                    "en" -> "Inspect duration, format, bitrate, channels and sample rate."
                    "fr" -> "Consulte la durée, le format, le bitrate, les canaux et l’échantillonnage."
                    "es" -> "Consulta duración, formato, bitrate, canales y muestreo."
                    else -> "Prüfe Dauer, Format, Bitrate, Kanäle und Abtastrate."
                },
                if (LanguageManager.get(context) == "pt") "Leitura técnica do ficheiro sem envio para a internet." else when (LanguageManager.get(context)) {
                    "en" -> "Technical file inspection without uploading audio."
                    "fr" -> "Inspection technique du fichier sans envoi audio."
                    "es" -> "Inspección técnica del archivo sin subir el audio."
                    else -> "Technische Dateiprüfung ohne Audio-Upload."
                },
                t(context, "analyze_tool"), "Ficheiro de áudio", "Relatório técnico",
                when (LanguageManager.get(context)) {
                    "en" -> listOf("Choose a file", "Read its media metadata", "Review the report")
                    "fr" -> listOf("Choisis un fichier", "Lis ses métadonnées", "Consulte le rapport")
                    "es" -> listOf("Elige un archivo", "Lee sus metadatos", "Consulta el informe")
                    "de" -> listOf("Datei auswählen", "Metadaten lesen", "Bericht prüfen")
                    else -> listOf("Escolhe um ficheiro", "Lê os metadados", "Consulta o relatório")
                }
            )
        }
    }
}
