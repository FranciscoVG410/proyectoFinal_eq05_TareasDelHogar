package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val allTasks = mutableListOf<Task>()
    private lateinit var buttonsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create dummy tasks
        val tasks1 = mutableListOf(
            Task("Monday", "to buy", "Milk, bread, and eggs","pending"),
            Task("Monday", "work","Complete the quarterly report for work","pending")
        )

        val tasks2 = mutableListOf(
            Task("Tuesday", "groceries","Buy groceries", "pending"),
            Task("Tuesday", "birthday","go buy a present for my birthday","pending")
        )

        val tasks3 = mutableListOf(
            Task("Wednesday", "gym", "Go to the gym", "pending")
        )

        val homeButtonTitles = listOf("home1", "home2", "home3")
        val taskLists = listOf(tasks1, tasks2, tasks3)

        buttonsContainer = findViewById(R.id.buttonsContainer)

        val marginInPixels = (16 * resources.displayMetrics.density).toInt()

        for (i in homeButtonTitles.indices) {
            val button = Button(this)
            button.text = homeButtonTitles[i]
            button.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_purple)
            button.setTextColor(ContextCompat.getColor(this, R.color.white))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.rightMargin = marginInPixels
            button.layoutParams = params

            button.setOnClickListener {
                updateTasks(taskLists[i])
            }
            buttonsContainer.addView(button)
        }

        taskAdapter = TaskAdapter(allTasks, this)
        recyclerView.adapter = taskAdapter

        // Show the first list of tasks by default
        updateTasks(taskLists[0])

        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val fragment = NewTaskFragment()

            supportFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null)
                commit()
            }
        }
    }

    private fun updateTasks(tasks: List<Task>) {
        allTasks.clear()
        allTasks.addAll(tasks)
        taskAdapter.notifyDataSetChanged()
    }
}