package pl.rodzon.chatwithme.model.message

data class TextMessageRequest(
    val roomID: String,
    val text: String,
    val time: String,
    val publicKey: String,
    val username: String)
