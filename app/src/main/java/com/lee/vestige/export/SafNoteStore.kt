package com.lee.vestige.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Local note store: reads/writes Markdown under a user-chosen directory via the Storage
 * Access Framework, organized into year/month subfolders (created on demand). Needs no
 * storage permission and works with Obsidian vault folders.
 *
 * [treeUri] comes from `ACTION_OPEN_DOCUMENT_TREE` (persisted).
 */
class SafNoteStore(
    private val context: Context,
    private val treeUri: Uri,
) : NoteStore {

    override val displayName: String = "本地目录"

    override suspend fun read(date: LocalDate): String? = withContext(Dispatchers.IO) {
        val dir = navigate(date, create = false) ?: return@withContext null
        val file = dir.findFile(NotePath.fileName(date))?.takeIf { it.isFile }
            ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.bufferedReader().readText()
            }
        }.getOrNull()
    }

    override suspend fun write(date: LocalDate, content: String): SaveResult =
        withContext(Dispatchers.IO) {
            val dir = navigate(date, create = true)
                ?: return@withContext SaveResult.Error("无法访问或创建保存目录")
            val name = NotePath.fileName(date)
            val file = dir.findFile(name)?.takeIf { it.isFile }
                ?: dir.createFile(MIME_MARKDOWN, name)
                ?: return@withContext SaveResult.Error("创建文件失败")

            runCatching {
                // "wt" truncates so an existing file is replaced, not appended to.
                context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("无法打开输出流")
            }.fold(
                onSuccess = { SaveResult.Success },
                onFailure = { SaveResult.Error(it.message ?: "写入失败") },
            )
        }

    /**
     * Walks (and optionally creates) the year/month subfolders under the tree root,
     * returning the leaf directory, or `null` on failure.
     */
    private fun navigate(date: LocalDate, create: Boolean): DocumentFile? {
        var dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        for (name in NotePath.folders(date)) {
            val existing = dir.findFile(name)
            dir = when {
                existing != null && existing.isDirectory -> existing
                existing != null -> return null // name taken by a file — give up
                create -> dir.createDirectory(name) ?: return null
                else -> return null
            }
        }
        return dir
    }

    companion object {
        private const val MIME_MARKDOWN = "text/markdown"
    }
}
