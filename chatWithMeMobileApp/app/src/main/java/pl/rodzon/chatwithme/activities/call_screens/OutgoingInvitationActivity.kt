package pl.rodzon.chatwithme.activities.call_screens

import android.app.PendingIntent.getActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.json.JSONObject
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatActivity
import pl.rodzon.chatwithme.model.call.CallingInfo
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.MyBackgroundService
import ua.naiksoftware.stomp.StompClient
import java.net.URL
import java.time.LocalDateTime

class OutgoingInvitationActivity : AppCompatActivity() {
    private lateinit var callProvider: String
    private lateinit var user: UserJson
    private lateinit var currentLoggedUser: UserJson
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var handler: Handler
    private val stompClientCall: StompClient = ChatContext.getStompClientCall()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_invitation)

        val filter = IntentFilter(MyBackgroundService.OUTGOING_CALL_ACTIVITY)
        registerReceiver(dataReceiver, filter)

        val typeImageView: ImageView = findViewById(R.id.imageMeetingType)
        val imageViewStopInvitation: ImageView = findViewById(R.id.imageStopInvitation)
        val textFirstChar: TextView = findViewById(R.id.textFirstChar)
        val textUsername: TextView = findViewById(R.id.textUsername)

        val type = intent.getStringExtra("type")
        this.callProvider = intent.getStringExtra("callProvider")!!
        this.user = intent.extras?.get("user") as UserJson
        this.currentLoggedUser = intent.extras?.get("currentLoggedUser") as UserJson

        if (type != null) {
            if (type == "video") {
                typeImageView.setImageResource(R.drawable.ic_video)
            } else {
                typeImageView.setImageResource(R.drawable.ic_audio)
            }
        }

        textFirstChar.text = user.username[0].toString()
        textUsername.text = user.username

        sendInformationAboutCallingToReceiver(CallingInfo("start", currentLoggedUser.username, "", callProvider))

        handler = Handler()
        handler.postDelayed({
            sendInformationAboutCallingToReceiver(CallingInfo("cancel", currentLoggedUser.username, "", callProvider))
            val intentToChat = Intent(this@OutgoingInvitationActivity, MessageChatActivity::class.java)
            intentToChat.putExtra("currentLoggedUser", currentLoggedUser as java.io.Serializable)
            intentToChat.putExtra("user", user as java.io.Serializable)
            intentToChat.putExtra("sendMessageCallRequestNotAnswer", "\u2196    Outgoing call request to ${user.username} - receiver not answer.    \u2196")
            intentToChat.putExtra("sendMessageMissedCall", "\u2199    Missed call from ${currentLoggedUser.username} at: " + LocalDateTime.now().toString()
                .substring(0, 19).replace("T", " ") + "    \u2199")
            intentToChat.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intentToChat)
            finish()
        }, 60000)

        mediaPlayer = MediaPlayer.create(this, R.raw.call)
        mediaPlayer.start()



        imageViewStopInvitation.setOnClickListener {
            sendInformationAboutCallingToReceiver(CallingInfo("cancel", currentLoggedUser.username, "", callProvider))
            val intentToChat = Intent(this@OutgoingInvitationActivity, MessageChatActivity::class.java)
            intentToChat.putExtra("currentLoggedUser", currentLoggedUser as java.io.Serializable)
            intentToChat.putExtra("user", user as java.io.Serializable)
            intentToChat.putExtra("sendMessageCallRequestCancelled", "\u2196    Outgoing call request to ${user.username} - cancelled by You.    \u2196")
            intentToChat.putExtra("sendMessageMissedCall", "\u2199    Missed call from ${currentLoggedUser.username} at: " + LocalDateTime.now().toString()
                .substring(0, 19).replace("T", " ") + "    \u2199")
            intentToChat.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intentToChat)
            finish()
        }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendInformationAboutCallingToReceiver(CallingInfo("cancel", currentLoggedUser.username, "", callProvider))
                finish()
            }
        })

    }

    private fun sendInformationAboutCallingToReceiver(body: Any) {
        stompClientCall.send("/app/call/audio/${user.username}", Gson().toJson(body))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Obsługa wysłania wiadomości */ },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun callRejected() {
        val intentToChat = Intent(this@OutgoingInvitationActivity, MessageChatActivity::class.java)
        intentToChat.putExtra("currentLoggedUser", currentLoggedUser as java.io.Serializable)
        intentToChat.putExtra("user", user as java.io.Serializable)
        startActivity(intentToChat)
        finish()
    }

    private fun callAccepted(roomId: String) {
        if (callProvider == "Jitsi meet") {
            val serverURL = URL("https://jitsi.belnet.be")
            val options = JitsiMeetConferenceOptions.Builder()
                .setServerURL(serverURL)
                .setRoom(roomId)
                .setAudioMuted(true)
                .setVideoMuted(true)
                .build()

            JitsiMeetActivity.launch(this@OutgoingInvitationActivity, options)
            finish()
        } else if (callProvider == "pl.rodzon") {
            val intent = Intent(this, CallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("username", user.username)
            intent.putExtra("currentLoggedUser", currentLoggedUser.username)
            startActivity(intent)
            finish()
        }
    }

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MyBackgroundService.OUTGOING_CALL_ACTIVITY) {
                val receivedData = intent.getStringExtra("message")
                if (receivedData == "reject") {
                    callRejected()
                } else if (receivedData == "accept") {
                    callAccepted(intent.getStringExtra("extraData")!!)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacksAndMessages(null)
    }
}