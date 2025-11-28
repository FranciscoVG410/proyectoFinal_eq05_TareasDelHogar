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
import valdez.francisco.dingdone.graphics.CustomPieDrawable
import valdez.francisco.dingdone.graphics.PieSlice

class GraphsFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val homeShareViewModel: HomeShareViewModel by activityViewModels()

    private lateinit var customPieView: View
    private lateinit var spinnerPeriod: Spinner
    private lateinit var rgDataType: RadioGroup

    private var currentPeriod: PeriodType = PeriodType.WEEKLY
    private var currentDataType: GraphDataType = GraphDataType.COMPLETED

    private val userColorMap = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graphs, container, false)

        customPieView = view.findViewById(R.id.customPieChart)
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
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerPeriod.adapter = adapter
        spinnerPeriod.setSelection(periodOptions.indexOf(PeriodType.WEEKLY.name))

        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPeriod = PeriodType.valueOf(periodOptions[position])
                Log.d("GraphsFragment", "Period Selected: $currentPeriod")
                if (currentDataType == GraphDataType.COMPLETED) {
                    loadDataForCurrentHome()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDataTypeRadioGroup() {
        rgDataType.setOnCheckedChangeListener { _, checkedId ->
            currentDataType = when (checkedId) {
                R.id.rbCompleted -> GraphDataType.COMPLETED
                R.id.rbUnfinished -> GraphDataType.UNFINISHED
                else -> return@setOnCheckedChangeListener
            }
            loadDataForCurrentHome()
        }
        rgDataType.check(R.id.rbCompleted)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            if (!homeId.isNullOrEmpty()) {
                Log.d("GraphsFragment", "Loading tasks: $homeId")
                loadDataForCurrentHome()
            } else {
                Log.w("GraphsFragment", "Home ID invalid")
                customPieView.background = null
            }
        }
    }

    private fun loadDataForCurrentHome() {
        val homeId = homeShareViewModel.selectedHomeId.value ?: return

        when (currentDataType) {
            GraphDataType.COMPLETED -> {
                userViewModel.loadCompletedTasksForHome(homeId)
            }
            GraphDataType.UNFINISHED -> {
                userViewModel.loadUnfinishedTasksForHome(homeId)
            }
        }
    }

    private fun updatePieChartData(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            customPieView.background = null
            return
        }

        val allMembers = tasks.flatMap { it.member }
        val taskCounts = allMembers.groupBy { it }.mapValues { it.value.size }
        val total = taskCounts.values.sum().toFloat()

        taskCounts.keys.forEach { userId ->
            if (!userColorMap.containsKey(userId)) {
                userColorMap[userId] = getRandomColor()
            }
        }

        val slices = taskCounts.map { (userId, count) ->
            PieSlice(
                label = resolveUserName(userId),
                angle = (count / total) * 360f,
                color = userColorMap[userId]!!,
                count = count
            )
        }
        val isCompletedFlag = currentDataType == GraphDataType.COMPLETED
        customPieView.background = CustomPieDrawable(requireContext(), slices, isCompletedFlag)
    }

    private fun getRandomColor(): Int {
        val rnd = java.util.Random()
        val r = rnd.nextInt(256)
        val g = rnd.nextInt(256)
        val b = rnd.nextInt(256)
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun resolveUserName(userId: String): String {
        return userId
    }
}
