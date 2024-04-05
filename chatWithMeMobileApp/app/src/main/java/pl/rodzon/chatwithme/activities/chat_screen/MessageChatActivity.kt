package pl.rodzon.chatwithme.activities.chat_screen

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.call_screens.OutgoingInvitationActivity
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.activities.welcome_screen.WelcomeActivity
import pl.rodzon.chatwithme.adapter_classes.ChatsAdapter
import pl.rodzon.chatwithme.model.message.*
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.naiksoftware.stomp.StompClient
import java.io.File
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MessageChatActivity : AppCompatActivity() {
    private lateinit var user: UserJson
    private lateinit var currentLoggedUser: UserJson
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var messagesJson: MessagesJson
    private lateinit var recyclerViewChats: RecyclerView
    private lateinit var usernameText: TextView
    private lateinit var keySaver: KeySaver

    private val stompClientChat: StompClient = ChatContext.getStompClientChat()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_chat)

        val toolbar: Toolbar = findViewById(R.id.toolbar_message_chat)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this@MessageChatActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("user", currentLoggedUser as java.io.Serializable)
            startActivity(intent)
            finish()
        }

        val imageViewAudioCall: ImageView = findViewById(R.id.image_audio_call)

        this.user = intent.extras?.get("user") as UserJson
        this.currentLoggedUser = intent.extras?.get("currentLoggedUser") as UserJson
        this.recyclerViewChats = findViewById(R.id.recycler_view_chats)
        recyclerViewChats.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        recyclerViewChats.layoutManager = linearLayoutManager

        removeNotification(user.username.hashCode(), user.username)
        ChatContext.setReceiverUsername(user.username)

        val messageEditText: EditText = findViewById(R.id.text_message)
        this.usernameText = findViewById(R.id.username_mchat)
        // Inicjalizacja klienta Stomp

        subscribeMessageTopic()
        subscribeWritingTopic()
        subscribeForImageTopic()
        subscribeForDeleteMessageTopic()

        this.messagesJson = MessagesJson(
            ArrayList<MessageJson>()
        )

        this.chatsAdapter = ChatsAdapter(
            this@MessageChatActivity,
            messagesJson.messages,
            user.picture,
            currentLoggedUser
        )
        this.recyclerViewChats.adapter = chatsAdapter

        this.usernameText.text = user.username

        val profilePicture: CircleImageView = findViewById(R.id.profile_image_mchat)
        Picasso.get().load(user.picture).into(profilePicture)

        val sendImageButton: ImageView = findViewById(R.id.attact_image_file_btn)
        val sendButton: ImageView = findViewById(R.id.send_message_btn)

        val viewGroup =
            (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup

        initializeActivity(viewGroup)

        checkIfAnyExtraBehaviorRequest()

        sendButton.setOnClickListener() {
            val messageString: String = messageEditText.text.toString()

            if (messageString == "") {
                Toast.makeText(
                    this@MessageChatActivity,
                    "Please write something first...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val roomId = currentLoggedUser.userID + user.userID
                val time = LocalDateTime.now()
                val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val timeString: String = time.format(dateTimeFormatter)
                sendMessageToUser(
                    currentLoggedUser.userID,
                    user.userID,
                    SSHUtils.encryptMessage(messageString, currentLoggedUser.publicKey),
                    timeString,
                    currentLoggedUser.publicKey
                ) // wysłanie wiadomości szyfrując kluczem publicznym usera zalogowanego
                //sendMessageToUser(currentLoggedUser.userID, user.userID, SSHUtils.encryptMessage(messageString, user.publicKey), user.publicKey) // wysłanie wiadomości szyfrując kluczem publicznym usera do którego chcemy wysłać
                if (currentLoggedUser.username != user.username) {
                    val message = TextMessageRequest(
                        roomId,
                        SSHUtils.encryptMessage(messageString, user.publicKey),
                        timeString,
                        user.publicKey,
                        currentLoggedUser.username,
                    )
                    sendMessageToUserWebSocket(message)
                    sendWritingInfo("")
                }
                messageEditText.setText("")
            }
        }

        sendImageButton.setOnClickListener() {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Pick Image"), 438)
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                timerToChangeText()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendWritingInfo(" is writing...")
            }
        })

        imageViewAudioCall.setOnClickListener {
            var callProvider = ""
            val options = arrayOf<CharSequence>("pl.rodzon", "Jitsi meet", "Cancel")
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MessageChatActivity)
            builder.setTitle("Choose provider:")
            builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                if (which == 0) {
                    callProvider = "pl.rodzon"
                    intentToOutgoingCallScreen(callProvider)
                } else if (which == 1) {
                    callProvider = "Jitsi meet"
                    intentToOutgoingCallScreen(callProvider)
                } else {
                    dialog.dismiss()
                }
            })
            builder.show()
        }
    }

    private fun intentToOutgoingCallScreen(callProvider: String) {
        val intent = Intent(this@MessageChatActivity, OutgoingInvitationActivity::class.java)
        intent.putExtra("user", user as Serializable)
        intent.putExtra("currentLoggedUser", currentLoggedUser as Serializable)
        intent.putExtra("type", "audio")
        intent.putExtra("callProvider", callProvider)
        startActivity(intent)
        finish()
    }

    private fun initializeActivity(view: View) {
        (messagesJson.messages as ArrayList).clear()
        loadMessage(
            currentLoggedUser.userID + user.userID,
            currentLoggedUser.publicKey,
            view
        ) //wczytanie wiadomości zalogowanego usera

        if (currentLoggedUser.username != user.username) {
            loadMessage(
                user.userID + currentLoggedUser.userID,
                currentLoggedUser.publicKey,
                view
            ) //wczytanie wiadomości usera z kim się chatuje
        }

        Handler().postDelayed({
            checkIfPrivateKeyIsGood()
        }, 500)
    }

    private fun getMessagesFromServer(view: View) {
        (messagesJson.messages as ArrayList).clear()
        loadMessage(
            currentLoggedUser.userID + user.userID,
            currentLoggedUser.publicKey,
            view
        ) //wczytanie wiadomości zalogowanego usera
        loadMessage(
            user.userID + currentLoggedUser.userID,
            user.publicKey,
            view
        ) //wczytanie wiadomości usera z kim się chatuje
        setMessageTimeLocalDateTime()
        (messagesJson.messages as ArrayList<MessageJson>).sortWith(
            Comparator.comparing(
                MessageJson::messageTimeLocalDateTime
            )
        )
        chatsAdapter.notifyDataSetChanged()
    }

    private fun sendMessageToUser(
        senderId: String,
        receiverId: String,
        messageText: String?,
        time: String?,
        publicKey: String
    ) {
        val roomId = senderId + receiverId
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerMessageURL())
            .build()

        val service = retrofit.create(MessageChatApiInterface::class.java)

        val call = service.sendTextMessage(
            roomId,
            messageText,
            time,
            publicKey
        )

        call.enqueue(object : Callback<MessageJson?> {
            override fun onResponse(call: Call<MessageJson?>, response: Response<MessageJson?>) {
                if (response.code() == 201) {
                    val responseBody = response.body()
                    val messageJsonResponse = responseBody!!
                    (messagesJson.messages as ArrayList).add(
                        MessageJson(
                            messageJsonResponse.id,
                            SSHUtils.decryptMessage(
                                messageJsonResponse.text,
                                ChatContext.getPrivateKey()
                            ),
                            null,
                            messageJsonResponse.roomID,
                            messageJsonResponse.messageTime
                        )
                    )
                    setMessageTimeLocalDateTime()
                    (messagesJson.messages as ArrayList<MessageJson>).sortWith(
                        Comparator.comparing(
                            MessageJson::messageTimeLocalDateTime
                        )
                    )
                    chatsAdapter.notifyDataSetChanged()
                    scrollDownRecyclerView()
                } else {
                    Toast.makeText(
                        this@MessageChatActivity,
                        "An error occurred on fetch image. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<MessageJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    this@MessageChatActivity,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun loadMessage(roomId: String, publicKey: String, view: View) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerMessageURL())
            .build()
            .create(MessageChatApiInterface::class.java)

        val retrofitData = retrofitBuilder.getMessage(roomId, publicKey, false)

        retrofitData.enqueue(object : Callback<MessagesJson?> {
            override fun onResponse(call: Call<MessagesJson?>, response: Response<MessagesJson?>) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    val messagesJsonResponse = responseBody!!.messages
                    for (messageJson in messagesJsonResponse) {
                        if (messageJson.image != null) {
                            (messagesJson.messages as ArrayList).add(
                                MessageJson(
                                    messageJson.id,
                                    null,
                                    messageJson.image,
                                    messageJson.roomID,
                                    messageJson.messageTime
                                )
                            )
                        } else {
                            (messagesJson.messages as ArrayList).add(
                                MessageJson(
                                    messageJson.id,
                                    SSHUtils.decryptMessage(
                                        messageJson.text,
                                        ChatContext.getPrivateKey()
                                    ),
                                    messageJson.image,
                                    messageJson.roomID,
                                    messageJson.messageTime
                                )
                            )
                        }
                    }
                    setMessageTimeLocalDateTime()
                    (messagesJson.messages as ArrayList<MessageJson>).sortWith(
                        Comparator.comparing(
                            MessageJson::messageTimeLocalDateTime
                        )
                    )
                    chatsAdapter.notifyDataSetChanged()
                    scrollDownRecyclerView()
                } else {
                    Toast.makeText(
                        view.context,
                        "An error occurred on fetch users. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<MessagesJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    view.context,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.data != null) {
            val loadingBar = ProgressDialog(this)
            loadingBar.setMessage("Please wait, image is being send...")
            loadingBar.show()
            // tu wysłanie wiadomości do bazy danych oraz do usera ze zdjęciem

            val fileUri: Uri? = data.data
            val imageFile = ImageUtil.uriToFile(this, fileUri!!)

            val time = LocalDateTime.now()
            if (currentLoggedUser.username != user.username) {
                sendImageMessage(
                    imageFile!!,
                    currentLoggedUser.userID + user.userID,
                    currentLoggedUser.publicKey,
                    null,
                    time
                )
            }
            sendImageMessage(
                imageFile!!,
                currentLoggedUser.userID + user.userID,
                user.publicKey,
                user.username,
                time
            )

            loadingBar.dismiss()
        } else if (requestCode == KeySaver.REQUEST_CODE_LOAD && resultCode == RESULT_OK) {
            val uri: Uri = data!!.data!!
            val loadKeyFromFile = keySaver.loadKeyFromFile(uri)

            LocalStorageAccess.deleteFileFromInternalStorage(this@MessageChatActivity, currentLoggedUser.username + "_private_key.txt")
            ChatContext.setPrivateKey(loadKeyFromFile)
            LocalStorageAccess.savePrivateKeyToInternalStorage(this@MessageChatActivity, currentLoggedUser.username + "_private_key.txt", ChatContext.getPrivateKey())
            ChatContext.setIsWrongPrivateKey(false)
            Toast.makeText(
                this@MessageChatActivity,
                "Private key has been imported.",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this@MessageChatActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("user", currentLoggedUser as java.io.Serializable)
            startActivity(intent)
            finish()
        }
    }

    private fun sendImageMessage(
        imageFile: File,
        roomID: String,
        publicKey: String,
        username: String?,
        time: LocalDateTime?
    ) {

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerMessageURL())
            .build()

        val service = retrofit.create(MessageChatApiInterface::class.java)

        val requestFile = imageFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        // Konwersja pozostałych parametrów na RequestBody
        val roomIDPart = roomID.toRequestBody("text/plain".toMediaTypeOrNull())
        val publicKeyPart = publicKey.toRequestBody("text/plain".toMediaTypeOrNull())
        var userName: RequestBody? = null
        if (username != null) {
            userName = username.toRequestBody("text/plain".toMediaTypeOrNull())
        }
        val sender = currentLoggedUser.username.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val timeRequestBody =
            time?.format(dateTimeFormatter)?.toRequestBody("text/plain".toMediaTypeOrNull())

        // Wysłanie żądania
        val call = service.sendImageMessage(
            imagePart,
            roomIDPart,
            publicKeyPart,
            timeRequestBody,
            userName,
            sender
        )
        call.enqueue(object : Callback<MessageJson?> {
            override fun onResponse(call: Call<MessageJson?>, response: Response<MessageJson?>) {
                if (response.code() == 201) {
                    val responseBody = response.body()
                    val messageJsonResponse = responseBody!!
                    (messagesJson.messages as ArrayList).add(
                        MessageJson(
                            messageJsonResponse.id,
                            messageJsonResponse.text,
                            messageJsonResponse.image,
                            messageJsonResponse.roomID,
                            messageJsonResponse.messageTime
                        )
                    )
                    setMessageTimeLocalDateTime()
                    (messagesJson.messages as ArrayList<MessageJson>).sortWith(
                        Comparator.comparing(
                            MessageJson::messageTimeLocalDateTime
                        )
                    )
                    chatsAdapter.notifyDataSetChanged()
                    scrollDownRecyclerView()
                } else if (response.code() == 200) {
                    //do nothing
                } else {
                    Toast.makeText(
                        this@MessageChatActivity,
                        "An error occurred on sending image. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<MessageJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    this@MessageChatActivity,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun decryptImage(byteArray: ByteArray?): ByteArray? {
        if (byteArray == null) {
            return null
        }
        /*val encryptedImageAsString = byteArray.toString()*/
        val decryptedImage = SSHUtils.decryptEncryptedImage(byteArray, ChatContext.getPrivateKey())
        return decryptedImage
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setMessageTimeLocalDateTime() {
        for (message in this.messagesJson.messages) {
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
            val localDateTime: LocalDateTime =
                LocalDateTime.parse(message.messageTime, dateTimeFormatter)
            message.messageTimeLocalDateTime = localDateTime
        }
    }

    private fun scrollDownRecyclerView() {
        if (chatsAdapter.itemCount != 0) {
            recyclerViewChats.smoothScrollToPosition(chatsAdapter.itemCount - 1)
        }
    }

    private fun sendMessageToUserWebSocket(body: Any) {
        stompClientChat.send("/app/chat/${user.username}", Gson().toJson(body))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Obsługa wysłania wiadomości */ },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeMessageTopic() {
        stompClientChat.topic("/topic/messages/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val jsonMessage = JSONObject(payload.payload.toString())
                    val message = MessageJson(
                        UUID.fromString(jsonMessage.getString("id")),
                        SSHUtils.decryptMessage(
                            jsonMessage.getString("text"),
                            ChatContext.getPrivateKey()
                        ),
                        null,
                        jsonMessage.getString("roomID"),
                        jsonMessage.getString("messageTime")
                    )
                    (messagesJson.messages as ArrayList).add(
                        message
                    )
                    setMessageTimeLocalDateTime()
                    (messagesJson.messages as ArrayList<MessageJson>).sortWith(
                        Comparator.comparing(
                            MessageJson::messageTimeLocalDateTime
                        )
                    )
                    chatsAdapter.notifyDataSetChanged()
                    scrollDownRecyclerView()

                    // Tworzenie notyfikacji
                    /*val notificationHelper = NotificationHelper(this@MessageChatActivity)
                    notificationHelper.createNotification("my_channel_id", user.username, message.text!!)*/

                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeWritingTopic() {
        stompClientChat.topic("/topic/writing/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val info = JSONObject(payload.payload.toString())
                    runOnUiThread {
                        usernameText.text = user.username + info.get("writingInformation")
                    }
                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeForImageTopic() {
        stompClientChat.topic("/topic/messages/image/${currentLoggedUser.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    downloadImageFromServer()
                },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun subscribeForDeleteMessageTopic() {
        stompClientChat.topic("/topic/messages/delete/${user.username}")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { payload ->
                    // Obsługa otrzymanej wiadomości
                    val timeString = payload.payload.toString()
                    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val timeLocalDateTime = LocalDateTime.parse(timeString, dateTimeFormatter)
                    (messagesJson.messages as ArrayList).removeIf { messagesJson ->
                        messagesJson.messageTimeLocalDateTime == timeLocalDateTime
                    }
                    chatsAdapter.notifyDataSetChanged()
                },
                {
                    val string = "error"
                    println(string)
                }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun downloadImageFromServer() {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerMessageURL())
            .build()
            .create(MessageChatApiInterface::class.java)

        val retrofitData = retrofitBuilder.getLatestImageMessage(
            user.userID + currentLoggedUser.userID,
            currentLoggedUser.publicKey
        )

        retrofitData.enqueue(object : Callback<MessageJson?> {
            override fun onResponse(call: Call<MessageJson?>, response: Response<MessageJson?>) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    val messageJsonResponse = responseBody!!
                    (messagesJson.messages as ArrayList).add(
                        MessageJson(
                            messageJsonResponse.id,
                            messageJsonResponse.text,
                            messageJsonResponse.image,
                            messageJsonResponse.roomID,
                            messageJsonResponse.messageTime
                        )
                    )
                    setMessageTimeLocalDateTime()
                    (messagesJson.messages as ArrayList<MessageJson>).sortWith(
                        Comparator.comparing(
                            MessageJson::messageTimeLocalDateTime
                        )
                    )
                    chatsAdapter.notifyDataSetChanged()
                    scrollDownRecyclerView()
                } else {
                    Toast.makeText(
                        this@MessageChatActivity,
                        "An error occurred on fetch image. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<MessageJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    this@MessageChatActivity,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }


    private fun timerToChangeText() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                sendWritingInfo("")
                timer.cancel()
            }
        }, 3000)

    }

    private fun sendWritingInfo(info: String) {
        val writingInfo = IsWriting(
            info
        )
        stompClientChat.send("/app/chat/writing/${user.username}", Gson().toJson(writingInfo))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Obsługa wysłania wiadomości */ },
                { /* Obsługa błędu */ }
            )
            .let { compositeDisposable.add(it) }
    }

    private fun removeNotification(notificationId: Int, username: String) {
        MyBackgroundService.notificationHelper.removeNotification(notificationId, username)
    }

    override fun onBackPressed() {
        val intent = Intent(this@MessageChatActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("user", currentLoggedUser as java.io.Serializable)
        startActivity(intent)
        finish()
    }

    private fun checkIfAnyExtraBehaviorRequest() {
        intentSendMessageRejectCall()
        intentSendMessageCallRequestNotAnswer()
        intentSendMessageCallRequestCancelled()
        intentSendMessageCallEnd()
        intentSendMessageMissedCall()
    }

    private fun intentSendMessageRejectCall() {
        if (intent.getStringExtra("sendMessageCantTalk") != null) {
            val messageString: String = intent.getStringExtra("sendMessageCantTalk").toString()
            val time = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val timeString: String = time.format(dateTimeFormatter)
            sendMessageToUser(
                currentLoggedUser.userID,
                user.userID,
                SSHUtils.encryptMessage(messageString, currentLoggedUser.publicKey),
                timeString,
                currentLoggedUser.publicKey
            )
            val message = TextMessageRequest(
                currentLoggedUser.userID + user.userID,
                SSHUtils.encryptMessage(messageString, user.publicKey),
                timeString,
                user.publicKey,
                currentLoggedUser.username,
            )
            sendMessageToUserWebSocket(message)
        }
    }

    private fun intentSendMessageCallRequestNotAnswer() {
        if (intent.getStringExtra("sendMessageCallRequestNotAnswer") != null) {
            val messageString: String =
                intent.getStringExtra("sendMessageCallRequestNotAnswer").toString()
            val time = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val timeString: String = time.format(dateTimeFormatter)
            sendMessageToUser(
                currentLoggedUser.userID,
                user.userID,
                SSHUtils.encryptMessage(messageString, currentLoggedUser.publicKey),
                timeString,
                currentLoggedUser.publicKey
            )
        }
    }

    private fun intentSendMessageCallRequestCancelled() {
        if (intent.getStringExtra("sendMessageCallRequestCancelled") != null) {
            val messageString: String =
                intent.getStringExtra("sendMessageCallRequestCancelled").toString()
            val time = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val timeString: String = time.format(dateTimeFormatter)
            sendMessageToUser(
                currentLoggedUser.userID,
                user.userID,
                SSHUtils.encryptMessage(messageString, currentLoggedUser.publicKey),
                timeString,
                currentLoggedUser.publicKey
            )
        }
    }

    private fun intentSendMessageCallEnd() {
        if (intent.getStringExtra("sendMessageCallEnd") != null) {
            val messageString: String = intent.getStringExtra("sendMessageCallEnd").toString()
            val time = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val timeString: String = time.format(dateTimeFormatter)
            sendMessageToUser(
                currentLoggedUser.userID,
                user.userID,
                SSHUtils.encryptMessage(messageString, currentLoggedUser.publicKey),
                timeString,
                currentLoggedUser.publicKey
            )
        }
    }

    private fun intentSendMessageMissedCall() {
        if (intent.getStringExtra("sendMessageMissedCall") != null) {
            val messageString: String = intent.getStringExtra("sendMessageMissedCall").toString()
            val time = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val timeString: String = time.format(dateTimeFormatter)

            val message = TextMessageRequest(
                currentLoggedUser.userID + user.userID,
                SSHUtils.encryptMessage(messageString, user.publicKey),
                timeString,
                user.publicKey,
                currentLoggedUser.username,
            )
            sendMessageToUserWebSocket(message)
        }
    }

    private fun checkIfPrivateKeyIsGood() {
        if (ChatContext.getIsWrongPrivateKey()) {
            val popupView: View = layoutInflater.inflate(R.layout.popup_import_private_key, null)
            val popupWindow = PopupWindow(
                popupView,
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )
            popupWindow.isFocusable = true

            val textViewWarning: TextView = popupView.findViewById(R.id.textViewWelcome)
            val textViewInfo: TextView = popupView.findViewById(R.id.textViewInfo)
            val btnCancel: Button = popupView.findViewById(R.id.btnLogout)
            val btnImport: Button = popupView.findViewById(R.id.btnImportKey)
            btnCancel.text = "Cancel"
            textViewWarning.text = "WARNING!"
            textViewInfo.text = "Your private key is wrong. Please import it again."

            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

            btnCancel.setOnClickListener {
                popupWindow.dismiss()
            }

            btnImport.setOnClickListener {
                keySaver = KeySaver(this@MessageChatActivity, currentLoggedUser.username)
                keySaver.loadKey("For now its not used")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sendWritingInfo("")
        compositeDisposable.dispose()
        ChatContext.setReceiverUsername(null)
    }
}
