package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileFragment : Fragment() {
    private lateinit var tvCompleteName: TextView
    private lateinit var tvRealEmail: TextView
    private lateinit var tvNumberCompletedTasks: TextView
    private lateinit var btnLogout: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tvCompleteName = view.findViewById(R.id.tv_completeName)
        tvRealEmail = view.findViewById(R.id.tv_realEmail)
        tvNumberCompletedTasks = view.findViewById(R.id.tv_numberCompletedTasks)
        btnLogout = view.findViewById(R.id.btn_logout)

        loadUserData()
        setupLogoutButton()
        return view
    }

    private fun setupLogoutButton() {
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            db.collection("users").document(uid).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")
                        val completedTasks = document.getLong("completedTasks") ?: 0L

                        tvCompleteName.text = name
                        tvRealEmail.text = email
                        tvNumberCompletedTasks.text = completedTasks.toString()

                    } else {

                    }
                }
                .addOnFailureListener { e ->
                    tvCompleteName.text = "Error"
                }
        } else {

        }
    }
}