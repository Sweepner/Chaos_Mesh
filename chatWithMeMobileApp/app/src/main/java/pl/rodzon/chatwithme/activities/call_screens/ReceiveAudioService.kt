package pl.rodzon.chatwithme.activities.call_screens

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.annotation.RequiresApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import pl.rodzon.chatwithme.model.call.AudioData
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.StompUtils
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.net.URISyntaxException
import java.nio.ByteBuffer

class ReceiveAudioService() : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private lateinit var stompClientCall: StompClient
    private var audioTrack: AudioTrack? = null
    private lateinit var currentLoggedUser: String

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        currentLoggedUser = intent.getStringExtra("currentLoggedUser")!!
        initializeStompClientCall()
        initializeAudioTrack()
        subscribeAudio()
        return START_NOT_STICKY
    }

    private fun initializeAudioTrack() {
        val sampleRate =
            44100 // Dla przykładu, można dostosować do rzeczywistych danych dźwiękowych
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun subscribeAudio() {
        stompClientCall.topic("/topic/receiveAudio/$currentLoggedUser")
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val jsonAudio = JSONObject(payload.payload.toString())
                    val audioData = AudioData(jsonAudio.getString("base64Data"))
                    val decodedData = decodeBase64ToByteArray(audioData.base64Data)

                    playMusicFromByteBuffer(ByteBuffer.wrap(decodedData))

                },
                { /* Obsługa błędu */ }
            )
            .let { ChatContext.getCompositeDisposable().add(it) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun playMusicFromByteBuffer(byteBuffer: ByteBuffer) {
        val audioData = ByteArray(byteBuffer.remaining())
        byteBuffer.get(audioData)

        audioTrack?.play()
        audioTrack?.write(audioData, 0, audioData.size)
        audioTrack?.stop()
    }

    private fun initializeStompClientCall() {
        try {
            stompClientCall = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                ChatContext.getWebsocketServerCallURL()
            )
            StompUtils.lifecycle(stompClientCall)
            stompClientCall.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClientCall.disconnect()
    }

    companion object {
        private fun decodeBase64ToByteArray(base64String: String): ByteArray {
            return Base64.decode(base64String, Base64.DEFAULT)
        }
    }

}