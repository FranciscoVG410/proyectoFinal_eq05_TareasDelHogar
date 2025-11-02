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

class NewTaskFragment : Fragment() {

    private lateinit var taskName: EditText
    private lateinit var taskDesc: EditText
    private lateinit var saveButton: Button
    private lateinit var memberSpinner: Spinner
    private lateinit var chipsContainer: LinearLayout
    private lateinit var dayButtons: List<Button>
    private val userList = mutableListOf<UserData>()
    private var date: String = ""

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

        // Days buttons
        val daysContainer = view.findViewById<GridLayout>(R.id.daysContainer)
        dayButtons = (0 until daysContainer.childCount).map { daysContainer.getChildAt(it) as Button }

        // Spinner data
        val members = resources.getStringArray(R.array.members_list)
        memberSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, members)

        // Handle day selection
        dayButtons.forEach { button ->
            Log.d("Dias", button.text.toString())

            button.setOnClickListener {
                dayButtons.forEach { other ->
                    other.setBackgroundResource(R.drawable.button_outline)
                    other.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    other.tag = "unselected"
                }

                button.setBackgroundResource(R.drawable.button_enabled)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                button.tag = "selected"
                date = button.text.toString()


//                else {
//                    button.setBackgroundResource(R.drawable.button_enabled)
//                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//                    button.tag = "selected"
//                }
                checkFormState()
            }
        }

        // Simple chips simulation
        memberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val name = parent.getItemAtPosition(position).toString()
                if (name.isNotBlank()) addChip(name)
                checkFormState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Validation listeners
        taskName.setOnFocusChangeListener { _, _ -> checkFormState() }
        taskDesc.setOnFocusChangeListener { _, _ -> checkFormState() }

        saveButton.setOnClickListener{

            checkFormState()

            var intent = Intent(requireContext(), TasksActivity::class.java)
            intent.putExtra("newTask", createTask())
            startActivity(intent)

        }

        return view
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

    fun createTask(): Task{

        return Task(taskName.text.toString(), taskDesc.text.toString(), userList, date(date), "Pendiente")

    }
}
