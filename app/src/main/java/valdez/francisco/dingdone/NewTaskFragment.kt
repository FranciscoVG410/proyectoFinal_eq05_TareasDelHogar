package valdez.francisco.dingdone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
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
    private var existingTask: Task? = null
    private var isEditMode: Boolean = false
    private var currentHomeId: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_task, container, false)

        arguments?.let {
            currentHomeId = it.getString(ARG_HOME_ID)
            existingTask = it.getParcelable(ARG_TASK)
            if (existingTask != null) {
                isEditMode = true
            }
        }

        if (currentHomeId == null) {
            Toast.makeText(requireContext(), "Error: ID de Home no disponible.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return view
        }

        taskName = view.findViewById(R.id.taskName)
        taskDesc = view.findViewById(R.id.taskDesc)
        saveButton = view.findViewById(R.id.saveButton)
        memberSpinner = view.findViewById(R.id.memberSpinner)
        chipsContainer = view.findViewById(R.id.chipsContainer)

        val backButtonHeader: ImageButton = view.findViewById(R.id.backButtonHeader)
        backButtonHeader.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val daysContainer = view.findViewById<GridLayout>(R.id.daysContainer)
        dayButtons = (0 until daysContainer.childCount).map { daysContainer.getChildAt(it) as Button }

        loadHouseMembers()

        if (isEditMode && existingTask != null) {
            val task = existingTask!!
            taskName.setText(task.nombre)
            taskDesc.setText(task.descripcio)
            saveButton.text = "Guardar Cambios"

            task.date.forEach { dayFull ->
                val dayShort = dayButtons.find { date(it.text.toString()) == dayFull }?.text.toString()

                dayButtons.find { it.text.toString() == dayShort }?.let { button ->
                    button.setBackgroundResource(R.drawable.button_enabled)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    button.tag = "selected"
                    selectedDays.add(dayFull)
                }
            }

            task.member.zip(task.assignedTo).forEach { (name, id) ->
                memberMap[name] = id
            }

        } else {
            saveButton.text = "Crear Tarea"
        }

        dayButtons.forEach { button ->
            Log.d("Dias", button.text.toString())
            if (button.tag != "selected") {
                button.tag = "unselected"
            }

            button.setOnClickListener {
                val dayShort = button.text.toString()
                val dayFull = date(dayShort)

                if (button.tag == "selected") {

                    button.setBackgroundResource(R.drawable.button_outline)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    button.tag = "unselected"
                    selectedDays.remove(dayFull)
                } else {

                    button.setBackgroundResource(R.drawable.button_enabled)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    button.tag = "selected"
                    selectedDays.add(dayFull)
                }

                checkFormState()
            }
        }

        memberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                val name = parent.getItemAtPosition(position).toString()
                if (name.isNotBlank() && position > 0) addChip(name)
                checkFormState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        taskName.setOnFocusChangeListener { _, _ -> checkFormState() }
        taskDesc.setOnFocusChangeListener { _, _ -> checkFormState() }

        saveButton.setOnClickListener {

            val homeId = currentHomeId

            if (homeId == null) {
                Toast.makeText(requireContext(), "No hay un hogar seleccionado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkFormState(true)) {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos requeridos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()

            if (isEditMode && existingTask != null) {
                val taskToUpdate = createTaskOrUpdate(existingTask!!.id)

                db.collection("homes").document(homeId)
                    .collection("tasks").document(taskToUpdate.id)
                    .set(taskToUpdate)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tarea actualizada!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NewTaskFragment", "Error updating task", e)
                        Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            } else {
                val taskRef = db.collection("homes")
                    .document(homeId)
                    .collection("tasks")
                    .document()

                val newTask = createTaskOrUpdate(taskRef.id)

                taskRef.set(newTask)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tarea creada!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NewTaskFragment", "Error creating task", e)
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        val backButton: Button = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun loadHouseMembers() {
        val homeId = currentHomeId

        if (homeId == null) {
            Toast.makeText(requireContext(), "No hay un hogar seleccionado", Toast.LENGTH_SHORT).show()
            val placeholder = listOf("Seleccionar miembro...")
            memberSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, placeholder)
            isSpinnerInitialized = false
            return
        }

        FirebaseFirestore.getInstance()
            .collection("homes")
            .document(homeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val memberIds = document.get("members") as? List<String> ?: emptyList()

                    if (memberIds.isEmpty()) {
                        houseMemberNames.clear()
                        val membersWithPlaceholder = listOf("Seleccionar miembro...")
                        memberSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, membersWithPlaceholder)
                        isSpinnerInitialized = false
                        return@addOnSuccessListener
                    }

                    houseMemberNames.clear()
                    var loadedCount = 0

                    val existingMembers = existingTask?.member ?: emptyList()

                    memberIds.forEach { userId ->
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc != null && userDoc.exists()) {
                                    val userName = userDoc.getString("name") ?: "Unknown"
                                    memberMap[userName] = userId
                                    houseMemberNames.add(userName)

                                    if (isEditMode && existingMembers.contains(userName)) {
                                        addChip(userName)
                                    }
                                }

                                loadedCount++
                                if (loadedCount == memberIds.size) {
                                    val membersWithPlaceholder = mutableListOf("Seleccionar miembro...")
                                    membersWithPlaceholder.addAll(houseMemberNames.sorted())
                                    memberSpinner.adapter = ArrayAdapter(
                                        requireContext(),
                                        android.R.layout.simple_spinner_dropdown_item,
                                        membersWithPlaceholder
                                    )
                                    isSpinnerInitialized = false
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("NewTaskFragment", "Error loading user $userId: $e")
                                loadedCount++
                                if (loadedCount == memberIds.size) {
                                    val membersWithPlaceholder = mutableListOf("Seleccionar miembro...")
                                    membersWithPlaceholder.addAll(houseMemberNames.sorted())
                                    memberSpinner.adapter = ArrayAdapter(
                                        requireContext(),
                                        android.R.layout.simple_spinner_dropdown_item,
                                        membersWithPlaceholder
                                    )
                                    isSpinnerInitialized = false
                                }
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Hogar no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w("NewTaskFragment", "Error loading home: $e")
                Toast.makeText(requireContext(), "Error al cargar miembros", Toast.LENGTH_SHORT).show()
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
                checkFormState()
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(8, 0, 8, 0)
            layoutParams = params
        }
        chipsContainer.addView(chip)
        userList.add(UserData(userId, name))
        checkFormState()
    }

    private fun checkFormState(isButtonClick: Boolean = false): Boolean {
        val valid = taskName.text.isNotBlank() && taskDesc.text.isNotBlank()
        val anyDaySelected = dayButtons.any { it.tag == "selected" }
        val anyNameInChips = chipsContainer.childCount > 0

        val overallValid = valid && anyDaySelected && anyNameInChips


        if (overallValid) {
            saveButton.isEnabled = true
            saveButton.setBackgroundResource(R.drawable.button_enabled)
            taskName.setBackgroundResource(R.drawable.edittext_bg)
            taskDesc.setBackgroundResource(R.drawable.edittext_bg)

        } else {
            saveButton.isEnabled = false
            saveButton.setBackgroundResource(R.drawable.button_disabled)
            if ((taskName.text.isBlank() || taskDesc.text.isBlank()) && isButtonClick) {
                taskName.setBackgroundResource(R.drawable.edittext_bg_error)
                taskDesc.setBackgroundResource(R.drawable.edittext_bg_error)
            }
        }
        return overallValid
    }

    fun date(date: String): String{

        return when(date){

            "Mon" -> "Lunes"
            "Tue" -> "Martes"
            "Wed" -> "Miercoles"
            "Thu" -> "Jueves"
            "Fri" -> "Viernes"
            "Sat" -> "Sabado"
            "Sun" -> "Domingo"
            else -> "Desconocido"

        }

    }

    fun createTaskOrUpdate(taskId: String): Task {
        return Task(
            id = taskId,
            nombre = taskName.text.toString(),
            descripcio = taskDesc.text.toString(),
            member = userList.map { it.nombre },
            assignedTo = userList.map { it.id },
            date = selectedDays.toList(),
            state = existingTask?.state ?: "Pendiente",
            completionDate = existingTask?.completionDate,
            stability = existingTask?.stability ?: 0,
            editableBy = existingTask?.editableBy ?: emptyList()
        )
    }

    companion object {
        private const val ARG_TASK = "task_object"
        private const val ARG_HOME_ID = "home_id"

        fun newInstance(homeId: String, task: Task? = null): NewTaskFragment {
            val fragment = NewTaskFragment()
            val args = Bundle().apply {
                putString(ARG_HOME_ID, homeId)
                task?.let { putParcelable(ARG_TASK, it) }
            }
            fragment.arguments = args
            return fragment
        }
    }
}