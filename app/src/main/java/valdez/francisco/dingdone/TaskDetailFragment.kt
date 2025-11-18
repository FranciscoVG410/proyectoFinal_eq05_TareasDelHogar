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

class TaskDetailFragment : Fragment() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val homeViewModel: HomeShareViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private var currentUserName: String = ""
    private var homeOwnerId: String = ""
    private var canEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_task_detail, container, false)

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

