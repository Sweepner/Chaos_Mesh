package pl.rodzon.chatwithme.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import pl.rodzon.chatwithme.R

class NotificationHelper(private val context: Context) {
    private val messagesMap = mutableMapOf<String, MutableList<String>>()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel(channelId: String, channelName: String, channelDescription: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        channelId: String,
        title: String,
        content: String,
        username: String?
    ) {
        val soundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
        var intent = Intent(context, FetchDataFromServerService::class.java)
        if (username == null) {
            intent.putExtra("main", "main")
        } else {
            intent.putExtra("username", username)
        }

        val messages = messagesMap.getOrPut(username!!) { mutableListOf() }
        messages.add(content)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(username)
            .setSummaryText(username)

        for (msg in messages) {
            inboxStyle.addLine(msg)
        }

        val fetchDataPendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(fetchDataPendingIntent)

        notificationManager.notify(username.hashCode(), builder.build())
    }

    fun removeNotification(notificationId: Int, username: String) {
        notificationManager.cancel(notificationId)
        removeAllNotificationsOfUser(username)
    }

    fun removeAllNotificationsOfUser(username: String) {
        messagesMap.remove(username)
    }
}
