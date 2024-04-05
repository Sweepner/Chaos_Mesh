package pl.rodzon.chatwithme.activities.call_screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatActivity
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.api_interface.UserApiInterface
import pl.rodzon.chatwithme.model.call.CallingInfo
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.LocalStorageAccess
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.naiksoftware.stomp.StompClient
import kotlin.math.roundToInt

class CallActivity : AppCompatActivity() {
    private val stompClientCall: StompClient = ChatContext.getStompClientCall()
    private lateinit var currentLoggedUser: String
    private lateinit var username: String
    private lateinit var serviceTimerIntent: Intent
    private lateinit var serviceSendAudioIntent: Intent
    private lateinit var serviceReceiveAudioIntent: Intent
    private var time = 0.0
    private lateinit var timerTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        this.currentLoggedUser = intent.getStringExtra("currentLoggedUser")!!
        this.username = intent.getStringExtra("username")!!

        subscribeCallingAudioTopic()

        this.timerTextView = findViewById(R.id.call_timer)

        serviceTimerIntent = Intent(applicationContext, TimerService::class.java)
        serviceSendAudioIntent = Intent(applicationContext, SendAudioService()::class.java)
        serviceReceiveAudioIntent = Intent(applicationContext, ReceiveAudioService()::class.java)
        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED))


        val stopCall: ImageView = findViewById(R.id.call_end)
        val mic: ImageView = findViewById(R.id.call_microphone)
        val textFirstChar: TextView = findViewById(R.id.textFirstChar)
        val textUsername: TextView = findViewById(R.id.textUsername)

        textFirstChar.text = username[0].toString()
        textUsername.text = username

        mic.setOnClickListener {
            if (ChatContext.isIsMicrophoneMuted()) {
                mic.setImageResource(R.drawable.ic_call_mic_on)
                ChatContext.setIsMicrophoneMuted(false)
                serviceSendAudioIntent.putExtra("username", username)
                startService(serviceSendAudioIntent)
            } else {
                mic.setImageResource(R.drawable.ic_call_mic_off)
                ChatContext.setIsMicrophoneMuted(true)
                stopService(serviceSendAudioIntent)
            }
        }

        stopCall.setOnClickListener {
            sendInformationAboutCallingToSender(CallingInfo("end", username , "", ""), username)
            goToChatActivity()
        }

        startServices()
    }


    private fun sendInformationAboutCallingToSender(body: Any, username: String) {
        stompClientCall.send("/app/call/audio/${username}", Gson().toJson(body))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Obsługa wysłania wiadomości */ },
                { /* Obsługa błędu */ }
            )
            .let { ChatContext.getCompositeDisposable().add(it) }
    }

    private fun subscribeCallingAudioTopic() {
        stompClientCall.topic("/topic/calling/audio/${currentLoggedUser}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val info = JSONObject(payload.payload.toString())
                    val callingInformation = info.getString("callingInformation")

                    if (callingInformation == "end") {
                        goToChatActivity()
                    }
                },
                { /* Obsługa błędu */ }
            )
            .let { ChatContext.getCompositeDisposable().add(it) }
    }

    private val updateTime: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            time = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            timerTextView.text = getTimeStringFromDouble(time)
        }
    }

    private fun getTimeStringFromDouble(time: Double): String {
        val resultInt = time.roundToInt()
        val hours = resultInt % 86400 / 3600
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun goToChatActivity() {
        if (LocalStorageAccess.doesFileExistInInternalStorage(this@CallActivity, "user.txt")) {
            val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(this@CallActivity, "user.txt")
            val userJSONObject = JSONObject(userString)
            val userJson = UserJson(
                userJSONObject.getString("userID"),
                userJSONObject.getString("username"),
                userJSONObject.getString("publicKey"),
                userJSONObject.getString("picture")
            )
            ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@CallActivity, userJson.username + "_private_key.txt"))

            getReceiverUserFromServer(username, userJson)
        }
    }

    private fun startServices() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@CallActivity, arrayOf(Manifest.permission.RECORD_AUDIO),
                SendAudioService.RECORD_AUDIO_PERMISSION_CODE
            )
        } else {
            serviceTimerIntent.putExtra(TimerService.TIME_EXTRA, time)
            serviceReceiveAudioIntent.putExtra("currentLoggedUser", currentLoggedUser)
            serviceSendAudioIntent.putExtra("username", username)
            startService(serviceTimerIntent)
            startService(serviceReceiveAudioIntent)
            startService(serviceSendAudioIntent)
        }
    }

    private fun stopServices() {
        stopService(serviceTimerIntent)
        stopService(serviceReceiveAudioIntent)
        stopService(serviceSendAudioIntent)
    }

    private fun getReceiverUserFromServer(username: String, currentLoggedUser: UserJson) {
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
                    val intentToChat = Intent(this@CallActivity, MessageChatActivity::class.java)
                    intentToChat.putExtra("currentLoggedUser", currentLoggedUser as java.io.Serializable)
                    intentToChat.putExtra("user", userReceiverFromServer as java.io.Serializable)
                    intentToChat.putExtra("sendMessageCallEnd", "Call ended after ${getTimeStringFromDouble(time)}")
                    intentToChat.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intentToChat)
                    finish()
                } else {
                    Toast.makeText(
                        this@CallActivity,
                        "An error occurred on getting user by username process. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UserJson?>, t: Throwable) {
                println(t.message)

                Toast.makeText(
                    this@CallActivity,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onDestroy() {
        stopServices()
        //ChatContext.getCompositeDisposable().clear()
        super.onDestroy()
    }
}