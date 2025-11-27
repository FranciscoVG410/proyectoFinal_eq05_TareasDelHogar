package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
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
        val miembros = arguments?.getStringArrayList("miembros") ?: arrayListOf()
        val editableBy = arguments?.getStringArrayList("editableBy") ?: arrayListOf()

        val tvNombre: TextView = view.findViewById(R.id.tvTituloDetail)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionDetal)
        val btnChangeState: Button = view.findViewById(R.id.btnChangeState)
        val chgMiembros: ChipGroup = view.findViewById(R.id.chgMembersTaskDetal)
        val btnReturn: Button = view.findViewById(R.id.btnBackHome)

        val todayDate = getTodayDateString()

        tvNombre.text = nombre
        tvDescripcion.text = descripcion

        btnChangeState.text = estado
        if (btnChangeState.text == "Completada") {
            btnChangeState.setBackgroundResource(R.drawable.item_completed)
        } else if (btnChangeState.text == "Pendiente") {
            btnChangeState.setBackgroundResource(R.drawable.item_pending)
        }



        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {

            db.collection("homes").document(homeId)
                .collection("tasks").document(taskId)
                .collection("history").document(todayDate)
                .get().addOnSuccessListener { document ->

                    if(document.exists()){

                        btnChangeState.text = "Completada"
                        btnChangeState.setBackgroundResource(R.drawable.item_completed)

                    }else {

                        btnChangeState.text = "Pendiente"
                        btnChangeState.setBackgroundResource(R.drawable.item_pending)

                    }

                }

            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { userDoc ->
                    currentUserName = userDoc.getString("name") ?: ""
                    
                    db.collection("homes").document(homeId).get()
                        .addOnSuccessListener { homeDoc ->
                            homeOwnerId = homeDoc.getString("ownerId") ?: ""
                            
                            canEdit = currentUserId == homeOwnerId || editableBy.contains(currentUserName)
                            
                            btnChangeState.isEnabled = canEdit
                            if (!canEdit) {
                                btnChangeState.alpha = 0.5f
                            }
                            
                            if (currentUserId == homeOwnerId) {
                                showEditPermissionsUI(view, miembros, editableBy, homeId, taskId)
                            }
                        }
                }
        }


        btnChangeState.setOnClickListener {
            if (!canEdit) {
                Toast.makeText(requireContext(), "No tienes permiso para editar esta tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var currentState = btnChangeState.text.toString()
            val hisotryRef = db.collection("homes").document(homeId)
                .collection("tasks").document(taskId)
                .collection("history").document(todayDate)

            if(currentState == "Pendiente"){

                    val completionData = hashMapOf(

                        "completedBy" to auth.currentUser?.uid,
                        "completedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "status" to "Completada"

                    )
                hisotryRef.set(completionData)
                    .addOnSuccessListener {

                        btnChangeState.text = "Completada"
                        btnChangeState.setBackgroundResource(R.drawable.item_completed)
                        textSuccess.text = "tasks completed today!"
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = layoutSucces
                        toast.show()

                    }
                    .addOnFailureListener{

                        textFail.text = "fail to complete the task"
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = layoutFail
                        toast.show()

                    }

            }else {

                hisotryRef.delete()
                    .addOnSuccessListener {

                        btnChangeState.text = "Pendiente"
                        btnChangeState.setBackgroundResource(R.drawable.item_pending)
                        textSuccess.text = "tasks mark pendind!"
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = layoutSucces
                        toast.show()

                    }
                    .addOnFailureListener{

                        textFail.text = "fail to update task"
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = layoutFail
                        toast.show()

                    }

            }

            val newStatus = if (btnChangeState.text == "Completada") "Pendiente" else "Completada"
            
            taskViewModel.updateTaskStatus(homeId, taskId, newStatus) { success ->
                if (success) {
                    btnChangeState.text = newStatus
                    estado = newStatus
                    if (newStatus == "Completada") {
                        btnChangeState.setBackgroundResource(R.drawable.item_completed)
                    } else {
                        btnChangeState.setBackgroundResource(R.drawable.item_pending)
                    }
                    Toast.makeText(requireContext(), "Estado actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReturn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        chgMiembros.removeAllViews()

        miembros.forEach { member: String ->
            val chipContext = ContextThemeWrapper(chgMiembros.context, com.google.android.material.R.style.Theme_MaterialComponents_Light)
            val chip = Chip(chipContext).apply {
                text = member
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.btnBackground)
                setTextColor(ContextCompat.getColor(chgMiembros.context, R.color.white))
                isCloseIconVisible = false
            }
            chgMiembros.addView(chip)
        }

        return view
    }

    private fun getTodayDateString(): String{

        val sdf =  SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())

    }

    private fun showEditPermissionsUI(
        view: View,
        miembros: ArrayList<String>,
        editableBy: ArrayList<String>,
        homeId: String,
        taskId: String
    ) {
        val mainLayout = view.findViewById<LinearLayout>(R.id.main)
        
        val permissionsSection = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        
        val permissionsTitle = TextView(requireContext()).apply {
            text = "Miembros que pueden editar:"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setPadding(0, 0, 0, 16)
        }
        permissionsSection.addView(permissionsTitle)
        
        val checkboxes = mutableListOf<Pair<CheckBox, String>>()
        
        miembros.forEach { member ->
            val checkbox = CheckBox(requireContext()).apply {
                text = member
                isChecked = editableBy.contains(member)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            permissionsSection.addView(checkbox)
            checkboxes.add(Pair(checkbox, member))
        }
        
        val savePermissionsBtn = Button(requireContext()).apply {
            text = "Guardar permisos"
            setBackgroundResource(R.drawable.button_enabled)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
        }
        
        savePermissionsBtn.setOnClickListener {
            val newEditableBy = checkboxes.filter { it.first.isChecked }.map { it.second }
            taskViewModel.updateEditableMembers(homeId, taskId, newEditableBy) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Permisos actualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar permisos", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        permissionsSection.addView(savePermissionsBtn)
        
        mainLayout.addView(permissionsSection, mainLayout.childCount)
    }
}

