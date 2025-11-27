package valdez.francisco.dingdone

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _userHomes = MutableLiveData<List<Home>>()
    val userHomes: LiveData<List<Home>> get() = _userHomes

    private val _homeTasks = MutableLiveData<Map<String, List<Task>>>()
    val homeTasks: LiveData<Map<String, List<Task>>> get() = _homeTasks

    private val _homesWithTasks = MutableLiveData<List<HomeWithTasks>>()
    val homesWithTasks: LiveData<List<HomeWithTasks>> get() = _homesWithTasks

    private val _completedTasksData = MutableLiveData<List<Task>>()
    val completedTasksData: LiveData<List<Task>> get() = _completedTasksData

    private val _unfinishedTasksData = MutableLiveData<List<Task>>()
    val unfinishedTasksData: LiveData<List<Task>> get() = _unfinishedTasksData

    private val _userTaskProgress = MutableLiveData<UserProgres>()
    val userTaskProgress: LiveData<UserProgres> get() = _userTaskProgress

    fun loadUserHomes(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if(document.exists()){

                    val homeIds = document.get("homes") as? List<String> ?: emptyList()
                    if (homeIds.isEmpty()) {
                        _userHomes.postValue(emptyList())
                        return@addOnSuccessListener
                    }

                    db.collection("homes")
                        .whereIn(FieldPath.documentId(), homeIds)
                        .get()
                        .addOnSuccessListener { homesSnap ->
                            val homes = homesSnap.toObjects(Home::class.java)
                            _userHomes.postValue(homes)
                        }
                        .addOnFailureListener { e ->
                            Log.w("loadUserHomes", "Error fetching homes: $e")
                        }

                }else{

                    Log.w("loadUserHomes", "User document does not exists")
                    _userHomes.postValue(emptyList())

                }
            }
            .addOnFailureListener { e ->
                Log.w("loadUserHomes", "Error fetching user_home: $e")
            }
    }


    fun loadTasksForHome(homeId: String) {
        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.toObjects(Task::class.java)

                val map = _homeTasks.value?.toMutableMap() ?: mutableMapOf()
                map[homeId] = tasks

                _homeTasks.value = map
            }
    }


    data class HomeWithTasks(
        val home: Home,
        val tasks: List<Task>
    )

    fun loadHomesAndTasks(userId: String) {
        loadUserHomes(userId)

        userHomes.observeForever { homes ->
            val resultList = mutableListOf<HomeWithTasks>()

            if (homes.isEmpty()) {
                _homesWithTasks.value = emptyList()
                return@observeForever
            }

            for (home in homes) {
                db.collection("homes")
                    .document(home.id)
                    .collection("tasks")
                    .get()
                    .addOnSuccessListener { taskSnapshot ->
                        val tasks = taskSnapshot.toObjects(Task::class.java)

                        resultList.add(HomeWithTasks(home, tasks))

                        if (resultList.size == homes.size) {
                            _homesWithTasks.value = resultList
                        }
                    }
            }

        }
    }

    fun loadCompletedTasksForHome(homeId: String, period: PeriodType) {
        if (homeId.isEmpty()) {
            _completedTasksData.postValue(emptyList())
            return
        }

        val startTime = getStartTime(System.currentTimeMillis(), period)

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .whereEqualTo("state", "Completada")
            .whereGreaterThanOrEqualTo("completionDate", startTime)
            .orderBy("completionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val completedTasks = snapshot.toObjects(Task::class.java)
                _completedTasksData.postValue(completedTasks)
            }
            .addOnFailureListener { e ->
                Log.w("UserViewModel", "Error fetching completed tasks for $homeId: $e")
                _completedTasksData.postValue(emptyList())
            }
    }

    fun loadUnfinishedTasksForHome(homeId: String) {
        if (homeId.isEmpty()) {
            _unfinishedTasksData.postValue(emptyList())
            return
        }

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .whereEqualTo("state", "Pendiente")
            .get()
            .addOnSuccessListener { snapshot ->
                val unfinishedTasks = snapshot.toObjects(Task::class.java)
                _unfinishedTasksData.postValue(unfinishedTasks)
            }
            .addOnFailureListener { e ->
                Log.w("UserViewModel", "Error fetching unfinished tasks for $homeId: $e")
                _unfinishedTasksData.postValue(emptyList())
            }
    }

    private fun getStartTime(currentTime: Long, period: PeriodType): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (period) {
            PeriodType.DAILY -> {

            }
            PeriodType.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            PeriodType.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun loadUserAssignedTasksProgress(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val homeIds = document.get("homes") as? List<String> ?: emptyList()

                if (homeIds.isEmpty()) {

                    _userTaskProgress.postValue(UserProgres())
                    return@addOnSuccessListener

                }

                val queryTasks = homeIds.map { homeId ->
                    db.collection("homes")
                        .document(homeId)
                        .collection("tasks")
                        .whereArrayContains("assignedTo", userId)
                        .get()
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<QuerySnapshot>(queryTasks)
                    .addOnSuccessListener { listOfSnapshots ->

                        val allAssignedTasks = mutableListOf<Task>()

                        listOfSnapshots.forEach { snapshot ->
                            val tasks = snapshot.toObjects(Task::class.java)
                            allAssignedTasks.addAll(tasks)
                        }

                        val completed = allAssignedTasks.count { it.state == "Completada" }
                        val total = allAssignedTasks.size
                        val pending = total - completed

                        val percentage = if (total > 0) (completed * 100) / total else 0

                        val progressData = UserProgres(
                            totalTasks = total,
                            completedTasks = completed,
                            pendingTasks = pending,
                            progressPercentage = percentage,
                            allTasks = allAssignedTasks
                        )

                        _userTaskProgress.postValue(progressData)
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserViewModel", "Error cargando tareas asignadas globales: $e")
                        _userTaskProgress.postValue(UserProgres())
                    }
            }
            .addOnFailureListener { e ->
                Log.w("UserViewModel", "Error obteniendo usuario para progreso: $e")
            }
    }
}
