package valdez.francisco.dingdone

data class Home(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val invitationCode: String = "",
    val createdAt: com.google.firebase.Timestamp? = null
)
