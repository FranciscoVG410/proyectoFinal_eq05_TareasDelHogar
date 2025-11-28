package valdez.francisco.dingdone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

    private lateinit var llHousesSection: LinearLayout
    private lateinit var llHousesContainer: LinearLayout

    private lateinit var layoutSuccess: View
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

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

        llHousesSection = view.findViewById(R.id.ll_housesSection)
        llHousesContainer = view.findViewById(R.id.ll_housesContainer)

        val inflate = layoutInflater
        layoutSuccess = inflate.inflate(R.layout.custome_toast_success, null)
        textSuccess = layoutSuccess.findViewById(R.id.txtTextToastS)
        toast = Toast(context)

        setupObservers()
        loadUserData()
        setupLogoutButton()
        return view
    }

    private fun setupLogoutButton(): View? {
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

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
            loadCreatedHomes(uid)
        } else {

        }
    }

    private fun loadCreatedHomes(userId: String) {
        db.collection("homes")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val homes = querySnapshot.toObjects(Home::class.java)
                if (homes.isNotEmpty()) {
                    llHousesSection.visibility = View.VISIBLE
                    llHousesContainer.removeAllViews()
                    homes.forEach { home ->
                        displayHouse(home)
                    }
                } else {
                    llHousesSection.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                llHousesSection.visibility = View.GONE
            }
    }

    private fun displayHouse(home: Home) {
        val houseItemView = layoutInflater.inflate(R.layout.custome_toast_success, llHousesContainer, false)
        
        val houseLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setPadding(12, 12, 12, 12)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_edit_text)
        }

        val displayName = if (home.name.length > 20) {
            val start = home.name.substring(0, 8)
            val end = home.name.substring(home.name.length - 8)
            "$start...$end"
        } else {
            home.name
        }

        val fullText = "$displayName → ${home.invitationCode}"
        val spannableString = SpannableString(fullText)
        val codeStartIndex = fullText.indexOf(home.invitationCode)
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            codeStartIndex,
            codeStartIndex + home.invitationCode.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val houseInfoText = TextView(requireContext()).apply {
            text = spannableString
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val copyButton = Button(requireContext()).apply {
            text = "Copy"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button_purple)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 0)
            }
            setPadding(24, 8, 24, 8)
            
            setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("House Code", home.invitationCode)
                clipboard.setPrimaryClip(clip)

                textSuccess.text = "House code copied!"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutSuccess
                toast.show()
            }
        }

        houseLayout.addView(houseInfoText)
        houseLayout.addView(copyButton)
        llHousesContainer.addView(houseLayout)
    }
}