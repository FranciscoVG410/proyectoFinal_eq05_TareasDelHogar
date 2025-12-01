package valdez.francisco.dingdone

data class UserProgres(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val progressPercentage: Int = 0,
    val allTasks: List<Task> = emptyList()
)