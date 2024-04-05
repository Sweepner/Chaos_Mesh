package pl.rodzon.chatwithme.activities.login_screen

import pl.rodzon.chatwithme.model.users.UserJson
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LoginApiInterface {

    @GET("auth/login")
    fun login(@Query("username") username: String?, @Query("password") password: String?): Call<UserJson>
}