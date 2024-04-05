package pl.rodzon.chatwithme.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import org.json.JSONObject
import pl.rodzon.chatwithme.model.users.UserJson
import java.io.IOException

class LocalStorageAccess {
    companion object {

        fun saveLoggedUserInformationToLocalStorage(context: Context, fileName: String, userJson: UserJson): Boolean {
            return try {
                context.openFileOutput(fileName, AppCompatActivity.MODE_PRIVATE).use { stream ->
                    val contentBytes = Gson().toJson(userJson).toString().toByteArray()
                    stream.write(contentBytes)
                    true
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        fun savePrivateKeyToInternalStorage(context: Context, fileName: String, content: String): Boolean {
            return try {
                context.openFileOutput(fileName, AppCompatActivity.MODE_PRIVATE).use { stream ->
                    val contentBytes = content.toByteArray()
                    stream.write(contentBytes)
                    true
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        fun deleteFileFromInternalStorage(context: Context, fileName: String): Boolean {
            return context.deleteFile(fileName)
        }

        fun doesFileExistInInternalStorage(context: Context, fileName: String): Boolean {
            val file = context.getFileStreamPath(fileName)
            return file.exists()
        }

        fun loadPrivateKeyFromInternalStorage(context: Context, fileName: String): String {
            return context.openFileInput(fileName).use { stream ->
                stream.bufferedReader().use {
                    it.readText()
                }
            }
        }

        fun loadUserInformationFromInternalStorage(context: Context, userInfo: String): String {
            return context.openFileInput(userInfo).use { stream ->
                stream.bufferedReader().use {
                    it.readText()
                }
            }
        }
    }
}