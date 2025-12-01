package valdez.francisco.dingdone

sealed class TaskListItemProfile {
    data class HeaderProfile(val dia: String) : TaskListItemProfile()
//    data class TaskItemProfile(val task: Task, val homeId: String = "") : TaskListItemProfile()
    data class HomeHeaderProfile(val homeName: String) : TaskListItemProfile()
    data class TaskItemProfile(val task: Task, val homeId: String, val dateForFilter: String): TaskListItemProfile()


}