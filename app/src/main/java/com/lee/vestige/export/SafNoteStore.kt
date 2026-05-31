package com.lee.vestige.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Local note store: reads/writes/lists Markdown under a user-chosen directory via the
 * Storage Access Framework. Notes live in `Vestige/年/月/日.md` (folders created on
 * demand). Needs no storage permission and works with Obsidian vault folders.
 *
 * [treeUri] comes from `ACTION_OPEN_DOCUMENT_TREE` (persisted).
 *
 * Note: enumeration/search use [DocumentFile], which is slow per-call; this is fine for
 * the MVP's expected note counts. Callers run these off the main thread already.
 */
class SafNoteStore(
    private val context: Context,
    private val treeUri: Uri,
) : NoteStore {

    override val displayName: String = "本地目录"

    override suspend fun read(date: LocalDate): String? = withContext(Dispatchers.IO) {
        val dir = navigate(NotePath.folders(date), create = false) ?: return@withContext null
        val file = dir.findFile(NotePath.fileName(date))?.takeIf { it.isFile }
            ?: return@withContext null
        readText(file)
    }

    override suspend fun write(date: LocalDate, content: String): SaveResult =
        withContext(Dispatchers.IO) {
            val dir = navigate(NotePath.folders(date), create = true)
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

    override suspend fun list(): List<NoteListItem> = withContext(Dispatchers.IO) {
        allNoteFiles()
            .map { NoteListItem(it.first) }
            .sortedByDescending { it.date }
    }

    override suspend fun search(keyword: String): List<NoteListItem> = withContext(Dispatchers.IO) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return@withContext list()

        allNoteFiles().mapNotNull { (date, file) ->
            val content = readText(file).orEmpty()
            val inContent = content.contains(kw, ignoreCase = true)
            val inDate = date.toString().contains(kw, ignoreCase = true)
            if (inContent || inDate) NoteListItem(date, snippet(content, kw)) else null
        }.sortedByDescending { it.date }
    }

    /** Walk base → year → month, collecting (date, file) for every valid note. */
    private fun allNoteFiles(): List<Pair<LocalDate, DocumentFile>> {
        val base = navigate(listOf(NotePath.BASE), create = false) ?: return emptyList()
        val result = mutableListOf<Pair<LocalDate, DocumentFile>>()
        for (yearDir in base.listFiles()) {
            if (!yearDir.isDirectory) continue
            for (monthDir in yearDir.listFiles()) {
                if (!monthDir.isDirectory) continue
                for (file in monthDir.listFiles()) {
                    if (!file.isFile) continue
                    val date = file.name?.let(NotePath::parseDate) ?: continue
                    result += date to file
                }
            }
        }
        return result
    }

    /**
     * Walks (and optionally creates) the given subfolders under the tree root,
     * returning the leaf directory, or `null` on failure.
     */
    private fun navigate(folders: List<String>, create: Boolean): DocumentFile? {
        var dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        for (name in folders) {
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

    private fun readText(file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use { it.bufferedReader().readText() }
    }.getOrNull()

    /** A one-line snippet of context around the first match (or the opening text). */
    private fun snippet(content: String, keyword: String): String {
        val flat = content.replace('\n', ' ').replace('\r', ' ')
        val idx = flat.indexOf(keyword, ignoreCase = true)
        if (idx < 0) return flat.trim().take(SNIPPET_LEN)
        val start = (idx - SNIPPET_PAD).coerceAtLeast(0)
        val end = (idx + keyword.length + SNIPPET_PAD).coerceAtMost(flat.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < flat.length) "…" else ""
        return prefix + flat.substring(start, end).trim() + suffix
    }

    companion object {
        private const val MIME_MARKDOWN = "text/markdown"
        private const val SNIPPET_LEN = 60
        private const val SNIPPET_PAD = 24
    }
}
