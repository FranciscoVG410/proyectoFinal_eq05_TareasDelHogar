package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.collection.mutableLongListOf
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
    private lateinit var noTasksText: TextView
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
        taskAdapter = TaskDateAdapterNew(emptyList(), "")
        recyclerView.adapter = taskAdapter


        val userId = auth.currentUser?.uid
        if (userId != null) {
            uvm.loadUserHomes(userId)
        }

        uvm.userHomes.observe(viewLifecycleOwner) { homes ->

            buttonsContainer.removeAllViews()

            if(homes.size > 1){
                buttonsContainer.visibility = View.VISIBLE
                homes.forEach { home ->

                    val displayName = if (home.name.length > 20) {
                        val start = home.name.substring(0, 8)
                        val end = home.name.substring(home.name.length - 8)
                        "$start...$end"
                    } else {
                        home.name
                    }

                    val button = Button(requireContext()).apply {
                        text = displayName
                        tag = home.name
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(12, 0, 12, 0)
                        }
                        minWidth = 150
                        maxWidth = 280
                        setPadding(24, 12, 24, 12)
                        background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.rounded_button_purple
                        )
                    }

                    button.setOnClickListener {
                        homeViewModel.selectHome(home.id)
                        uvm.loadTasksForHome(home.id)
                        updateButtonStyles(button)
                    }

                    buttonsContainer.addView(button)
                }

                if (homes.isNotEmpty()) {
                    val firstHome = homes.first()
                    homeViewModel.selectHome(firstHome.id)
                    uvm.loadTasksForHome(firstHome.id)

                    if (buttonsContainer.childCount > 0) {
                        val firstButton = buttonsContainer.getChildAt(0) as Button
                        updateButtonStyles(firstButton)
                    }
                }
            }else if(homes.size == 1) {

                buttonsContainer.visibility = View.VISIBLE
                val uniqueHome = homes.first()

                val titleView = TextView(requireContext()).apply {
                    text = uniqueHome.name
                    textSize = 36f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = android.view.Gravity.CENTER
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                buttonsContainer.addView(titleView)

                homeViewModel.selectHome(uniqueHome.id)
                uvm.loadTasksForHome(uniqueHome.id)

            }else if(homes.size == 0){

                mostrarText()

            }
        }

        uvm.homeTasks.observe(viewLifecycleOwner) { map ->
            val selectedHomeId = homeViewModel.selectedHomeId.value

            if (selectedHomeId != null) {
                val tasks = map[selectedHomeId] ?: emptyList()
                updateTasks(tasks, selectedHomeId)
            }
        }

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

    private fun mostrarText(){

        noTasksText.visibility = View.VISIBLE
        noTasksText.text = "No current homes..."
        recyclerView.visibility = View.GONE

    }

    private fun updateButtonStyles(selectedButton: Button) {
        for (i in 0 until buttonsContainer.childCount) {
            val button = buttonsContainer.getChildAt(i) as Button
            val fullName = button.tag as? String ?: button.text.toString()
            
            if (button == selectedButton) {
                button.text = fullName
                button.maxWidth = 600
                button.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.fondobtn
                )
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                val displayName = if (fullName.length > 20) {
                    val start = fullName.substring(0, 8)
                    val end = fullName.substring(fullName.length - 8)
                    "$start...$end"
                } else {
                    fullName
                }
                button.text = displayName
                button.maxWidth = 280
                button.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.contorno
                )
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
            }
        }
    }

    private fun updateTasks(tasks: List<Task>, homeId: String) {

        allTasks.clear()
        allTasks.addAll(tasks)

        if(recyclerView.adapter == null || (recyclerView.adapter as? TaskDateAdapterNew) == null){

            taskAdapter == TaskDateAdapterNew(emptyList(), homeId)
            recyclerView.adapter = taskAdapter

        }

        taskAdapter = TaskDateAdapterNew(emptyList(), homeId)
        recyclerView.adapter = taskAdapter

        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        val todayString = when (dayOfWeek){

            java.util.Calendar.MONDAY -> "Lunes"
            java.util.Calendar.TUESDAY -> "Martes"
            java.util.Calendar.WEDNESDAY -> "Miercoles"
            java.util.Calendar.THURSDAY -> "Jueves"
            java.util.Calendar.FRIDAY -> "Viernes"
            java.util.Calendar.SUNDAY -> "Sabado"
            java.util.Calendar.SATURDAY-> "Domingo"
            else -> ""

        }

        val pendingTaskToday = allTasks.filter { task ->

            task.state != "Completada" && task.date.contains(todayString)

        }

        val completedTask = allTasks.filter { task ->

            task.state == "Completada" && task.date.contains(todayString)

        }

        if(pendingTaskToday.isEmpty() && completedTask.isEmpty()){

            noTasksText.visibility = View.VISIBLE
            noTasksText.text = "not task for today (${todayString})"
            recyclerView.visibility = View.GONE
            return

        }else {

            noTasksText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

        }

        val items = mutableListOf<TaskListItem>()

        if(pendingTaskToday.isNotEmpty()){

            items.add(TaskListItem.Header("Para hoy (${todayString})"))
            items.addAll(pendingTaskToday.map { TaskListItem.TaskItem(it) })

        }else {

            items.add(TaskListItem.Header("Para hoy (${todayString})"))

        }

        if(completedTask.isNotEmpty()){

            items.add(TaskListItem.Header("Completadas"))
            items.addAll(completedTask.map { TaskListItem.TaskItem(it) })

        }

//        if (allTasks.isEmpty()) {
//
//            noTasksText.visibility = View.VISIBLE
//            noTasksText.text = "No current tasks..."
//            recyclerView.visibility = View.GONE
//            return
//        } else {
//            noTasksText.visibility = View.GONE
//            recyclerView.visibility = View.VISIBLE
//        }
//
//        val pendingTasks = allTasks.filter { it.state != "Completada" }
//        val completedTasks = allTasks.filter { it.state == "Completada" }
//
//        val groupedPendingTasks = mutableMapOf<String, MutableList<Task>>()
//
//        for (task in pendingTasks) {
//            for (day in task.date) {
//                groupedPendingTasks.getOrPut(day) { mutableListOf() }.add(task)
//            }
//        }
//
//        val items = mutableListOf<TaskListItem>()
//
//        val weekDays = listOf(
//            "Lunes", "Martes", "Miercoles", "Jueves",
//            "Viernes", "Sabado", "Domingo"
//        )
//
//        for (day in weekDays) {
//            val dayTasks = groupedPendingTasks[day]
//
//            if (!dayTasks.isNullOrEmpty()) {
//                items.add(TaskListItem.Header(day))
//                items.addAll(dayTasks.map { TaskListItem.TaskItem(it) })
//            }
//        }
//
//        if (completedTasks.isNotEmpty()) {
//            items.add(TaskListItem.Header("Completadas"))
//            items.addAll(completedTasks.map { TaskListItem.TaskItem(it) })
//        }

        taskAdapter.updateItem(items)
    }


}

