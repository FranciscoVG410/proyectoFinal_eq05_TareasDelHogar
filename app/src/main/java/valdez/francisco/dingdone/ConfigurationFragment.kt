package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth

class ConfigurationFragment : Fragment() {

    private val uvm: UserViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var noTasksText: View
    private lateinit var layoutFail: View
    private lateinit var textFail: TextView
    private lateinit var toast: Toast

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuration, container, false)

        val inflate = layoutInflater
        layoutFail = inflate.inflate(R.layout.custome_toast_fail, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        toast = Toast(context)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            uvm.loadUserHomes(userId)
        }

        val btnCreateHome: Button = view.findViewById(R.id.btnCreateHome)
        val btnJoinHome: Button = view.findViewById(R.id.btnJoinHome)

        btnCreateHome.setOnClickListener {

            if(uvm.userHomes.value?.size!! < 5){

                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.fragment_container, CreateHomeFragment())
                    addToBackStack(null)
                    commit()
                }

            }else {

                textFail.text = "You’ve reached the limit of 5 homes"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()

            }

        }

        btnJoinHome.setOnClickListener {
            if(uvm.userHomes.value?.size!! < 5){

                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.fragment_container, JoinHomeFragment())
                    addToBackStack(null)
                    commit()
                }

            }else {

                textFail.text = "You’ve reached the limit of 5 homes"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()

            }

        }

        return view
    }
}