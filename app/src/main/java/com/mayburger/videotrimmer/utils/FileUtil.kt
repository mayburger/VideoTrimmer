package com.mayburger.videotrimmer.utils

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.provider.OpenableColumns
import android.text.TextUtils
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * Created by adityanaufal on 9/26/18.
 */

object FileUtil {
    fun getPath(context: Context, fileUri: Uri): String? {
        // SDK >= 11 && SDK < 19
        return if (Build.VERSION.SDK_INT < 19) {
            getRealPathFromURIAPI11to18(context, fileUri)
        } else {
            getRealPathFromURIAPI19(context, fileUri)
        }// SDK > 19 (Android 4.4) and up
    }

    fun getVideoDuration(context:Context, fileUri:Uri):Long?{
        val mFFmpegMediaMetadataRetriever: FFmpegMediaMetadataRetriever = FFmpegMediaMetadataRetriever()
        mFFmpegMediaMetadataRetriever.setDataSource(fileUri.toString())
        val mVideoDuration: String =
            mFFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)
        return mVideoDuration.toLong()
    }

    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI11to18(context: Context, contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null

        val cursorLoader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()

        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            result = cursor.getString(columnIndex)
            cursor.close()
        }
        return result
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author Niks
     */
    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI19(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    cursor!!.moveToNext()
                    val fileName = cursor.getString(0)
                    val path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } finally {
                    cursor?.close()
                }
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads"), java.lang.Long.valueOf(id))

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
            return when {
                isGooglePhotosUri(uri) -> uri.lastPathSegment
                isGoogleDriveUri(uri) == true -> saveFileIntoExternalStorageByUri(context, uri)
                isDropBoxFileCacheUri(uri) == true -> saveFileIntoExternalStorageByUri(context, uri)
                isDropBoxUri(uri) == true -> uri.path
                else -> getDataColumn(context, uri, null, null)
            }
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Niks
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Drive.
     */
    private fun isGoogleDriveUri(uri: Uri): Boolean? {
        return uri.authority?.contains("com.google.android.apps.docs.storage")
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DropBox.
     */
    private fun isDropBoxUri(uri: Uri): Boolean? {
        return uri.path?.contains("com.dropbox.android")
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DropBoxFileCache.
     */
    private fun isDropBoxFileCacheUri(uri: Uri): Boolean? {
        return uri.authority?.contains("com.dropbox.android")
    }

    fun getFileNameOrExtension(context: Context, uri: Uri?, isExtension: Boolean = false): String{

        var name = ""

        uri?.let {
            if (isDropBoxUri(it) == true) {
                name = it.lastPathSegment.toString()
                if (isExtension) {
                    name = name.substring(name.lastIndexOf('.') + 1, name.length)
                }
            } else {
                val returnCursor = context.contentResolver.query(uri, null, null, null, null)
                try {
                    val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    returnCursor?.moveToFirst()
                    name = nameIndex?.let { it1 -> returnCursor?.getString(it1) }.toString()
                    if (isExtension) {
                        name = name.substring(name.lastIndexOf('.') + 1, name.length)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    returnCursor?.close()
                }
            }
        }

        return name
    }

    @Throws(Exception::class)
    fun saveFileIntoExternalStorageByUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalSize = inputStream?.available()

        val bis: BufferedInputStream?
        val bos: BufferedOutputStream?
        val fileName = getFileNameOrExtension(context, uri)
        val file = makeEmptyFileIntoExternalStorageWithTitle(fileName)
        bis = BufferedInputStream(inputStream)
        bos = BufferedOutputStream(FileOutputStream(
                file, false))

        val buf = originalSize?.let { ByteArray(it) }
        bis.read(buf)
        do {
            bos.write(buf)
        } while (bis.read(buf) != -1)

        bos.run {
            flush()
            close()
        }
        bis.close()

        return file.absolutePath
    }

    private fun makeEmptyFileIntoExternalStorageWithTitle(title: String): File {
        val root = Environment.getExternalStorageDirectory().absolutePath
        return File(root, title)
    }

    fun viewFileIntent(context: Context, fileUrl : String){
        val intent = Intent(Intent.ACTION_VIEW)
        if (fileUrl.endsWith(".png", true) || fileUrl.endsWith(".jpg", true)) {
            val packageName = getPackageForGalery(context, "image/*", MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.setDataAndType(Uri.parse(fileUrl), "image/*")
            if(packageName != null) intent.setPackage(packageName)
        } else if (fileUrl.endsWith(".mp4", true)) {
            val packageName = getPackageForGalery(context, "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            intent.setDataAndType(Uri.parse(fileUrl), "video/mp4")
            if(packageName != null) intent.setPackage(packageName)
        } else {
            intent.data = Uri.parse(fileUrl)
        }
        context.startActivity(intent)
    }

    fun downloadFileIntent(context: Context, fileUrl: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(fileUrl)
        context.startActivity(intent)
    }


    fun getPackageForGalery(context: Context, type: String, uri: Uri): String? {
        val mainIntent = Intent(Intent.ACTION_PICK, uri)
        mainIntent.type = type
        val pkgAppsList = context.packageManager.queryIntentActivities(mainIntent, PackageManager.GET_RESOLVED_FILTER)
        for (infos in pkgAppsList) {
            return infos.activityInfo.processName
        }
        return null
    }

    fun getRealSizeFromUri(context: Context, uri: Uri): String {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Audio.Media.SIZE)
            cursor = context.contentResolver.query(uri, proj, null, null, null)
            val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            cursor?.moveToFirst()
            return column_index?.let { cursor?.getString(it) } ?:""
        } finally {
            cursor?.close()
        }
    }

    fun convertFileSize(size: String): String {
        var sizeName: String? = null
        val kiloByte = 1000
        val megaByte = 1000000
        val sizeMega = size.toInt() / megaByte.toFloat()
        val sizeKilo = size.toInt() / kiloByte.toFloat()
        if (sizeMega >= 1) {
            sizeName = String.format(Locale.getDefault(), "%.0f MB", sizeMega)
        } else if (sizeKilo >= 1) {
            sizeName = String.format(Locale.getDefault(), "%.0f KB", sizeKilo)
        }

        return sizeName?:""
    }

    fun getDirectoryPaths(directory: String): ArrayList<String> {
        val pathArray = ArrayList<String>()
        val file = File(directory)
        val listfiles: Array<File>? = file.listFiles()
        listfiles?.let { listfiles ->
            for (i in listfiles.indices) {
                if (listfiles[i].isDirectory) {
                    if(listfiles[i].list().isNotEmpty()){
                        listfiles[i].absolutePath.let { path->
                            if(isMediaDirectory(path)) pathArray.add(path)
                        }
                    }
                }
            }
        }
        return pathArray
    }

    fun getDirectoryFile(directory: String, isMultiple : Boolean): ArrayList<File> {
        val files = ArrayList<File>()
        val file = File(directory)
        val listfiles = file.listFiles()
        listfiles?.let { listfiles ->
            listfiles.indices.forEach { i ->
                val file = listfiles[i]
                val fileName = file.name
                val isValid = if (isMultiple) isImageFile(fileName) else isMediaFile(fileName)
                if (file.isFile && isValid) {
                    files.add(listfiles[i])
                }

            }
        }
        files.sortByDescending { it.lastModified() }
        return files
    }

    fun isMediaDirectory(directory: String): Boolean{
        val file = File(directory)
        val listfiles = file.listFiles()
        listfiles?.let { listfiles ->
            for (i in listfiles.indices) {
                val file = listfiles[i]
                val fileName = file.name
                val isValid = isMediaFile(fileName)
                if (isValid) {
                    return true
                }
            }
        }

        return false
    }

    fun getDirectoryFileByCursor(contentResolver: ContentResolver?, isImage: Boolean, isMultiple: Boolean, cursorId: String, cursorData: String, cursorContentUri: Uri): ArrayList<File>{

        val files = ArrayList<File>()

        val PROJECTION =
            if(isImage) arrayOf(cursorId, cursorData, MediaStore.Images.Media.ORIENTATION)
            else arrayOf(cursorId, cursorData)

        var imageCursor: Cursor? = null

        try {
            val orderBy = if(isImage) MediaStore.Images.Media.DATE_TAKEN + " DESC" else MediaStore.Video.Media.DATE_TAKEN + " DESC"

            imageCursor = contentResolver?.query(cursorContentUri, PROJECTION, null, null,
                    orderBy)


            if (imageCursor != null) {
                while (imageCursor.moveToNext()) {
                    val path = imageCursor.getString(imageCursor.getColumnIndex(cursorData))
                    if ((isImage && isImageFile(path) || (!isImage && isVideoFile(path)))){
                    files.add(File(path))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (imageCursor != null && !imageCursor.isClosed) {
                imageCursor.close()
            }

        }

        return files
    }

    @Throws(IOException::class)
    fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun isMediaFile(fileName: String?) : Boolean{
        val extension = fileName?.substring(fileName.lastIndexOf('.') + 1, fileName.length)
        return extension == "jpg" || extension == "png" || extension == "JPG" || extension == "PNG" || extension == "mp4" || extension == "MP4"
    }

    fun isVideoFile(fileName: String?) : Boolean{
        val extension = fileName?.substring(fileName.lastIndexOf('.') + 1, fileName.length)
        return extension == "mp4" || extension == "MP4"
    }

    fun isImageFile(fileName: String?) : Boolean{
        val extension = fileName?.substring(fileName.lastIndexOf('.') + 1, fileName.length)
        return extension == "jpg" || extension == "png" || extension == "JPG" || extension == "PNG" || extension == "jpeg" || extension == "JPEG"
    }
    fun isGifFile(fileName: String?) : Boolean{
        val extension = fileName?.substring(fileName.lastIndexOf('.') + 1, fileName.length)
        return extension == "gif"
    }


    fun getVideoLength(contentResolver: ContentResolver?, uri: Uri?): String {
        val cursor = MediaStore.Video.query(contentResolver, uri, arrayOf(MediaStore.Video.VideoColumns.DURATION))
        var duration: Long = 0
        if (cursor != null && cursor.moveToFirst()) {
            duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            cursor.close()
        }
        return String.format("%02d : %02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))
    }

    fun getVideoId(videoFilePath: String, contentResolver: ContentResolver): Long {
        val SELECTION = MediaColumns.DATA + "=?"
        val PROJECTION = arrayOf(BaseColumns._ID)
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selectionArgs = arrayOf(videoFilePath)
        val cursor = contentResolver.query(uri, PROJECTION, SELECTION, selectionArgs, null)
        var videoId: Long = 0
        if (cursor != null  && cursor.moveToFirst()) {
            videoId = cursor.getLong(0)
        }
        cursor?.close()
        return videoId
    }
}

