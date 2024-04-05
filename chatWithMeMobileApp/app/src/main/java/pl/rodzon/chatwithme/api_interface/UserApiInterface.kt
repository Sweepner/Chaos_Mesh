package pl.rodzon.chatwithme.api_interface

import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.model.users.UsersJson
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiInterface {
    @GET("users")
    fun getUsers(@Query("isPageableEnabled") isPageableEnabled: Boolean?): Call<UsersJson>

    @GET("users")
    fun getUsers(@Query("username") username: String?, @Query("isPageableEnabled") isPageableEnabled: Boolean?): Call<UsersJson>

    @GET("users/{username}")
    fun getUserByUsername(@Path("username") username: String?): Call<UserJson>
}