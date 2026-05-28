package com.dyremark.dailywallpaper.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists images inside a user-picked SAF tree and picks one at random.
 *
 * Designed to scale to folders with thousands of images: it queries the children with a raw
 * [android.content.ContentResolver] cursor projecting only the document id + mime type (never the
 * slow [DocumentFile.listFiles] wrappers), and only collects lightweight id strings.
 */
class FolderRepository(private val context: Context) {

    /** Human-readable name of the picked folder, or null if it can't be resolved. */
    fun folderName(treeUri: Uri): String? =
        DocumentFile.fromTreeUri(context, treeUri)?.name

    /** Number of images in the folder. */
    suspend fun imageCount(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        collectImageDocumentIds(treeUri).size
    }

    /**
     * Picks a random image document URI from the folder, avoiding [exclude] when the folder holds
     * more than one image. Returns null if the folder has no images.
     */
    suspend fun pickRandomImage(treeUri: Uri, exclude: Uri?): Uri? = withContext(Dispatchers.IO) {
        val ids = collectImageDocumentIds(treeUri)
        if (ids.isEmpty()) return@withContext null

        var chosen = ids[ids.indices.random()]
        if (ids.size > 1 && exclude != null) {
            val excludedId = runCatching { DocumentsContract.getDocumentId(exclude) }.getOrNull()
            // Re-roll a few times to dodge an immediate repeat without scanning the whole list.
            var attempts = 0
            while (chosen == excludedId && attempts < 5) {
                chosen = ids[ids.indices.random()]
                attempts++
            }
        }
        DocumentsContract.buildDocumentUriUsingTree(treeUri, chosen)
    }

    private fun collectImageDocumentIds(treeUri: Uri): List<String> {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val ids = ArrayList<String>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol)
                if (mime != null && mime.startsWith("image/")) {
                    ids.add(cursor.getString(idCol))
                }
            }
        }
        return ids
    }
}
