package pl.rodzon.chatwithme.model.message

import java.time.LocalDateTime
import java.util.*

data class MessageJson(
    var id: UUID? = null,
    var text: String? = null,
    var image: String? = null,
    var roomID: String? = null,
    var messageTime: String? = null,
    var messageTimeLocalDateTime: LocalDateTime?= null
) : java.io.Serializable