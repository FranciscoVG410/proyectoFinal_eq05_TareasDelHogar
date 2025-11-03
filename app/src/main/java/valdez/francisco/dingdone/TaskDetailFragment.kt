package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class TaskDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_task_detail, container, false)

        val nombre = arguments?.getString("nombre")
        val descripcion = arguments?.getString("descripcion")
        val estado = arguments?.getString("estado")
        val miembros = arguments?.getStringArrayList("miembros") ?: arrayListOf()

        val tvNombre: TextView = view.findViewById(R.id.tvTituloDetail)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionDetal)
        val btnChangeState: Button = view.findViewById(R.id.btnChangeState)
        val chgMiembros: ChipGroup = view.findViewById(R.id.chgMembersTaskDetal)
        val btnReturn: Button = view.findViewById(R.id.btnBackHome)

        btnChangeState.text = estado
        if (btnChangeState.text == "Completada") {
            btnChangeState.setBackgroundResource(R.drawable.item_completed)
        } else if (btnChangeState.text == "Pendiente") {
            btnChangeState.setBackgroundResource(R.drawable.item_pending)
        }
        
        tvNombre.text = nombre
        tvDescripcion.text = descripcion
        
        btnChangeState.setOnClickListener {
            if (btnChangeState.text == "Completada") {
                btnChangeState.text = "Pendiente"
                btnChangeState.setBackgroundResource(R.drawable.item_pending)
            } else if (btnChangeState.text == "Pendiente") {
                btnChangeState.text = "Completada"
                btnChangeState.setBackgroundResource(R.drawable.item_completed)
            }
        }

        btnReturn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        chgMiembros.removeAllViews()

        miembros.forEach { member: String ->
            val chipContext = ContextThemeWrapper(chgMiembros.context, com.google.android.material.R.style.Theme_MaterialComponents_Light)
            val chip = Chip(chipContext).apply {
                text = member
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.btnBackground)
                setTextColor(ContextCompat.getColor(chgMiembros.context, R.color.white))
                isCloseIconVisible = true
            }
            chgMiembros.addView(chip)
        }

        return view
    }
}

