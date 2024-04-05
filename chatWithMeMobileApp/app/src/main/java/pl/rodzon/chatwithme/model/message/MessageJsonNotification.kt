package pl.rodzon.chatwithme.model.message


import java.util.*

data class MessageJsonNotification(
    var id: UUID? = null,
    var text: String? = null,
    var image: String? = null,
    var roomID: String? = null,
    var messageTime: String? = null,
    var username: String? = null
) : java.io.Serializable
