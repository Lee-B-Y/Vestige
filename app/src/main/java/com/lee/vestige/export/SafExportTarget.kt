package com.lee.vestige.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local export target: writes into a user-chosen directory via the Storage Access
 * Framework. Works with Obsidian vault folders and needs no storage permission.
 *
 * The destination [treeUri] comes from `ACTION_OPEN_DOCUMENT_TREE` (persisted).
 */
class SafExportTarget(
    private val context: Context,
    private val treeUri: Uri,
) : ExportTarget {

    override val displayName: String = "本地目录"

    override suspend fun export(
        fileName: String,
        content: String,
        overwrite: Boolean,
    ): ExportResult = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ExportResult.Error("无法访问所选目录，请重新选择")

        val existing = dir.findFile(fileName)
        if (existing != null) {
            if (!overwrite) return@withContext ExportResult.AlreadyExists(fileName)
            if (!existing.delete()) {
                return@withContext ExportResult.Error("无法覆盖已存在的文件")
            }
        }

        val file = dir.createFile(MIME_MARKDOWN, fileName)
            ?: return@withContext ExportResult.Error("创建文件失败")

        runCatching {
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("无法打开输出流")
        }.fold(
            onSuccess = { ExportResult.Success(fileName) },
            onFailure = { ExportResult.Error(it.message ?: "写入失败") },
        )
    }

    companion object {
        private const val MIME_MARKDOWN = "text/markdown"
    }
}
