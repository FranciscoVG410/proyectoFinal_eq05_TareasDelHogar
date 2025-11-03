package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class ConfigurationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuration, container, false)

        val btnCreateHome: Button = view.findViewById(R.id.btnCreateHome)
        val btnJoinHome: Button = view.findViewById(R.id.btnJoinHome)

        btnCreateHome.setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, CreateHomeFragment())
                addToBackStack(null)
                commit()
            }
        }

        btnJoinHome.setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, JoinHomeFragment())
                addToBackStack(null)
                commit()
            }
        }

        return view
    }
}