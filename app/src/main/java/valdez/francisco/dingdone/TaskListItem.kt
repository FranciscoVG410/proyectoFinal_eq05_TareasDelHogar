package valdez.francisco.dingdone

sealed class TaskListItem {

    data class Header(val dia: String) : TaskListItem()
//    data class TaskItem(val task: Task, val homeId: String = "") : TaskListItem()
    data class HomeHeader(val homeName: String) : TaskListItem()
    data class TaskItem(val task: Task, val homeId: String, val dateForFilter: String): TaskListItem()

}