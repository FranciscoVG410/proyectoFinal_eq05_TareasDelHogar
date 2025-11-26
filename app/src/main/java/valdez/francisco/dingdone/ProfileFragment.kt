package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileFragment : Fragment() {
    private lateinit var tvCompleteName: TextView
    private lateinit var tvRealEmail: TextView
    private lateinit var tvNumberCompletedTasks: TextView
    private lateinit var btnLogout: Button

    private lateinit var tvProgressStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val uvm: UserViewModel by viewModels()
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

        tvProgressStatus = view.findViewById(R.id.tv_numberCompletedTasks)
        progressBar = view.findViewById(R.id.progressBar_assignments)

        setupObservers()
        loadUserData()

        btnLogout.setOnClickListener{

            auth.signOut()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()

        }

        return view
    }

    private fun setupObservers(){

            uvm.userTaskProgress.observe(viewLifecycleOwner){ progress ->

                tvNumberCompletedTasks.text = progress.completedTasks.toString()
                progressBar.progress = progress.progressPercentage
                if(progress.totalTasks == 0){

                    tvProgressStatus.text = "Sin tareas asignadas"

                }else{

                    tvProgressStatus.text = "${progress.completedTasks}/${progress.totalTasks} hechas (${progress.progressPercentage}%)"

                }

            }

    }

    private fun loadUserData() {

        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            db.collection("users").document(uid).get().
            addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")

                        tvCompleteName.text = name
                        tvRealEmail.text = email

                    }
                }
                .addOnFailureListener { e ->
                    tvCompleteName.text = "Error"
                }
            uvm.loadUserAssignedTasksProgress(uid)
        } else {

        }
    }
}