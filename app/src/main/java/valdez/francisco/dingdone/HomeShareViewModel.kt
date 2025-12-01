package valdez.francisco.dingdone

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeShareViewModel : ViewModel() {

    private val _selectedHomeId = MutableLiveData<String?>()
    val selectedHomeId: LiveData<String?> get() = _selectedHomeId

    fun selectHome(homeId: String) {
        _selectedHomeId.value = homeId
    }
}
