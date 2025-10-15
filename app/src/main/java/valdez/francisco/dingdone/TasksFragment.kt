package valdez.francisco.dingdone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Task(val day: String, val title: String, val description: String, val status: String)

class TasksFragment : Fragment() {

    private lateinit var taskList: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_tasks, container, false)

        taskList = view.findViewById(R.id.recyclerViewTasks)
        emptyText = view.findViewById(R.id.emptyText)

        val dummyTasks = listOf(
            Task("Monday", "Lavar los platos", "Lavar todos los platos que se usaron en la mañana", "Completado"),
            Task("Monday", "Sacar la basura", "Sacar la basura antes de las 10 porque llega el camión", "Pendiente"),
            Task("Tuesday", "Lavar los carros", "Lavar el Eclipse del Beto porque se enoja si no", "Pendiente"),
            Task("Tuesday", "Lavar los correos", "Lavar correos inventados para probar longitud", "Completado")
        )

        if (dummyTasks.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            taskList.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            taskList.visibility = View.VISIBLE
            taskList.layoutManager = LinearLayoutManager(requireContext())
            taskList.adapter = TaskAdapter(dummyTasks, this.requireContext())
        }

        return view
    }
}

class TaskAdapter(private val tasks: List<Task>, private val context: Context) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val day: TextView = view.findViewById(R.id.taskDay)
        val title: TextView = view.findViewById(R.id.textViewTitle)
        val desc: TextView = view.findViewById(R.id.textViewDescription)
        val status: TextView = view.findViewById(R.id.taskStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val t = tasks[position]
        holder.day.text = t.day
        holder.title.text = t.title
        holder.desc.text = t.description
        holder.status.text = t.status
        holder.itemView.setOnClickListener {

            context.startActivity(Intent(context, HomeConfiguration::class.java))

        }
    }

    override fun getItemCount() = tasks.size
}
