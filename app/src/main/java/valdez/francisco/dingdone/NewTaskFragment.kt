package valdez.francisco.dingdone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import com.google.firebase.firestore.FirebaseFirestore

class NewTaskFragment : Fragment() {

    private lateinit var taskName: EditText
    private lateinit var taskDesc: EditText
    private lateinit var saveButton: Button
    private lateinit var memberSpinner: Spinner
    private lateinit var chipsContainer: LinearLayout
    private lateinit var dayButtons: List<Button>

    private val userList = mutableListOf<UserData>()
    private val selectedDays = mutableSetOf<String>()

    private val taskViewModel: TaskViewModel by viewModels()
    private val homeViewModel: HomeShareViewModel by activityViewModels()

    private val memberMap = mutableMapOf<String, String>()
    private val houseMemberNames = mutableListOf<String>()
    private var isSpinnerInitialized = false

    private var mode: String = "create"
    private var taskId: String? = null

    private fun validateBeforeSaving(): Boolean {

        if (taskName.text.isBlank()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return false
        }

        if (taskDesc.text.isBlank()) {
            Toast.makeText(requireContext(), "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(requireContext(), "Debes seleccionar al menos un día", Toast.LENGTH_SHORT).show()
            return false
        }

        if (userList.isEmpty()) {
            Toast.makeText(requireContext(), "Debes asignar al menos un miembro", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_new_task, container, false)

        taskName = view.findViewById(R.id.taskName)
        taskDesc = view.findViewById(R.id.taskDesc)
        saveButton = view.findViewById(R.id.saveButton)
        memberSpinner = view.findViewById(R.id.memberSpinner)
        chipsContainer = view.findViewById(R.id.chipsContainer)

        mode = arguments?.getString("mode") ?: "create"
        taskId = arguments?.getString("taskId")

        val daysContainer = view.findViewById<GridLayout>(R.id.daysContainer)
        dayButtons = (0 until daysContainer.childCount).map { daysContainer.getChildAt(it) as Button }

        val backButtonHeader: ImageButton = view.findViewById(R.id.backButtonHeader)
        backButtonHeader.setOnClickListener { parentFragmentManager.popBackStack() }

        loadHouseMembers()
        configureDays()
        configureSpinner()
        configureInputs()

        if (mode == "create") enableSaveButton()
        saveButton.setOnClickListener {

            if (!validateBeforeSaving()) return@setOnClickListener

            if (mode == "edit") updateTask()
            else createTask()
        }

        if (mode == "edit" && taskId != null) {
            saveButton.isEnabled = false
            saveButton.alpha = 0.5f
            loadTaskData(taskId!!)
        }

        return view
    }


    private fun enableSaveButton() {
        saveButton.isEnabled = true
        saveButton.alpha = 1f
        saveButton.setBackgroundResource(R.drawable.button_enabled)
    }

    private fun loadTaskData(id: String) {
        val homeId = homeViewModel.selectedHomeId.value ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) return@addOnSuccessListener

                taskName.setText(doc.getString("nombre") ?: "")
                taskDesc.setText(doc.getString("descripcio") ?: "")

                val members = doc.get("member") as? List<String> ?: emptyList()
                val days = doc.get("date") as? List<String> ?: emptyList()

                members.forEach { name -> addChip(name) }

                dayButtons.forEach { button ->
                    val day = date(button.text.toString())
                    if (day in days) {
                        button.performClick()
                    }
                }

                saveButton.text = "Guardar cambios"

                configureInputs()
            }
    }

    private fun createTask() {
        val homeId = homeViewModel.selectedHomeId.value ?: return
        val db = FirebaseFirestore.getInstance()

        val taskRef = db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document()

        val task = Task(
            id = taskRef.id,
            nombre = taskName.text.toString(),
            descripcio = taskDesc.text.toString(),
            member = userList.map { it.nombre },
            assignedTo = userList.map { it.id },
            date = selectedDays.toList(),
            state = "Pendiente",
            editableBy = emptyList()
        )

        taskRef.set(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Tarea creada!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateTask() {
        val homeId = homeViewModel.selectedHomeId.value ?: return
        val id = taskId ?: return

        val db = FirebaseFirestore.getInstance()

        val updates = mapOf(
            "nombre" to taskName.text.toString(),
            "descripcio" to taskDesc.text.toString(),
            "member" to userList.map { it.nombre },
            "assignedTo" to userList.map { it.id },
            "date" to selectedDays.toList()
        )

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document(id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cambios guardados!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_LONG).show()
            }
    }

    private fun configureDays() {
        dayButtons.forEach { button ->
            button.tag = "unselected"
            button.setOnClickListener {
                val day = button.text.toString()

                if (button.tag == "selected") {
                    button.setBackgroundResource(R.drawable.button_outline)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    button.tag = "unselected"
                    selectedDays.remove(date(day))
                } else {
                    button.setBackgroundResource(R.drawable.button_enabled)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    button.tag = "selected"
                    selectedDays.add(date(day))
                }

                enableSaveButton()
            }
        }
    }

    private fun configureSpinner() {
        memberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                val name = parent.getItemAtPosition(position).toString()
                if (name.isNotBlank() && position > 0) {
                    addChip(name)
                    enableSaveButton()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configureInputs() {
        taskName.addTextChangedListener { enableSaveButton() }
        taskDesc.addTextChangedListener { enableSaveButton() }
    }


    private fun loadHouseMembers() {
        val homeId = homeViewModel.selectedHomeId.value ?: return

        FirebaseFirestore.getInstance()
            .collection("homes")
            .document(homeId)
            .get()
            .addOnSuccessListener { doc ->
                val memberIds = doc.get("members") as? List<String> ?: emptyList()

                if (memberIds.isEmpty()) {
                    memberSpinner.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Seleccionar miembro...")
                    )
                    isSpinnerInitialized = false
                    return@addOnSuccessListener
                }

                houseMemberNames.clear()
                var loadedCount = 0

                memberIds.forEach { userId ->
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val userName = userDoc.getString("name") ?: "Unknown"
                            memberMap[userName] = userId
                            houseMemberNames.add(userName)

                            loadedCount++
                            if (loadedCount == memberIds.size) {
                                val items = mutableListOf("Seleccionar miembro...")
                                items.addAll(houseMemberNames)
                                memberSpinner.adapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_dropdown_item,
                                    items
                                )
                                isSpinnerInitialized = false
                            }
                        }
                }
            }
    }


    private fun addChip(name: String) {
        if (userList.any { it.nombre == name }) return

        val userId = memberMap[name] ?: return

        val chip = TextView(requireContext()).apply {
            text = "$name ✕"
            setPadding(24, 8, 24, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.button_enabled)
            setOnClickListener {
                chipsContainer.removeView(this)
                userList.removeIf { it.nombre == name }
                enableSaveButton()
            }
        }

        chipsContainer.addView(chip)
        userList.add(UserData(userId, name))
        enableSaveButton()
    }


    fun date(day: String): String = when (day) {
        "Mon" -> "Lunes"
        "Tue" -> "Martes"
        "Wed" -> "Miercoles"
        "Thu" -> "Jueves"
        "Fri" -> "Viernes"
        "Sat" -> "Sabado"
        "Sun" -> "Domingo"
        else -> day
    }
}
