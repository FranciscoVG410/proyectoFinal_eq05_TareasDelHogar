package valdez.francisco.dingdone

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Home(
    var id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val invitationCode: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val membersCanEdit: Boolean = true
) : Parcelable
