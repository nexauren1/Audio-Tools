package com.nexauren.audiotools.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

data class SavedAudio(
    val uri: Uri,
    val name: String
)

object NexaurenStorage {
    private const val PREFS = "nexauren_storage"
    private const val TREE_URI = "tree_uri"

    fun getTreeUri(context: Context): Uri? =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getString(TREE_URI, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)

    fun rememberTree(
        context: Context,
        uri: Uri
    ) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            IntentFlags.READ_WRITE
        )

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                TREE_URI,
                uri.toString()
            )
            .apply()
    }

    fun clearTree(context: Context) {
        getTreeUri(context)?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    IntentFlags.READ_WRITE
                )
            }
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(TREE_URI)
            .apply()
    }

    fun hasTree(context: Context): Boolean =
        getTreeUri(context) != null

    fun saveAudio(
        context: Context,
        toolName: String,
        source: File,
        targetName: String,
        mimeType: String
    ): SavedAudio {
        require(source.exists()) {
            "O resultado ainda não está disponível."
        }

        val treeUri =
            getTreeUri(context)
                ?: throw IllegalStateException(
                    "Escolhe primeiro a pasta do Audio Tools."
                )

        val rootDocumentId =
            DocumentsContract.getTreeDocumentId(
                treeUri
            )

        val rootDocumentUri =
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                rootDocumentId
            )

        val folderName =
            safeName(toolName) +
                " - Nexauren"

        val folderUri =
            findChildDirectory(
                context,
                treeUri,
                rootDocumentUri,
                folderName
            )
                ?: DocumentsContract.createDocument(
                    context.contentResolver,
                    rootDocumentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    folderName
                )
                ?: throw IllegalStateException(
                    "Não foi possível criar a pasta da ferramenta."
                )

        val safeTarget =
            uniqueName(
                context,
                treeUri,
                folderUri,
                safeName(targetName)
            )

        val outputUri =
            DocumentsContract.createDocument(
                context.contentResolver,
                folderUri,
                mimeType,
                safeTarget
            )
                ?: throw IllegalStateException(
                    "Não foi possível criar o ficheiro de áudio."
                )

        try {
            context.contentResolver
                .openOutputStream(outputUri)
                ?.use { output ->
                    source.inputStream().use { input ->
                        input.copyTo(output)
                    }
                    output.flush()
                }
                ?: throw IllegalStateException(
                    "Não foi possível escrever o resultado."
                )
        } catch (error: Exception) {
            runCatching {
                DocumentsContract.deleteDocument(
                    context.contentResolver,
                    outputUri
                )
            }
            throw error
        }

        return SavedAudio(
            uri = outputUri,
            name = safeTarget
        )
    }

    fun rootLabel(context: Context): String {
        val uri =
            getTreeUri(context)
                ?: return "Ainda não escolhida"

        val documentId =
            runCatching {
                DocumentsContract.getTreeDocumentId(uri)
            }.getOrDefault("")

        return documentId
            .substringAfter(
                ':',
                documentId
            )
            .replace(
                '%',
                ' '
            )
            .ifBlank {
                "Armazenamento partilhado"
            }
    }

    private fun findChildDirectory(
        context: Context,
        treeUri: Uri,
        parentUri: Uri,
        name: String
    ): Uri? {
        val parentId =
            DocumentsContract.getDocumentId(
                parentUri
            )

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentId
            )

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex =
                cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )

            val nameIndex =
                cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )

            val mimeIndex =
                cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )

            while (cursor.moveToNext()) {
                val childName =
                    cursor.getString(nameIndex)
                        ?: continue

                val mime =
                    cursor.getString(mimeIndex)
                        ?: ""

                if (
                    childName == name &&
                    mime ==
                        DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    val id =
                        cursor.getString(idIndex)

                    return DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        id
                    )
                }
            }
        }

        return null
    }

    private fun uniqueName(
        context: Context,
        treeUri: Uri,
        parentUri: Uri,
        name: String
    ): String {
        val existing =
            mutableSetOf<String>()

        val parentId =
            DocumentsContract.getDocumentId(
                parentUri
            )

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentId
            )

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val index =
                cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )

            while (cursor.moveToNext()) {
                cursor.getString(index)
                    ?.let(existing::add)
            }
        }

        if (!existing.contains(name)) {
            return name
        }

        val dot =
            name.lastIndexOf('.')

        val stem =
            if (dot > 0) {
                name.substring(0, dot)
            } else {
                name
            }

        val extension =
            if (dot > 0) {
                name.substring(dot)
            } else {
                ""
            }

        var counter = 2

        while (
            existing.contains(
                "$stem ($counter)$extension"
            )
        ) {
            counter++
        }

        return "$stem ($counter)$extension"
    }

    private fun safeName(value: String): String =
        value.trim()
            .replace(
                Regex("[\\/:*?\"<>|]"),
                "_"
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .take(80)
            .ifBlank {
                "Audio"
            }

    private object IntentFlags {
        const val READ_WRITE =
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
