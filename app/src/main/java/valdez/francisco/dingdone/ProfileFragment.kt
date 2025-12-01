package valdez.francisco.dingdone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


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
    private lateinit var btnEditarNombe: ImageButton
    private lateinit var btnEditarProfileImage: ImageButton
    private lateinit var ll_congrats: LinearLayout
    private lateinit var ivProfileAvatar: ImageView

    private var isTasksExpanded = true

    private lateinit var layoutSuccess: View
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    private val uvm: UserViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

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
        ll_congrats = view.findViewById(R.id.ll_congrats)
        ivProfileAvatar = view.findViewById(R.id.iv_profileAvatar)

        btnEditarNombe = view.findViewById(R.id.btn_editName)
        btnEditarProfileImage = view.findViewById(R.id.btn_editAvatar)

        btnEditarNombe.setOnClickListener{

            showEditNameDialog()

        }

        btnEditarProfileImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        ll_congrats.visibility = View.GONE

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

    private fun showEditNameDialog() {
        val context = requireContext()
        val builder = android.app.AlertDialog.Builder(context)

        val dialogLayout = LinearLayout(context)
        dialogLayout.orientation = LinearLayout.VERTICAL
        dialogLayout.setPadding(50, 40, 50, 10)

        val input = android.widget.EditText(context)
        input.hint = "Nuevo nombre"
        input.setText(tvCompleteName.text.toString())
        dialogLayout.addView(input)

        builder.setView(dialogLayout)
        builder.setTitle("Editar Nombre")

        builder.setPositiveButton("Guardar") { dialog, _ ->

            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {

                updateUserName(newName)

            } else {

                Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()

            }

            dialog.dismiss()

        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->

            dialog.cancel()

        }

        builder.show()

    }

    private fun updateUserName(newName: String) {

        val uid = currentUserId
        if (uid != null) {

            db.collection("users").document(uid)
                .update("name", newName)
                .addOnSuccessListener {

                    tvCompleteName.text = newName

                    textSuccess.text = "Nombre actualizado correctamente"
                    toast.duration = Toast.LENGTH_SHORT
                    toast.view = layoutSuccess
                    toast.show()
                    uvm.loadUserTasksGroupedByHome(uid)

                }
                .addOnFailureListener {

                    Toast.makeText(context, "Error al actualizar nombre", Toast.LENGTH_SHORT).show()

                }

        }

    }

    private fun uploadProfileImage(uri: Uri) {
        try {
            val imageStream = requireContext().contentResolver.openInputStream(uri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)

            val scaledBitmap = scaleBitmap(selectedImage, 500)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // 70% quality
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            saveProfileImageBase64(base64String)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error procesando imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageBase64(base64String: String) {
        val uid = currentUserId ?: return

        db.collection("users").document(uid)
            .update("profileImage", base64String)
            .addOnSuccessListener {
                loadProfileImage(base64String) // Display immediately
                textSuccess.text = "Foto actualizada"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutSuccess
                toast.show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(base64String: String?) {
        if (base64String.isNullOrEmpty()) return

        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            ivProfileAvatar.load(decodedBitmap) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.circle_profile)
                error(R.drawable.circle_profile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var newWidth = maxDimension
        var newHeight = maxDimension

        if (originalWidth > originalHeight) {
            newHeight = (newWidth * originalHeight) / originalWidth
        } else {
            newWidth = (newHeight * originalWidth) / originalHeight
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }


    private fun setupObservers(){

        uvm.userTaskProgress.observe(viewLifecycleOwner){ progress ->

            progressBar.max = 100
            progressBar.setProgress(progress.progressPercentage, true)
            if(progress.totalTasks == 0){

                tvProgressStatus.text = "Sin tareas para esta semana"
                ll_congrats.visibility = View.GONE

            }else if(progress.progressPercentage == 100){

                tvProgressStatus.text = "${progress.completedTasks}/${progress.totalTasks} hechas (${progress.progressPercentage}%)"
                ll_congrats.visibility = View.VISIBLE

            }else {

                tvProgressStatus.text = "${progress.completedTasks}/${progress.totalTasks} hechas (${progress.progressPercentage}%)"
                ll_congrats.visibility = View.GONE

            }

        }

        uvm.totalLifetimeCompleted.observe(viewLifecycleOwner){ totalCount ->

            tvNumberCompletedTasks.text = totalCount.toString()

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
                    val profileImageBase64 = document.getString("profileImage")

                    tvCompleteName.text = name
                    tvRealEmail.text = email
                    loadProfileImage(profileImageBase64)

                }
            }
                .addOnFailureListener { e ->
                    tvCompleteName.text = "Error"
                }
            uvm.loadUserAssignedTasksProgress(uid)
            uvm.loadUserHomes(uid)
            uvm.loadUserTasksGroupedByHome(uid)
            uvm.loadTotalCompletedTasks(uid)

        } else {
        }
    }

    private fun displayUserTasksByHome(homeTasksPairs: List<Pair<Home, List<Task>>>) {

        val allItems = mutableListOf<TaskListItem>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val currentUserName = tvCompleteName.text.toString().trim()

        val dayMap = mapOf(

            Calendar.MONDAY to "Lunes",
            Calendar.TUESDAY to "Martes",
            Calendar.WEDNESDAY to "Miercoles",
            Calendar.THURSDAY to "Jueves",
            Calendar.FRIDAY to "Viernes",
            Calendar.SATURDAY to "Sabado",
            Calendar.SUNDAY to "Domingo"

        )

        android.util.Log.d("ProfileDebug", "Buscando tareas para el nombre: '$currentUserName'")

        homeTasksPairs.forEach { (home, tasks) ->

            val myTasks = tasks.filter { task ->

                task.assignedTo.contains(currentUserId)

            }

            if (myTasks.isNotEmpty()) {
                val itemsForThisHome = mutableListOf<TaskListItem>()
                val loopCalendar = Calendar.getInstance()

                for (i in 0..6) {
                    val currentDayInt = loopCalendar.get(Calendar.DAY_OF_WEEK)
                    val currentDayString = dayMap[currentDayInt] ?: ""
                    val currentDateCode = dateFormat.format(loopCalendar.time)

                    val tasksForDay = myTasks.filter { task ->

                        task.date.any { diaGuardado ->

                            diaGuardado.equals(currentDayString, ignoreCase = true)

                        }
                    }

                    if (tasksForDay.isNotEmpty()) {


                        itemsForThisHome.add(TaskListItem.Header(currentDayString))
                        itemsForThisHome.addAll(tasksForDay.map {
                            TaskListItem.TaskItem(it, home.id, currentDateCode)

                        })

                    }

                    loopCalendar.add(Calendar.DAY_OF_YEAR, 1)

                }

                if (itemsForThisHome.isNotEmpty()) {

                    allItems.add(TaskListItem.HomeHeader(home.name))
                    allItems.addAll(itemsForThisHome)

                }
            }
        }

        if (allItems.isNotEmpty()) {

            llTasksSection.visibility = View.VISIBLE
            tasksAdapter.updateItem(allItems)

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

//        if (isOwner) {
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
//        } else {
//            houseLayout.addView(houseInfoText)
//        }

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