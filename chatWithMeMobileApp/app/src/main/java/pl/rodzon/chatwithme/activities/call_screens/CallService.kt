package pl.rodzon.chatwithme.activities.call_screens

import android.app.Service
import android.content.Intent
import android.media.*
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import pl.rodzon.chatwithme.model.call.AudioData
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.StompUtils
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.util.*

class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private val timer = Timer()
    private lateinit var stompClientCall: StompClient
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private lateinit var currentLoggedUser: String
    private lateinit var username: String
    private lateinit var recordAudioTask: RecordAudioTask

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val time = intent.getDoubleExtra(TIME_EXTRA, 0.0)
        currentLoggedUser = intent.getStringExtra("currentLoggedUser")!!
        username = intent.getStringExtra("username")!!
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)

        initializeAudioTrack()
        initializeStompClientCall()
        initializeAudioRecorder()

        this.recordAudioTask = RecordAudioTask(audioRecord!!, stompClientCall, username)

        startAudioStreaming()
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

    private fun initializeAudioRecorder() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
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

    private fun startAudioStreaming() {
        // Rozpocznij nagrywanie dźwięku w tle
        recordAudioTask.execute()
    }

    private fun decodeBase64ToByteArray(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun subscribeAudio() {
        stompClientCall.topic("/topic/receiveAudio/$currentLoggedUser")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
            .let { compositeDisposable.add(it) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun playMusicFromByteBuffer(byteBuffer: ByteBuffer) {
        val audioData = ByteArray(byteBuffer.remaining())
        byteBuffer.get(audioData)

        audioTrack?.play()
        audioTrack?.write(audioData, 0, audioData.size)
        audioTrack?.stop()
    }


    override fun onDestroy() {
        timer.cancel()
        audioThread.interrupt()
        recordAudioTask.cancel(true)
        super.onDestroy()
    }

    private inner class TimeTask(private var time: Double) : TimerTask() {
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            time++
            intent.putExtra(TIME_EXTRA, time)
            sendBroadcast(intent)
        }
    }

    private class RecordAudioTask(private var audioRecord: AudioRecord,
                                  private var stompClientCall: StompClient,
                                  private var username: String) : AsyncTask<Void, ByteArray, Void>() {
        val audioBufferSize = 6000


        override fun doInBackground(vararg params: Void): Void? {
            val buffer = ByteArray(audioBufferSize)

            audioRecord.startRecording()

            while (!isCancelled && !ChatContext.isIsMicrophoneMuted()) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    publishProgress(buffer.copyOf(bytesRead))
                }
            }

            audioRecord.stop()
            audioRecord.release()

            return null
        }

        override fun onProgressUpdate(vararg values: ByteArray) {
            // Wysyłaj dane audio do serwera
            audioThread = Thread {
                // Pobierz dane audio (to jest przykładowa implementacja, dostosuj do swoich potrzeb)
                // Wyślij dane audio do serwera
                val base64Data = encodeByteArrayToBase64(values[0])
                val audioData = AudioData(base64Data)
                val toJson = Gson().toJson(audioData)
                stompClientCall.send("/app/call/sendAudio/$username", toJson)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { /* Obsługa wysłania wiadomości */ },
                        { /* Obsługa błędu */ }
                    )
                    .let { compositeDisposable.add(it) }
            }
            audioThread.start()
        }
    }

    companion object {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timeExtra"
        const val RECORD_AUDIO_PERMISSION_CODE = 123
        private val compositeDisposable = CompositeDisposable()
        private lateinit var audioThread: Thread

        private fun encodeByteArrayToBase64(byteArray: ByteArray): String {
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }
}