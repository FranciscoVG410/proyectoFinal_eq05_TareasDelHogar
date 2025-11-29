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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView


class ProfileFragment : Fragment() {
    private lateinit var tvCompleteName: TextView
    private lateinit var tvRealEmail: TextView
    private lateinit var tvNumberCompletedTasks: TextView
    private lateinit var btnLogout: Button

    private lateinit var tvProgressStatus: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var llHousesSection: LinearLayout
    private lateinit var llHousesContainer: LinearLayout

    private lateinit var llTasksSection: LinearLayout
    private lateinit var llTasksHeader: LinearLayout
    private lateinit var ivTasksExpand: ImageView
    private lateinit var rvUserTasks: RecyclerView
    private lateinit var tasksAdapter: TaskDateAdapterNew
    private var isTasksExpanded = true

    private lateinit var layoutSuccess: View
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    private val uvm: UserViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tvCompleteName = view.findViewById(R.id.tv_completeName)
        tvRealEmail = view.findViewById(R.id.tv_realEmail)
        tvNumberCompletedTasks = view.findViewById(R.id.tv_numberCompletedTasks)
        btnLogout = view.findViewById(R.id.btn_logout)

        tvProgressStatus = view.findViewById(R.id.tv_progressStatus)
        progressBar = view.findViewById(R.id.progressBar_assignments)

        llHousesSection = view.findViewById(R.id.ll_housesSection)
        llHousesContainer = view.findViewById(R.id.ll_housesContainer)

        llTasksSection = view.findViewById(R.id.ll_tasksSection)
        llTasksHeader = view.findViewById(R.id.ll_tasksHeader)
        ivTasksExpand = view.findViewById(R.id.iv_tasksExpand)
        rvUserTasks = view.findViewById(R.id.rv_userTasks)

        rvUserTasks.layoutManager = LinearLayoutManager(requireContext())
        tasksAdapter = TaskDateAdapterNew(emptyList(), "")
        rvUserTasks.adapter = tasksAdapter

        val inflate = layoutInflater
        layoutSuccess = inflate.inflate(R.layout.custome_toast_success, null)
        textSuccess = layoutSuccess.findViewById(R.id.txtTextToastS)
        toast = Toast(context)

        setupObservers()
        setupTasksCollapse()
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
            activity?.finish()
        }
        return null
    }

    private fun setupObservers(){

        uvm.userTaskProgress.observe(viewLifecycleOwner){ progress ->

            progressBar.progress = progress.progressPercentage
            if(progress.totalTasks == 0){

                tvProgressStatus.text = "Sin tareas asignadas"

            }else{

                tvProgressStatus.text = "${progress.completedTasks}/${progress.totalTasks} hechas (${progress.progressPercentage}%)"

            }

        }

        uvm.userTasksGroupedByHome.observe(viewLifecycleOwner) { homeTasksPairs ->
            displayUserTasksByHome(homeTasksPairs)
        }

        uvm.userHomes.observe(viewLifecycleOwner) { allHomes ->
            currentUserId?.let { uid ->
                displayUserHomes(allHomes, uid)
            }
        }
    }

    private fun setupTasksCollapse() {
        llTasksHeader.setOnClickListener {
            isTasksExpanded = !isTasksExpanded
            if (isTasksExpanded) {
                rvUserTasks.visibility = View.VISIBLE
                ivTasksExpand.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                rvUserTasks.visibility = View.GONE
                ivTasksExpand.setImageResource(android.R.drawable.arrow_down_float)
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
            uvm.loadUserTasksGroupedByHome(uid)
            uvm.loadUserHomes(uid)
        } else {
        }
    }

    private fun displayUserTasksByHome(homeTasksPairs: List<Pair<Home, List<Task>>>) {
        val items = mutableListOf<TaskListItem>()
        val weekDays = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo")

        var hasAnyTasks = false

        homeTasksPairs.forEach { (home, tasks) ->
            val pendingTasks = tasks.filter { it.state != "Completada" }

            if (pendingTasks.isNotEmpty()) {
                hasAnyTasks = true
                items.add(TaskListItem.HomeHeader(home.name))

                val groupedByDay = mutableMapOf<String, MutableList<Task>>()
                pendingTasks.forEach { task ->
                    task.date.forEach { day ->
                        groupedByDay.getOrPut(day) { mutableListOf() }.add(task)
                    }
                }

                weekDays.forEach { day ->
                    val dayTasks = groupedByDay[day]
                    if (!dayTasks.isNullOrEmpty()) {
                        items.add(TaskListItem.Header(day))
                        items.addAll(dayTasks.map { TaskListItem.TaskItem(it, home.id) })
                    }
                }
            }
        }

        if (hasAnyTasks) {
            llTasksSection.visibility = View.VISIBLE
            tasksAdapter.updateItem(items)
        } else {
            llTasksSection.visibility = View.GONE
        }
    }

    private fun displayUserHomes(allHomes: List<Home>, currentUserId: String) {
        llHousesContainer.removeAllViews()
        llHousesSection.visibility = View.GONE

        val ownedHomes = allHomes.filter { it.ownerId == currentUserId }
        val memberHomes = allHomes.filter { it.ownerId != currentUserId }

        var isVisible = false

        if (ownedHomes.isNotEmpty()) {
            displayHomeSectionHeader("My Houses")
            ownedHomes.forEach { home ->
                displayHouse(home, isOwner = true)
            }
            isVisible = true
        }

        if (memberHomes.isNotEmpty()) {
            displayHomeSectionHeader("Member of")
            memberHomes.forEach { home ->
                displayHouse(home, isOwner = false)
            }
            isVisible = true
        }

        if (isVisible) {
            llHousesSection.visibility = View.VISIBLE
        }
    }

    private fun displayHomeSectionHeader(title: String) {
        val header = TextView(requireContext()).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
                bottomMargin = 8
            }
        }
        llHousesContainer.addView(header)
    }

    private fun displayHouse(home: Home, isOwner: Boolean) {

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

            setOnClickListener {
                navigateToHomeProfile(home, isOwner)
            }
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

        if (isOwner) {
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

                    it.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            houseLayout.addView(houseInfoText)
            houseLayout.addView(copyButton)
        } else {
            houseLayout.addView(houseInfoText)
        }

        llHousesContainer.addView(houseLayout)
    }

    private fun navigateToHomeProfile(home: Home, isOwner: Boolean) {
        val fragment = HomeProfileFragment.newInstance(home, isOwner)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}