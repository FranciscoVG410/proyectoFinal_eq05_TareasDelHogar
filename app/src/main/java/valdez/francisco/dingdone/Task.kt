package valdez.francisco.dingdone

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    var nombre: String = "",
    var descripcio: String = "",
    var member: List<String> = emptyList(),
    var date: List<String> = emptyList(),
    var stability: Int = 0,
    var state: String = ""
): Parcelable
