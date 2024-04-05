package pl.rodzon.chatwithme.utils

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import pl.rodzon.chatwithme.activities.call_screens.IncomingInvitationActivity
import pl.rodzon.chatwithme.model.call.CallingInfo
import pl.rodzon.chatwithme.model.message.MessageJsonNotification
import pl.rodzon.chatwithme.model.users.UserJson
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.net.URISyntaxException
import java.util.*


class MyBackgroundService : Service() {
    companion object {
        lateinit var notificationHelper: NotificationHelper
        const val INCOMING_CALL_ACTIVITY = "pl.rodzon.chatwithme.INCOMING_CALL_ACTIVITY"
        const val OUTGOING_CALL_ACTIVITY = "pl.rodzon.chatwithme.OUTGOING_CALL_ACTIVITY"
    }

    private lateinit var stompClientChat: StompClient
    private lateinit var stompClientCall: StompClient
    private val compositeDisposable = CompositeDisposable()
    private lateinit var currentLoggedUser: UserJson

    override fun onCreate() {
        super.onCreate()

        showLocationNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                applicationContext
            )
        ) {
            // Jeśli nie masz uprawnień do okna nakładanego, poproś użytkownika o zgodę
            val intentUprawnien = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intentUprawnien.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intentUprawnien)
        }
        notificationHelper = NotificationHelper(this@MyBackgroundService)
        if (LocalStorageAccess.doesFileExistInInternalStorage(this, "user.txt")) {
            val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(
                this@MyBackgroundService,
                "user.txt"
            )
            val userJSONObject = JSONObject(userString)
            currentLoggedUser = UserJson(
                userJSONObject.getString("userID"),
                userJSONObject.getString("username"),
                userJSONObject.getString("publicKey"),
                userJSONObject.getString("picture")
            )
        }

        initializeStompClientChat()
        initializeStompClientCall()

        createNotificationChannel()

        subscribeMessageNotificationTopic()
        subscribeMessageImageNotificationTopic()

        subscribeCallingAudioTopic()
    }

    private fun subscribeMessageNotificationTopic() {
        stompClientChat.topic("/topic/messages/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val jsonMessage = JSONObject(payload.payload.toString())
                    val message = MessageJsonNotification(
                        UUID.fromString(jsonMessage.getString("id")),
                        SSHUtils.decryptMessage(
                            jsonMessage.getString("text"),
                            ChatContext.getPrivateKey()
                        ),
                        null,
                        jsonMessage.getString("roomID"),
                        jsonMessage.getString("messageTime"),
                        jsonMessage.getString("username"),
                    )

                    if (ChatContext.getReceiverUsername() != message.username) {
                        // Tworzenie notyfikacji
                        notificationHelper.createNotification(
                            "my_channel_id",
                            message.username!!,
                            message.text!!,
                            message.username!!
                        )
                    }
                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeMessageImageNotificationTopic() {
        stompClientChat.topic("/topic/messages/image/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    // Tworzenie notyfikacji
                    if (ChatContext.getReceiverUsername() != payload.payload.toString()) {
                        notificationHelper.createNotification(
                            "my_channel_id",
                            payload.payload.toString(),
                            "Sent you an image",
                            payload.payload.toString()
                        )
                    }
                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeCallingAudioTopic() {
        stompClientCall.topic("/topic/calling/audio/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val info = JSONObject(payload.payload.toString())
                    val callingInfo = CallingInfo(
                        info.getString("callingInformation"),
                        info.getString("username"),
                        info.getString("roomId"),
                        info.getString("callProvider")
                    )
                    if (callingInfo.callingInformation == "start") {
                        startIncomingCallActivity(callingInfo.username, callingInfo.callProvider)
                    } else if (callingInfo.callingInformation == "cancel") {
                        cancelIncomingCallActivity()
                    } else if (callingInfo.callingInformation == "reject") {
                        rejectIncomingCallActivity()
                    } else  if (callingInfo.callingInformation == "accept") {
                        acceptIncomingCallActivity(callingInfo.roomId)
                    }
                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun startIncomingCallActivity(username: String, callProvider: String) {
        val intent = Intent(applicationContext, IncomingInvitationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("username", username)
        intent.putExtra("currentLoggedUser", currentLoggedUser.username)
        intent.putExtra("type", "audio")
        intent.putExtra("callProvider", callProvider)
        applicationContext.startActivity(intent)
    }

    private fun cancelIncomingCallActivity() {
        sendDataToActivity(INCOMING_CALL_ACTIVITY, "cancel", null)
    }

    private fun acceptIncomingCallActivity(roomId: String) {
        sendDataToActivity(OUTGOING_CALL_ACTIVITY, "accept", roomId)
    }

    private fun rejectIncomingCallActivity() {
        sendDataToActivity(OUTGOING_CALL_ACTIVITY, "reject", null)
    }

    private fun sendDataToActivity(destinationActivity: String, data: String, extraData: String?) {
        val intent = Intent(destinationActivity)
        intent.putExtra("message", data)
        if (extraData != null) {
            intent.putExtra("extraData", extraData)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        stompClientCall.disconnect()
        stompClientChat.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initializeStompClientChat() {
        try {
            stompClientChat = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                ChatContext.getWebsocketServerMessageURL()
            )
            StompUtils.lifecycle(stompClientChat)
            stompClientChat.connect()
            ChatContext.setStompClientChat(stompClientChat)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }


    private fun initializeStompClientCall() {
        try {
            stompClientCall = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                ChatContext.getWebsocketServerCallURL()
            )
            StompUtils.lifecycle(stompClientCall)
            stompClientCall.connect()
            ChatContext.setStompClientCall(stompClientCall)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        val channelId = "my_channel_id"
        val channelName = "My Channel"
        val channelDescription = "Channel for receiving new messages"

        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel(channelId, channelName, channelDescription)
    }

    private fun showLocationNotification() {
        val channel = NotificationChannel(
            "CHANNEL_ID",
            "ChatWithMeChannel",
            NotificationManager.IMPORTANCE_MAX
        )
        channel.description = "Channel for foreground service notification"

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this)
            .setContentTitle("ChatWithMe")
            .setContentText("ChatWithMe is running in the background")
            .setSmallIcon(R.mipmap.sym_def_app_icon)
            .setChannelId(channel.id)
            .build()
        startForeground(1, notification)
    }
}


