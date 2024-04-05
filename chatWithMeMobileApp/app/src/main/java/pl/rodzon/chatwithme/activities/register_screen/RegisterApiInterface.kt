package pl.rodzon.chatwithme.activities.register_screen

import pl.rodzon.chatwithme.model.users.UserJson
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface RegisterApiInterface {
    @POST("auth/registration")
    fun register(@Query("username") username: String?, @Query("password") password: String?, @Query("publicKey") publicKey: String?): Call<UserJson>
}