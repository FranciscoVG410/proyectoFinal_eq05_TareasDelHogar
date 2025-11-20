package valdez.francisco.dingdone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
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
    private lateinit var spinnerPeriod: Spinner
    private lateinit var rgDataType: RadioGroup

    private var currentPeriod: PeriodType = PeriodType.WEEKLY
    private var currentDataType: GraphDataType = GraphDataType.COMPLETED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graphs, container, false)
        pieChart = view.findViewById(R.id.pieChart)

        spinnerPeriod = view.findViewById(R.id.spinnerPeriod)
        rgDataType = view.findViewById(R.id.rgDataType)

        setupPeriodSpinner()

        setupDataTypeRadioGroup()

        return view
    }

    private fun setupPeriodSpinner() {

        val periodOptions = PeriodType.entries.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPeriod.adapter = adapter

        val initialPosition = periodOptions.indexOf(PeriodType.WEEKLY.name)
        spinnerPeriod.setSelection(initialPosition)

        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val selectedName = periodOptions[position]
                currentPeriod = PeriodType.valueOf(selectedName)
                Log.d("GraphsFragment", "Period Selected: $currentPeriod")

                if (currentDataType == GraphDataType.COMPLETED) {
                    loadDataForCurrentHome()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun setupDataTypeRadioGroup() {
        rgDataType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCompleted -> currentDataType = GraphDataType.COMPLETED
                R.id.rbUnfinished -> currentDataType = GraphDataType.UNFINISHED
                else -> return@setOnCheckedChangeListener
            }
            Log.d("GraphsFragment", "Selected DataType: $currentDataType")

            loadDataForCurrentHome()
        }

        rgDataType.check(R.id.rbCompleted)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()

        setupSelectedHomeObserver()

        userViewModel.completedTasksData.observe(viewLifecycleOwner) { completedTasks ->
            if (currentDataType == GraphDataType.COMPLETED) {
                updatePieChartData(completedTasks)
            }
        }

        userViewModel.unfinishedTasksData.observe(viewLifecycleOwner) { unfinishedTasks ->
            if (currentDataType == GraphDataType.UNFINISHED) {
                updatePieChartData(unfinishedTasks)
            }
        }
    }

    private fun setupSelectedHomeObserver() {
        homeShareViewModel.selectedHomeId.observe(viewLifecycleOwner) { homeId ->
            if (homeId != null && homeId.isNotEmpty()) {
                Log.d("GraphsFragment", "Loading tasks: $homeId")

                loadDataForCurrentHome()
            } else {
                Log.w("GraphsFragment", "Home ID invalid")
                pieChart.setNoDataText("Select a home to see the tasks")
                pieChart.data = null
                pieChart.invalidate()
            }
        }
    }

    private fun loadDataForCurrentHome() {
        val homeId = homeShareViewModel.selectedHomeId.value
        if (homeId != null && homeId.isNotEmpty()) {
            when (currentDataType) {
                GraphDataType.COMPLETED -> {
                    userViewModel.loadCompletedTasksForHome(homeId, currentPeriod)
                    pieChart.centerText = "Completed Tasks (${currentPeriod.name})"
                }
                GraphDataType.UNFINISHED -> {

                    userViewModel.loadUnfinishedTasksForHome(homeId)
                    pieChart.centerText = "Uncompleted Tasks"

                }
            }
        }
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.centerText = "Completed Tasks"
        pieChart.animateY(1400)

        val legend = pieChart.legend

        legend.orientation = Legend.LegendOrientation.VERTICAL

        legend.textSize = 20f
        legend.formSize = 14f
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 8f
    }

    private fun updatePieChartData(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            pieChart.setNoDataText("There's no completed Tasks in this home.")
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