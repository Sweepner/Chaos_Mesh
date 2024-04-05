package pl.rodzon.chatwithme.model.users

data class UserJson(
    val userID: String,
    val username: String,
    val publicKey: String,
    val picture: String
) : java.io.Serializable