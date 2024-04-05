package pl.rodzon.chatwithme.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import android.util.Base64
import java.io.FileOutputStream


class ImageUtil {
    companion object {
        @Throws(IOException::class)
        fun readBytes(context: Context, uri: Uri): ByteArray? =
            context.contentResolver.openInputStream(uri)?.use { it.buffered().readBytes() }

        fun convertByteArrayToUri(byteArray: ByteArray): Uri {
            return Uri.parse(byteArray.contentToString())
        }

        fun prepareImagePart(partName: String, fileUri: Uri): MultipartBody.Part {
            // Przygotowanie pliku do wysłania jako Multipart
            val requestFile = fileUri.path!!.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData(partName, fileUri.lastPathSegment, requestFile)
        }

        fun saveBase64ImageToFile(context: Context, base64Image: String, fileName: String): File? {
            try {
                // Dekodowanie ciągu Base64 na tablicę bajtów
                val decodedBytes: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)

                // Utworzenie pliku w katalogu cache aplikacji
                val cacheDir = context.cacheDir
                val imageFile = File(cacheDir, fileName)

                // Zapisanie tablicy bajtów do pliku
                val outputStream = FileOutputStream(imageFile)
                outputStream.write(decodedBytes)
                outputStream.close()

                return imageFile
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun uriToFile(context: Context, uri: Uri): File? {
            val contentResolver: ContentResolver = context.contentResolver
            val filePath = getFilePathFromUri(context, uri)

            if (filePath != null) {
                return File(filePath)
            }

            val inputStream: InputStream? = try {
                contentResolver.openInputStream(uri)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

            if (inputStream != null) {
                val tempFile = createTempFile(context)
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return tempFile
            }

            return null
        }

        private fun getFilePathFromUri(context: Context, uri: Uri): String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            val columnIndex: Int? = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            val filePath: String? = columnIndex?.let { cursor?.getString(it) }
            cursor?.close()
            return filePath
        }

        private fun createTempFile(context: Context): File {
            val timeStamp = System.currentTimeMillis()
            val fileName = "temp_image_$timeStamp"
            val storageDir = context.cacheDir
            return File.createTempFile(fileName, null, storageDir)
        }
    }
}