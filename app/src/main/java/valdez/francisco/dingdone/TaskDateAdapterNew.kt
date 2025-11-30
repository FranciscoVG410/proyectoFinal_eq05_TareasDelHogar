package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDateAdapterNew(
    private var items: List<TaskListItem>,
    private val homeId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
//    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun updateItem(newItem: List<TaskListItem>){
        items = newItem
        notifyDataSetChanged()
    }

    companion object{
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
        private const val TYPE_HOME_HEADER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TaskListItem.Header -> TYPE_HEADER
            is TaskListItem.TaskItem -> TYPE_TASK
            is TaskListItem.HomeHeader -> TYPE_HOME_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_HOME_HEADER -> {
                val view = inflater.inflate(R.layout.item_home_header, parent, false)
                HomeHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
        }
    }

    class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val headerText: TextView = view.findViewById(R.id.headerText)
        fun bind(item: TaskListItem.Header){
            headerText.text = item.dia
        }
    }

    class HomeHeaderViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val homeHeaderText: TextView = view.findViewById(R.id.homeHeaderText)
        fun bind(item: TaskListItem.HomeHeader){
            homeHeaderText.text = item.homeName
        }
    }

    inner class TaskViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val nombre: TextView = view.findViewById(R.id.tvTitulo)
        private val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
        private val status: TextView = view.findViewById(R.id.tvTaskStatus)
        private val chgMembers: ChipGroup = view.findViewById(R.id.chgMembersTast)

        fun bind(item: TaskListItem.TaskItem){
            nombre.text = item.task.nombre
            descripcion.text = item.task.descripcio
            status.text = "Cargando..."
            status.setBackgroundResource(R.drawable.item_pending)

            val taskHomeId = if(item.homeId.isNotEmpty()) item.homeId else homeId

            val historyDateId = item.dateForFilter

            db.collection("homes").document(taskHomeId)
                .collection("tasks").document(item.task.id)
                .collection("history").document(historyDateId)
                .get()
                .addOnSuccessListener { document ->

                    if (document.exists() && document.getString("status") == "Completada") {
                        status.text = "Completada"
                        status.setBackgroundResource(R.drawable.item_completed)
                    } else {
                        status.text = "Pendiente"
                        status.setBackgroundResource(R.drawable.item_pending)
                    }

                }.addOnFailureListener{

                    status.text = "Pendiente"
                    status.setBackgroundResource(R.drawable.item_pending)

                }

            chgMembers.removeAllViews()

            item.task.member.forEach{ member ->
                val chipContext = ContextThemeWrapper(chgMembers.context, com.google.android.material.R.style.Theme_MaterialComponents_Light)
                val chip = Chip(chipContext).apply {
                    text = member
                    isClickable = false
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.btnBackground)
                    setTextColor(ContextCompat.getColor(chgMembers.context, R.color.white))
                    isCloseIconVisible = false
                }
                chgMembers.addView(chip)
            }

            itemView.setOnClickListener{
                val context = itemView.context
                if (context is FragmentActivity) {

                    val taskHomeId = if (item.homeId.isNotEmpty()) item.homeId else homeId
                    val fragment = TaskDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString("taskId", item.task.id)
                            putString("homeId", taskHomeId)
                            putString("nombre", item.task.nombre)
                            putString("descripcion", item.task.descripcio)
                            putString("estado", status.text.toString())
                            putStringArrayList("miembros", ArrayList(item.task.member))
                            putString("selectedDate", item.dateForFilter)
                        }
                    }
                    
                    context.supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragment_container, fragment)
                        addToBackStack(null)
                        commit()
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item)
            is TaskListItem.HomeHeader -> (holder as HomeHeaderViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

