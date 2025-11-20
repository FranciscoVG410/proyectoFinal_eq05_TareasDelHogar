package valdez.francisco.dingdone

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    @DocumentId
    var id: String = "",
    var nombre: String = "",
    var descripcio: String = "",
    var member: List<String> = emptyList(),
    var date: List<String> = emptyList(),
    var stability: Int = 0,
    var completionDate: Long? = null,
    var state: String = "",
    var editableBy: List<String> = emptyList()
): Parcelable
