package app.minidaw.measure

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Hands one file of results to the app that the owner picks in the share sheet. It serves only files of
 * the folder of results, only for reading, and only with a permission that the share grants.
 */
class ResultsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = MIME

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") {
            throw FileNotFoundException("The files of results are read only")
        }
        val file = fileFor(uri) ?: throw FileNotFoundException("No such file of results")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val file = fileFor(uri) ?: return null
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cursor.addRow(arrayOf<Any>(file.name, file.length()))
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun fileFor(uri: Uri): File? {
        val name = uri.lastPathSegment
        val dir = context?.let { Results.directory(it) }
        val file = if (name != null && dir != null && NAME.matches(name)) File(dir, name) else null
        return file?.takeIf { it.isFile }
    }

    companion object {
        private const val MIME = "application/json"
        private val NAME = Regex("[A-Za-z0-9._-]+\\.json")

        fun uriFor(context: Context, name: String): Uri {
            val builder = Uri.Builder()
            builder.scheme("content")
            builder.authority(context.packageName + ".results")
            builder.appendPath(name)
            return builder.build()
        }
    }
}
