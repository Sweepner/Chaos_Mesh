package pl.rodzon.chatwithme.model.message

data class ImageMessageRequest(
    val roomID: String,
    val image: String,
    val publicKey: String)
