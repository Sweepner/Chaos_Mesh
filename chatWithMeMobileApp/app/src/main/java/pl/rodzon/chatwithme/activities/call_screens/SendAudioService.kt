package pl.rodzon.chatwithme.activities.call_screens

import android.app.Service
import android.content.Intent
import android.media.*
import android.os.AsyncTask
import android.os.IBinder
import android.util.Base64
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pl.rodzon.chatwithme.model.call.AudioData
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.StompUtils
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.net.URISyntaxException

class SendAudioService() : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private lateinit var stompClientCall: StompClient
    private var audioRecord: AudioRecord? = null
    private lateinit var username: String
    private lateinit var recordAudioTask: RecordAudioTask



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        username = intent.getStringExtra("username")!!

        initializeStompClientCall()
        initializeAudioRecorder()

        this.recordAudioTask = RecordAudioTask(audioRecord!!, stompClientCall, username)

        startAudioStreaming()
        return START_NOT_STICKY
    }

    private fun initializeAudioRecorder() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    private fun startAudioStreaming() {
        // Rozpocznij nagrywanie dźwięku w tle
        recordAudioTask.execute()
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
        stompClientCall.disconnect()
        audioThread.interrupt()
        recordAudioTask.cancel(true)
        super.onDestroy()
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
                    .let { ChatContext.getCompositeDisposable().add(it) }
            }
            audioThread.start()
        }
    }

    companion object {
        const val RECORD_AUDIO_PERMISSION_CODE = 123
        private lateinit var audioThread: Thread
        private fun encodeByteArrayToBase64(byteArray: ByteArray): String {
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }

}