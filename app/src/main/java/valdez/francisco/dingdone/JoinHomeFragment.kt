package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class JoinHomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_join_home, container, false)

        val btnJoinHome: Button = view.findViewById(R.id.btnCreateHome)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)

        btnJoinHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        return view
    }
}

