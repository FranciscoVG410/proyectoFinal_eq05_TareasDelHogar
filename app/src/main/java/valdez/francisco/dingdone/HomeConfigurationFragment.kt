package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeConfigurationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_home_configuration, container, false)

        val btnConfirm: Button = view.findViewById(R.id.btnConfirm)

        btnConfirm.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        return view
    }
}

