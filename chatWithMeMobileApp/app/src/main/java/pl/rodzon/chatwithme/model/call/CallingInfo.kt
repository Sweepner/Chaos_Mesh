package pl.rodzon.chatwithme.model.call

data class CallingInfo(
    val callingInformation: String,
    val username: String,
    val roomId: String,
    val callProvider: String)
