package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TasksFragmentNew : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskDateAdapterNew
    private val allTasks = mutableListOf<Task>()
    private lateinit var buttonsContainer: LinearLayout
    private val tasks1 = mutableListOf<Task>()
    private val tasks2 = mutableListOf<Task>()
    private val tasks3 = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks_new, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        val task = arguments?.getParcelable<Task>("newTask")
        task?.let {
            tasks1.add(it)
        }

        if (tasks1.isEmpty() || tasks2.isEmpty() || tasks3.isEmpty()) {
            tasks1.add(Task("Lavar platos", "Lavar todos los platos que se usaron en la mañana", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Completada"))
            tasks1.add(Task("Sacar la basura", "Sacar la basura antes de las 10 porque llega el camión", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Martes", "Completada"))
            tasks1.add(Task("Lavar los carros", "Lavar el Eclipse del Beto porque se enoja si no", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Viernes", "Pendiente"))
            tasks1.add(Task("Revisar el correo", "Lavar correos inventados para probar longitud", listOf(UserData("Juan"), UserData("Amos")), "Viernes", "Pendiente"))

            tasks2.add(Task("Lavar platos", "Lavar todos los platos que se usaron en la mañana", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Completada"))
            tasks2.add(Task("Sacar la basura", "Sacar la basura antes de las 10 porque llega el camión", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Completada"))
            tasks2.add(Task("Lavar los carros", "Lavar el Eclipse del Beto porque se enoja si no", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Pendiente"))
            tasks2.add(Task("Revisar el correo", "Lavar correos inventados para probar longitud", listOf(UserData("Juan"), UserData("Amos")), "Lunes", "Pendiente"))

            tasks3.add(Task("Lavar platos", "Lavar todos los platos que se usaron en la mañana", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Completada"))
            tasks3.add(Task("Sacar la basura", "Sacar la basura antes de las 10 porque llega el camión", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Completada"))
            tasks3.add(Task("Lavar los carros", "Lavar el Eclipse del Beto porque se enoja si no", listOf(UserData("Juan"), UserData("Francisco"), UserData("Victor")), "Lunes", "Pendiente"))
            tasks3.add(Task("Revisar el correo", "Lavar correos inventados para probar longitud", listOf(UserData("Juan"), UserData("Amos")), "Lunes", "Pendiente"))
        }

        val homeButtonTitles = listOf("home1", "home2", "home3")
        val taskLists = listOf(tasks1, tasks2, tasks3)

        buttonsContainer = view.findViewById(R.id.buttonsContainer)

        val marginInPixels = (16 * resources.displayMetrics.density).toInt()

        for (i in homeButtonTitles.indices) {
            val button = Button(requireContext())
            button.text = homeButtonTitles[i]
            button.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button_purple)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.rightMargin = marginInPixels
            button.layoutParams = params

            button.setOnClickListener {
                updateTasks(taskLists[i])
                updateButtonStyles(button)
            }
            buttonsContainer.addView(button)
        }

        taskAdapter = TaskDateAdapterNew(emptyList())
        recyclerView.adapter = taskAdapter

        updateTasks(taskLists[0])
        if (buttonsContainer.childCount > 0) {
            updateButtonStyles(buttonsContainer.getChildAt(0) as Button)
        }

        val fabAddTask: FloatingActionButton = view.findViewById(R.id.fabAddTask)
        val fragmentContainer = view.findViewById<android.view.View>(R.id.fragment_container)
        
        fabAddTask.setOnClickListener {
            val fragment = NewTaskFragment()
            
            fragmentContainer.visibility = android.view.View.VISIBLE
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null)
                commit()
            }
        }
        
        // Hide fragment container when back stack changes
        parentFragmentManager.addOnBackStackChangedListener {
            if (parentFragmentManager.backStackEntryCount == 0) {
                fragmentContainer.visibility = android.view.View.GONE
            }
        }

        return view
    }

    private fun updateButtonStyles(selectedButton: Button) {
        for (i in 0 until buttonsContainer.childCount) {
            val button = buttonsContainer.getChildAt(i) as Button
            if (button == selectedButton) {
                button.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button_purple_selected)
            } else {
                button.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button_purple)
            }
        }
    }

    private fun updateTasks(tasks: List<Task>) {
        allTasks.clear()
        allTasks.addAll(tasks)
        val groupedTasks: Map<String, List<Task>> = allTasks.groupBy { it.date }

        val items = mutableListOf<TaskListItem>()
        val weekDays = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo")

        for (day in weekDays) {
            val dayTasks = groupedTasks[day]
            if (!dayTasks.isNullOrEmpty()) {
                items.add(TaskListItem.Header(day))
                items.addAll(dayTasks.map { TaskListItem.TaskItem(it) })
            }
        }
        taskAdapter.updateItem(items)
    }
}

