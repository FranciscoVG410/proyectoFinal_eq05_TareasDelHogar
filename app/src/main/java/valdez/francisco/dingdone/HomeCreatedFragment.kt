package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast



class HomeCreatedFragment : Fragment() {

    private lateinit var layoutSuccess: View
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_home_created, container, false)

        val inflate = layoutInflater
        layoutSuccess = inflate.inflate(R.layout.custome_toast_success, null)
        textSuccess = layoutSuccess.findViewById(R.id.txtTextToastS)
        toast = Toast(context)

        val btnGoHome: Button = view.findViewById(R.id.btnGoHome)
        val tvInvitationCode: TextView = view.findViewById(R.id.tv_codeCreated)
        val btnCopy: Button = view.findViewById(R.id.btnCopyCode)

        val codigo = arguments?.getString("invitationCode")
        tvInvitationCode.text = codigo ?: ""

        btnCopy.setOnClickListener {
            val codeToCopy = tvInvitationCode.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("Invitation Code", codeToCopy)

            clipboard.setPrimaryClip(clip)

            textSuccess.text = "Invitation code copied!"
            toast.duration = Toast.LENGTH_SHORT
            toast.view = layoutSuccess
            toast.show()

        }

        btnGoHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TasksFragmentNew())
                .commit()
        }

        return view
    }
}

