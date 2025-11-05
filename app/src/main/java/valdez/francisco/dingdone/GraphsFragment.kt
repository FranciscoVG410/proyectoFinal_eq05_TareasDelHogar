package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView

class GraphsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graphs, container, false)

        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        val entries = listOf(
            PieEntry(40f, "Amosingazzz"),
            PieEntry(30f, "CompaVic"),
            PieEntry(20f, "PaquitoChicaEmo")
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 25f

        val data = PieData(dataSet)
        pieChart.data = data

        pieChart.description.isEnabled = false
        pieChart.centerText = "Completed Tasks"
        pieChart.animateY(1400)

        val legend = pieChart.legend
        legend.textSize = 20f
        legend.formSize = 14f
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 8f

        return view
    }
}