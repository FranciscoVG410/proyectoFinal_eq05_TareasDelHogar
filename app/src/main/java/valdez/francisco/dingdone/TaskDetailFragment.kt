package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskDetailFragment : Fragment() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val homeViewModel: HomeShareViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var layoutSucces: View
    private lateinit var textSuccess: TextView
    private lateinit var layoutFail: View
    private lateinit var textFail: TextView
    private lateinit var toast: Toast

    private var currentUserName: String = ""
    private var homeOwnerId: String = ""
    private var canEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_task_detail, container, false)

        layoutFail = inflater.inflate(R.layout.custome_toast_fail, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        layoutSucces = inflater.inflate(R.layout.custome_toast_success, null)
        textSuccess = layoutSucces.findViewById(R.id.txtTextToastS)
        toast = Toast(requireContext())

        val taskId = arguments?.getString("taskId") ?: ""
        val homeId = arguments?.getString("homeId") ?: ""
        val nombre = arguments?.getString("nombre")
        val descripcion = arguments?.getString("descripcion")
        var estado = arguments?.getString("estado")
        var assignedTo = arguments?.getStringArrayList("assignedTo") ?: arrayListOf()
        val miembros = arguments?.getStringArrayList("miembros") ?: arrayListOf()
        val editableBy = arguments?.getStringArrayList("editableBy") ?: arrayListOf()
        val selectedDateArg = arguments?.getString("selectedDate")
        val targetDate = if(!selectedDateArg.isNullOrEmpty()) selectedDateArg else getTodayDateString()

        val tvNombre: TextView = view.findViewById(R.id.tvTituloDetail)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionDetal)
        val btnChangeState: Button = view.findViewById(R.id.btnChangeState)
        val chgMiembros: ChipGroup = view.findViewById(R.id.chgMembersTaskDetal)
        val btnReturn: Button = view.findViewById(R.id.btnBackHome)
        val btnEditTask: ImageView = view.findViewById(R.id.btnEditTask)

        val todayDate = getTodayDateString()

        tvNombre.text = nombre
        tvDescripcion.text = descripcion

        btnChangeState.text = estado

        if (estado == "Completada") {
            btnChangeState.setBackgroundResource(R.drawable.item_completed)
        } else {
            btnChangeState.setBackgroundResource(R.drawable.item_pending)
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {

            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { userDoc ->
                    currentUserName = userDoc.getString("name") ?: ""


                    db.collection("homes").document(homeId)
                        .get()
                        .addOnSuccessListener { homeDoc ->
                            homeOwnerId = homeDoc.getString("ownerId") ?: ""
                            var membersCanEdit = homeDoc.getBoolean("membersCanEdit") ?: true

                            canEdit = currentUserId == homeOwnerId ||
                                    membersCanEdit || currentUserName in miembros

                            btnChangeState.isEnabled = canEdit
                            btnChangeState.alpha = if (canEdit) 1f else 0.5f

                            btnEditTask.visibility = if (canEdit) View.VISIBLE else View.GONE

                        }
                }
        }

        btnEditTask.setOnClickListener {
            val frag = NewTaskFragment()
            val args = Bundle()

            args.putString("mode", "edit")
            args.putString("taskId", taskId)
            args.putString("homeId", homeId)
            args.putString("nombre", nombre)
            args.putString("descripcion", descripcion)
            args.putString("estado", estado)
            args.putStringArrayList("miembros", miembros)
            args.putStringArrayList("editableBy", editableBy)

            frag.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, frag)
                .addToBackStack(null)
                .commit()
        }

        btnChangeState.setOnClickListener {

            if(btnChangeState.text.toString() == "Completeada"){

                Toast.makeText(requireContext(), "Esta tarea ya fue finalizada.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            if(homeOwnerId == currentUserId || currentUserName in miembros || currentUserId in assignedTo){

                val historyRef = db.collection("homes").document(homeId)
                    .collection("tasks").document(taskId)
                    .collection("history").document(targetDate)

//                val isCurrentlyCompleted = btnChangeState.text.toString() == "Completada"

                val data = hashMapOf(
                    "completedBy" to currentUserId,
                    "completedAt" to targetDate,
                    "status" to "Completada",
                    "dateId" to targetDate
                )

                historyRef.set(data).addOnSuccessListener {

                    btnChangeState.text = "Completada"
                    btnChangeState.setBackgroundResource(R.drawable.item_completed)

                    textSuccess.text = "Tarea completada con éxito"
                    toast.duration = Toast.LENGTH_SHORT
                    toast.view = layoutSucces
                    toast.show()

                }.addOnFailureListener { e ->

                    textFail.text = "Error: ${e.message}"
                    toast.duration = Toast.LENGTH_SHORT
                    toast.view = layoutFail
                    toast.show()

                }

            }else {

                Toast.makeText(requireContext(), "No tienes permiso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

        }

        val historyCheckRef = db.collection("homes").document(homeId)
            .collection("tasks").document(taskId)
            .collection("history").document(targetDate)

        historyCheckRef.get().addOnSuccessListener { doc ->

            if(doc.exists() && doc.getString("status") == "Completada"){

                btnChangeState.text = "Completada"
                btnChangeState.setBackgroundResource(R.drawable.item_completed)
                btnChangeState.isEnabled = false
                btnChangeState.alpha = 0.5f

            }else {

                btnChangeState.text = "Pendiente"
                btnChangeState.setBackgroundResource(R.drawable.item_pending)
                btnChangeState.isEnabled = true
                btnChangeState.alpha = 1f

            }

        }

        btnReturn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        chgMiembros.removeAllViews()
        miembros.forEach { member ->
            val chipContext = ContextThemeWrapper(
                chgMiembros.context,
                com.google.android.material.R.style.Theme_MaterialComponents_Light
            )

            val chip = Chip(chipContext).apply {
                text = member
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.btnBackground)
                setTextColor(ContextCompat.getColor(chgMiembros.context, R.color.white))
            }

            chgMiembros.addView(chip)
        }

        return view
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

//    private fun showEditPermissionsUI(
//        view: View,
//        miembros: ArrayList<String>,
//        editableBy: ArrayList<String>,
//        homeId: String,
//        taskId: String
//    ) {
//        val mainLayout = view.findViewById<LinearLayout>(R.id.main)
//
//        val permissionsSection = LinearLayout(requireContext()).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(40, 20, 40, 20)
//        }
//
//        val title = TextView(requireContext()).apply {
//            text = "Members can edit:"
//            textSize = 16f
//            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
//        }
//        permissionsSection.addView(title)
//
//        val checkboxes = mutableListOf<Pair<CheckBox, String>>()
//        miembros.forEach { member ->
//            val chk = CheckBox(requireContext()).apply {
//                text = member
//                isChecked = editableBy.contains(member)
//            }
//            permissionsSection.addView(chk)
//            checkboxes.add(Pair(chk, member))
//        }
//
//        val btnSave = Button(requireContext()).apply {
//            text = "Save"
//            setBackgroundResource(R.drawable.button_enabled)
//            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//        }
//
//        btnSave.setOnClickListener {
//            val nuevos = checkboxes.filter { it.first.isChecked }.map { it.second }
//
//            taskViewModel.updateEditableMembers(homeId, taskId, nuevos) { ok ->
//                Toast.makeText(
//                    requireContext(),
//                    if (ok) "Updated" else "ERROR TO UPDATE",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//        permissionsSection.addView(btnSave)
//        mainLayout.addView(permissionsSection, mainLayout.childCount)
//    }
}
