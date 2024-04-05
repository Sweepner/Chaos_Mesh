package pl.rodzon.chatwithme.activities.chat_screen

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import pl.rodzon.chatwithme.model.message.MessageJson
import pl.rodzon.chatwithme.model.message.MessagesJson
import pl.rodzon.chatwithme.model.users.UsersWithMessagesJson
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDateTime


interface MessageChatApiInterface {
    @POST("chat/send")
    fun sendTextMessage(@Query("roomID") roomID: String?, @Query("text") message: String?, @Query("time") time: String?, @Query("publicKey") publicKey: String?): Call<MessageJson>

    @GET("chat/load")
    fun getMessage(@Query("roomID") roomID: String?, @Query("publicKey") publicKey: String?, @Query("isPageableEnabled") isPageableEnabled: Boolean?): Call<MessagesJson>

    @GET("chat/users/{userId}")
    fun getAllUsersFromRoomIdByUserId(@Path("userId") userId: String?): Call<UsersWithMessagesJson>

    @Multipart
    @POST("chat/image/send")
    fun sendImageMessage(
        @Part image: MultipartBody.Part,
        @Part("roomID") roomID: RequestBody?,
        @Part("publicKey") publicKey: RequestBody?,
        @Part("time") time: RequestBody?,
        @Part("username") username: RequestBody?,
        @Part("sender") sender: RequestBody?
    ): Call<MessageJson?>

    @GET("chat/image/latest")
    fun getLatestImageMessage(@Query("roomID") userID: String?, @Query("publicKey") publicKey: String?): Call<MessageJson>

    @DELETE("chat/message/self/{id}")
    fun deleteMessage(@Path("id") messageId: String?): Call<Void?>?

    @DELETE("chat/message/{time}")
    fun deleteMessageByCreationTime(@Path("time") time: String?, @Query("username") username: String?): Call<Void?>?

}