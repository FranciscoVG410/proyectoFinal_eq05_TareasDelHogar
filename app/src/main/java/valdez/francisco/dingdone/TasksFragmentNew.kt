package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.fragment.app.activityViewModels

class TasksFragmentNew : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskDateAdapterNew
    private val allTasks = mutableListOf<Task>()
    private lateinit var buttonsContainer: LinearLayout

    private val homeViewModel: HomeShareViewModel by activityViewModels()
    private val uvm: UserViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var noTasksText: View
    private lateinit var layoutFail: View
    private lateinit var textFail: TextView
    private lateinit var toast: Toast


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_tasks_new, container, false)

        val inflate = layoutInflater
        layoutFail = inflate.inflate(R.layout.custome_toast_fail, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        toast = Toast(context)

        noTasksText = view.findViewById(R.id.tvNoTasks)
        recyclerView = view.findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        buttonsContainer = view.findViewById(R.id.buttonsContainer)
        taskAdapter = TaskDateAdapterNew(emptyList())
        recyclerView.adapter = taskAdapter


        val userId = auth.currentUser?.uid
        if (userId != null) {
            uvm.loadUserHomes(userId)
        }



        // OBSERVAR LAS HOMES DEL USUARIO
        uvm.userHomes.observe(viewLifecycleOwner) { homes ->

            buttonsContainer.removeAllViews()  // limpiamos botones anteriores

            homes.forEach { home ->

                val button = Button(requireContext()).apply {
                    text = home.name
                    layoutParams = LinearLayout.LayoutParams(
                        190,  // ancho fijo
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(12, 0, 12, 0)
                    }
                    background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.rounded_button_purple
                    )
                }

                // Evento al pulsar
                button.setOnClickListener {
                    homeViewModel.selectHome(home.id)
                    uvm.loadTasksForHome(home.id)
                    updateButtonStyles(button)
                }

                buttonsContainer.addView(button)
            }

            // Seleccionar automáticamente la primera home
            if (homes.isNotEmpty()) {
                val firstHome = homes.first()
                homeViewModel.selectHome(firstHome.id)
                uvm.loadTasksForHome(firstHome.id)

                if (buttonsContainer.childCount > 0) {
                    val firstButton = buttonsContainer.getChildAt(0) as Button
                    updateButtonStyles(firstButton)
                }
            }
        }

        // OBSERVAR LAS TAREAS
        uvm.homeTasks.observe(viewLifecycleOwner) { map ->
            val selectedHomeId = homeViewModel.selectedHomeId.value

            if (selectedHomeId != null) {
                val tasks = map[selectedHomeId] ?: emptyList()
                updateTasks(tasks)
            }
        }



        // FAB – Crear tarea
        val fabAddTask: FloatingActionButton = view.findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {

            if(uvm.userHomes.value?.isEmpty() == true){

                textFail.text = "Join or Create a Home"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()

            }else{

                val fragment = NewTaskFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()

            }


        }

        return view
    }

    private fun updateButtonStyles(selectedButton: Button) {
        for (i in 0 until buttonsContainer.childCount) {
            val button = buttonsContainer.getChildAt(i) as Button
            if (button == selectedButton) {
                button.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.fondobtn
                )
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                button.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.contorno
                )
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
            }
        }
    }

    private fun updateTasks(tasks: List<Task>) {

        allTasks.clear()
        allTasks.addAll(tasks)

        // Mostrar mensaje si no hay tareas
        if (allTasks.isEmpty()) {
            noTasksText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        } else {
            noTasksText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        // Agrupar por día
        val groupedTasks = mutableMapOf<String, MutableList<Task>>()

        for (task in allTasks) {
            for (day in task.date) {
                groupedTasks.getOrPut(day) { mutableListOf() }.add(task)
            }
        }

        val items = mutableListOf<TaskListItem>()

        val weekDays = listOf(
            "Lunes", "Martes", "Miercoles", "Jueves",
            "Viernes", "Sabado", "Domingo"
        )

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

