package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlin.random.Random

class CreateHomeFragment : Fragment() {

    private lateinit var layoutFail: View
    private lateinit var layoutSucces: View
    private lateinit var textFail: TextView
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_home, container, false)

        layoutFail = inflater.inflate(R.layout.custome_toast_fail, null)
        layoutSucces = inflater.inflate(R.layout.custome_toast_success, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        textSuccess = layoutSucces.findViewById(R.id.txtTextToastS)
        toast = Toast(requireContext())

        var etHomeName: EditText = view.findViewById(R.id.et_homeName)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnCreate: Button = view.findViewById(R.id.btnCreate)
        val tvRandomCode: TextView = view.findViewById(R.id.tv_randomCode)

        tvRandomCode.text = generateRandomCode()

        btnCreate.setOnClickListener {
            if (etHomeName.text.toString().isEmpty()) {
                textFail.text = "Please enter a name for the family"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()
                etHomeName.setBackgroundResource(R.drawable.error_rounded_edit_text)
            } else {
                val code = tvRandomCode.text.toString()
                val fragment = HomeCreatedFragment().apply {
                    arguments = Bundle().apply {
                        putString("invitationCode", code)
                    }
                }
                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.fragment_container, fragment)
                    addToBackStack(null)
                    commit()
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun generateRandomCode(): String {
        val letters = (1..3)
            .map { ('A'..'Z').random() }
            .joinToString("")
        val numbers = (1..3)
            .map { Random.nextInt(0, 9) }
            .joinToString("")
        return letters + numbers
    }
}

