package pl.rodzon.chatwithme.activities.call_screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.json.JSONObject
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatActivity
import pl.rodzon.chatwithme.api_interface.UserApiInterface
import pl.rodzon.chatwithme.model.call.CallingInfo
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.LocalStorageAccess
import pl.rodzon.chatwithme.utils.MyBackgroundService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.naiksoftware.stomp.StompClient
import java.net.URL
import java.time.LocalDateTime
import java.util.*

class IncomingInvitationActivity : AppCompatActivity() {
    private lateinit var ringtone: Ringtone
    private val stompClientCall: StompClient = ChatContext.getStompClientCall()
    private val compositeDisposable = CompositeDisposable()
    private lateinit var currentLoggedUser: String
    private lateinit var roomId: String
    private lateinit var callProvider: String
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_invitation)

        val filter = IntentFilter(MyBackgroundService.INCOMING_CALL_ACTIVITY)
        registerReceiver(dataReceiver, filter)

        roomId = UUID.randomUUID().toString()

        val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, defaultRingtoneUri)
        ringtone.play()

        this.username = intent.getStringExtra("username")!!
        currentLoggedUser = intent.getStringExtra("currentLoggedUser")!!
        val type = intent.getStringExtra("type")
        callProvider = intent.getStringExtra("callProvider")!!

        val typeImageView: ImageView = findViewById(R.id.imageMeetingType)
        val rejectImageView: ImageView = findViewById(R.id.imageRejectInvitation)
        val acceptImageView: ImageView = findViewById(R.id.imageAcceptInvitation)
        val textFirstChar: TextView = findViewById(R.id.textFirstChar)
        val textUsername: TextView = findViewById(R.id.textUsername)

        textFirstChar.text = username[0].toString()
        textUsername.text = username

        if (type != null) {
            if (type == "video") {
                typeImageView.setImageResource(R.drawable.ic_video)
            } else {
                typeImageView.setImageResource(R.drawable.ic_audio)
            }
        }

        acceptImageView.setOnClickListener {
            sendInformationAboutCallingToSender(CallingInfo("accept", username, roomId, callProvider), username)
            ringtone.stop()

            if (callProvider == "Jitsi meet") {
                val serverURL = URL("https://jitsi.belnet.be")
                val options = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setRoom(roomId)
                    .setAudioMuted(true)
                    .setVideoMuted(true)
                    .build()

                JitsiMeetActivity.launch(this@IncomingInvitationActivity, options)
                finish()
            } else if (callProvider == "pl.rodzon") {
                val intent = Intent(this, CallActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("username", username)
                intent.putExtra("currentLoggedUser", currentLoggedUser)
                startActivity(intent)
                finish()
            }
        }

        rejectImageView.setOnClickListener {
            sendInformationAboutCallingToSender(CallingInfo("reject", username, roomId, ""), username)
            val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(this@IncomingInvitationActivity, "user.txt")
            val userJSONObject = JSONObject(userString)
            val userJson = UserJson(
                userJSONObject.getString("userID"),
                userJSONObject.getString("username"),
                userJSONObject.getString("publicKey"),
                userJSONObject.getString("picture")
            )
            ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@IncomingInvitationActivity, userJson.username + "_private_key.txt"))

            getReceiverUserFromServer(username, "sendMessageCantTalk","Sorry I can't talk right now. Please try catching me later.")
        }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendInformationAboutCallingToSender(CallingInfo("cancel", username, roomId, ""), username)
                finish()
            }
        })

    }

    private fun callCanceled() {
        ringtone.stop()
        val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(this@IncomingInvitationActivity, "user.txt")
        val userJSONObject = JSONObject(userString)
        val userJson = UserJson(
            userJSONObject.getString("userID"),
            userJSONObject.getString("username"),
            userJSONObject.getString("publicKey"),
            userJSONObject.getString("picture")
        )
        ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@IncomingInvitationActivity, userJson.username + "_private_key.txt"))

        /*val messageMissedCall = "\u2199    Missed call from $username at:" + LocalDateTime.now().toString()
            .substring(0, 19).replace("T", " ") + "    \u2199"

        MyBackgroundService.notificationHelper.createNotification("my_channel_id", username, messageMissedCall, username)*/
        finish()
    }

    private fun sendInformationAboutCallingToSender(body: Any, username: String) {
        stompClientCall.send("/app/call/audio/${username}", Gson().toJson(body))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Obsługa wysłania wiadomości */ },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun getReceiverUserFromServer(username: String, messageTextKeyInIntent: String, messageText: String) {
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
                    val userReceiverFromServer = UserJson(
                        responseBody!!.userID,
                        responseBody.username,
                        responseBody.publicKey,
                        responseBody.picture
                    )
                    val intentToChat = Intent(this@IncomingInvitationActivity, MessageChatActivity::class.java)
                    intentToChat.putExtra("currentLoggedUser", getCurrentLoggedUser() as java.io.Serializable)
                    intentToChat.putExtra("user", userReceiverFromServer as java.io.Serializable)
                    intentToChat.putExtra(messageTextKeyInIntent, messageText)
                    intentToChat.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intentToChat)
                    finish()
                } else {
                    Toast.makeText(
                        this@IncomingInvitationActivity,
                        "An error occurred on sending message process. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UserJson?>, t: Throwable) {
                println(t.message)

                Toast.makeText(
                    this@IncomingInvitationActivity,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun getCurrentLoggedUser(): UserJson {
        val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(
            this@IncomingInvitationActivity,
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

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MyBackgroundService.INCOMING_CALL_ACTIVITY) {
                val receivedData = intent.getStringExtra("message")
                if (receivedData == "cancel") {
                    callCanceled()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        ringtone.stop()
    }
}