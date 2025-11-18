package valdez.francisco.dingdone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
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
    private val houseMemberNames = mutableListOf<String>()
    private var isSpinnerInitialized = false





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_task, container, false)


        val homes = listOf<Home>() // tu lista real

        taskName = view.findViewById(R.id.taskName)
        taskDesc = view.findViewById(R.id.taskDesc)
        saveButton = view.findViewById(R.id.saveButton)
        memberSpinner = view.findViewById(R.id.memberSpinner)
        chipsContainer = view.findViewById(R.id.chipsContainer)
        
        // Back button in header
        val backButtonHeader: ImageButton = view.findViewById(R.id.backButtonHeader)
        backButtonHeader.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Days buttons
        val daysContainer = view.findViewById<GridLayout>(R.id.daysContainer)
        dayButtons = (0 until daysContainer.childCount).map { daysContainer.getChildAt(it) as Button }

        // Load house members dynamically
        loadHouseMembers()

        // Handle day selection - allow multiple selection
        dayButtons.forEach { button ->
            Log.d("Dias", button.text.toString())
            button.tag = "unselected" // Initialize tag

            button.setOnClickListener {
                val day = button.text.toString()
                
                if (button.tag == "selected") {
                    // Deselect
                    button.setBackgroundResource(R.drawable.button_outline)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    button.tag = "unselected"
                    selectedDays.remove(date(day))
                } else {
                    // Select
                    button.setBackgroundResource(R.drawable.button_enabled)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    button.tag = "selected"
                    selectedDays.add(date(day))
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

        // Validation listeners
        taskName.setOnFocusChangeListener { _, _ -> checkFormState() }
        taskDesc.setOnFocusChangeListener { _, _ -> checkFormState() }

        saveButton.setOnClickListener {

            val homeId = homeViewModel.selectedHomeId.value

            if (homeId == null) {
                Toast.makeText(requireContext(), "No hay un hogar seleccionado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val task = createTask()

            FirebaseFirestore.getInstance()
                .collection("homes")
                .document(homeId)
                .collection("tasks")
                .add(task)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Tarea creada!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_LONG).show()
                }
        }



        // Bottom back button
        val backButton: Button = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun loadHouseMembers() {
        val homeId = homeViewModel.selectedHomeId.value
        
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

                    memberIds.forEach { userId ->
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc != null && userDoc.exists()) {
                                    val userName = userDoc.getString("name") ?: "Unknown"
                                    houseMemberNames.add(userName)
                                }
                                
                                loadedCount++
                                if (loadedCount == memberIds.size) {
                                    val membersWithPlaceholder = mutableListOf("Seleccionar miembro...")
                                    membersWithPlaceholder.addAll(houseMemberNames)
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
                                    membersWithPlaceholder.addAll(houseMemberNames)
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
        userList.add(UserData(name))
        checkFormState()
    }

    private fun checkFormState(isButtonClick: Boolean = false) {
        val valid = taskName.text.isNotBlank() && taskDesc.text.isNotBlank()
        val anyDaySelected = dayButtons.any { it.tag == "selected" }
        val anyNameInChips = chipsContainer.childCount > 0


        if (valid && anyDaySelected && anyNameInChips) {
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

    fun createTask(): Task {
        return Task(
            nombre = taskName.text.toString(),
            descripcio = taskDesc.text.toString(),
            member = userList.map { it.nombre },
            date = selectedDays.toList(),
            state = "Pendiente"
        )
    }

}
