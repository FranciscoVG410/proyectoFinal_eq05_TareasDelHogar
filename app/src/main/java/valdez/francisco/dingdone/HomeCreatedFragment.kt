package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeCreatedFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_home_created, container, false)

        val btnGoHome: Button = view.findViewById(R.id.btnGoHome)
        val tvInvitationCode: TextView = view.findViewById(R.id.tv_codeCreated)

        val codigo = arguments?.getString("invitationCode")
        tvInvitationCode.text = codigo ?: ""

        btnGoHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        
        return view
    }
}

