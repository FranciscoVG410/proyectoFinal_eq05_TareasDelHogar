package valdez.francisco.dingdone

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class TaskDateAdapter(private var items: List<TaskListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateItem(newItem: List<TaskListItem>){

        items = newItem
        notifyDataSetChanged()

    }



    companion object{

        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1

    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TaskListItem.Header -> TYPE_HEADER
            is TaskListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view){

        private val headerText: TextView = view.findViewById(R.id.headerText)
        fun bind(item: TaskListItem.Header){

            headerText.text = item.dia

        }

    }

    class TaskViewHolder(view: View): RecyclerView.ViewHolder(view){

        private val nombre: TextView = view.findViewById(R.id.tvTitulo)
        private val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
        private val status: TextView = view.findViewById(R.id.tvTaskStatus)
        private val chgMembers: ChipGroup = view.findViewById(R.id.chgMembersTast)

        fun bind(item: TaskListItem.TaskItem){

            nombre.text = item.task.nombre
            descripcion.text = item.task.descripcio
            status.text = item.task.state
            if(item.task.state == "Completada"){

                status.setBackgroundResource(R.drawable.item_completed)

            }else if (item.task.state == "Penidente"){

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
//                val intent = Intent(context, TaskDetail::class.java).apply {
//
//                    putExtra("nombre", item.task.nombre)
//                    putExtra("descripcion", item.task.descripcio)
//                    putExtra("estado", item.task.state)
//                    putStringArrayListExtra("miembros", ArrayList(item.task.member.map { it.nombre }))
//
//                }

//                context.startActivity(intent)

            }

        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item)
        }

    }

    override fun getItemCount(): Int = items.size

}

