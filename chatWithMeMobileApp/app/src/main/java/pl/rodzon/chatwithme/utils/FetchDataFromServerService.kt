package pl.rodzon.chatwithme.utils

import android.app.IntentService
import android.content.Intent
import android.widget.Toast
import org.json.JSONObject
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatActivity
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.api_interface.UserApiInterface
import pl.rodzon.chatwithme.model.users.UserJson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class FetchDataFromServerService : IntentService("FetchDataFromServerService") {
    private lateinit var userReceiverFromServer: UserJson

    override fun onHandleIntent(intent: Intent?) {
        if (intent!!.getStringExtra("main") == "main") {
            val intentToMain = Intent(applicationContext, MainActivity::class.java)
            val currentLoggedUser = getCurrentLoggedUser()
            MyBackgroundService.notificationHelper.removeAllNotificationsOfUser(currentLoggedUser.username)
            intentToMain.putExtra("user", currentLoggedUser as java.io.Serializable)
            intentToMain.putExtra("fromNotification", "true")
            intentToMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            applicationContext.startActivity(intentToMain)
        } else {
            val userReceiverString = intent.getStringExtra("username")
            this.getReceiverUserFromServer(userReceiverString!!)
        }
    }

    private fun getCurrentLoggedUser(): UserJson {
        val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(
            applicationContext,
            "user.txt"
        )
        val userJSONObject = JSONObject(userString)

        val userJson = UserJson(
            userJSONObject.getString("userID"),
            userJSONObject.getString("username"),
            userJSONObject.getString("publicKey"),
            userJSONObject.getString("picture")
        )
        ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(applicationContext, userJson.username + "_private_key.txt"))
        return userJson
    }

    private fun getReceiverUserFromServer(username: String) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(UserApiInterface::class.java)

        val retrofitData = retrofitBuilder.getUserByUsername(username)

        retrofitData.enqueue(object : Callback<UserJson?> {
            override fun onResponse(call: Call<UserJson?>, response: Response<UserJson?>) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    userReceiverFromServer = UserJson(
                        responseBody!!.userID,
                        responseBody.username,
                        responseBody.publicKey,
                        responseBody.picture
                    )
                    MyBackgroundService.notificationHelper.removeAllNotificationsOfUser(userReceiverFromServer.username)
                    val intentToChat = Intent(applicationContext, MessageChatActivity::class.java)
                    intentToChat.putExtra("currentLoggedUser", getCurrentLoggedUser() as java.io.Serializable)
                    intentToChat.putExtra("user", userReceiverFromServer as java.io.Serializable)
                    intentToChat.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    applicationContext.startActivity(intentToChat)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "An error occurred on login process. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UserJson?>, t: Throwable) {
                println(t.message)

                Toast.makeText(
                    applicationContext,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }


}
