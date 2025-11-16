package valdez.francisco.dingdone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class GraphsFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private val homeShareViewModel: HomeShareViewModel by activityViewModels()

    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graphs, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()

        setupSelectedHomeObserver()

        userViewModel.completedTasksData.observe(viewLifecycleOwner) { completedTasks ->
            updatePieChartData(completedTasks)
        }
    }

    private fun setupSelectedHomeObserver() {
        homeShareViewModel.selectedHomeId.observe(viewLifecycleOwner) { homeId ->
            if (homeId != null && homeId.isNotEmpty()) {
                Log.d("GraphsFragment", "Cargando tareas para Home ID: $homeId")

                userViewModel.loadCompletedTasksForHome(homeId)
            } else {
                Log.w("GraphsFragment", "Home ID no seleccionado o inválido.")
                pieChart.setNoDataText("Selecciona un Home para ver los datos.")
                pieChart.data = null
                pieChart.invalidate()
            }
        }
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.centerText = "Tareas Completadas"
        pieChart.animateY(1400)

        val legend = pieChart.legend

        // ahora los nombres estaran ordenados verticalmente
        legend.orientation = Legend.LegendOrientation.VERTICAL

        legend.textSize = 20f
        legend.formSize = 14f
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 8f
    }

    private fun updatePieChartData(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            pieChart.setNoDataText("No hay tareas completadas en este Home.")
            pieChart.data = null
            pieChart.invalidate()
            return
        }

        val allCompletedMembers = tasks.flatMap { it.member }

        val taskCounts = allCompletedMembers
            .groupBy { it }
            .mapValues { it.value.size.toFloat() }

        val entries = taskCounts.map { (memberId, count) ->
            PieEntry(count, resolveUserName(memberId))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 25f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun resolveUserName(userId: String): String {
        return userId
    }
}