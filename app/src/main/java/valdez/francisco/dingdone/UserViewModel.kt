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

    private val _userTasksGroupedByHome = MutableLiveData<List<Pair<Home, List<Task>>>>()
    val userTasksGroupedByHome: LiveData<List<Pair<Home, List<Task>>>> get() = _userTasksGroupedByHome

    private val _homeMembers = MutableLiveData<List<User>>()
    val homeMembers: LiveData<List<User>> get() = _homeMembers

    private val _currentHomeTasks = MutableLiveData<List<Task>>()
    val currentHomeTasks: LiveData<List<Task>> get() = _currentHomeTasks

    private val _currentHomeDetails = MutableLiveData<Home?>()
    val currentHomeDetails: LiveData<Home?> get() = _currentHomeDetails

    private val _userNamesMap = MutableLiveData<Map<String, String>>(emptyMap())
    val userNamesMap: LiveData<Map<String, String>> get() = _userNamesMap

    fun loadHomeMembers(homeId: String) {
        db.collection("homes").document(homeId).get()
            .addOnSuccessListener { document ->
                val memberIds = document.get("memberIds") as? List<String> ?: emptyList()

                if (memberIds.isEmpty()) {
                    _homeMembers.postValue(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), memberIds)
                    .get()
                    .addOnSuccessListener { usersSnapshot ->
                        val members = usersSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.apply { id = doc.id }
                        }
                        _homeMembers.postValue(members)
                        updateUserNamesMap(members)
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserViewModel", "Error cargando miembros de la casa $homeId: $e")
                        _homeMembers.postValue(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error obteniendo Home para miembros $homeId: $e")
                _homeMembers.postValue(emptyList())
            }
    }

    private fun updateUserNamesMap(users: List<User>) {
        val currentMap = _userNamesMap.value.orEmpty().toMutableMap()
        users.forEach { user ->
            currentMap[user.id] = user.name
        }
        _userNamesMap.postValue(currentMap)
    }

    fun loadUserNamesForHome(homeId: String) {
        loadHomeMembers(homeId)
    }

    fun loadHomeDetails(homeId: String) {
        db.collection("homes").document(homeId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("UserViewModel", "Error al escuchar detalles de Home $homeId: $e")
                _currentHomeDetails.postValue(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val home = snapshot.toObject(Home::class.java)
                _currentHomeDetails.postValue(home)
            } else {
                _currentHomeDetails.postValue(null)
            }
        }
    }

    fun updateHomeName(homeId: String, newName: String) {
        db.collection("homes").document(homeId)
            .update("name", newName)
            .addOnSuccessListener {
                Log.d("UserViewModel", "Nombre de home $homeId actualizado a: $newName")
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error actualizando el nombre del home $homeId: $e")
            }
    }

    fun updateHomeEditPermission(homeId: String, canEdit: Boolean) {
        db.collection("homes").document(homeId)
            .update("membersCanEdit", canEdit)
            .addOnSuccessListener {
                Log.d("UserViewModel", "Permiso de edición actualizado para home $homeId a $canEdit")
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error actualizando permiso de edición para home $homeId: $e")
            }
    }

    fun loadCurrentHomeTasks(homeId: String) {
        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .orderBy("state", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.toObjects(Task::class.java)
                _currentHomeTasks.postValue(tasks)
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error cargando tareas para Home $homeId: $e")
                _currentHomeTasks.postValue(emptyList())
            }
    }

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

        loadHomeMembers(homeId) { membersMap ->
            db.collection("homes")
                .document(homeId)
                .collection("tasks")
                .whereEqualTo("state", "Completada")
                .get()
                .addOnSuccessListener { snapshot ->
                    val completedTasks = snapshot.documents.mapNotNull { doc ->
                        val task = doc.toObject(Task::class.java)?.apply { id = doc.id }
                        if (task?.completionDate != null && task.completionDate!! >= startTime) task else null
                    }

                    completedTasks.forEach { task ->
                        task.member = task.member.map { memberId -> membersMap[memberId] ?: "USUARIO DESCONOCIDO" }
                    }

                    _completedTasksData.postValue(completedTasks)
                }
                .addOnFailureListener { e ->
                    Log.e("UserViewModel", "Error cargando completadas $homeId: $e")
                    _completedTasksData.postValue(emptyList())
                }
        }
    }

    fun loadUnfinishedTasksForHome(homeId: String, period: PeriodType) {
        if (homeId.isEmpty()) {
            _unfinishedTasksData.postValue(emptyList())
            return
        }

        val startTime = getStartTime(System.currentTimeMillis(), period)

        loadHomeMembers(homeId) { membersMap ->
            db.collection("homes")
                .document(homeId)
                .collection("tasks")
                .whereEqualTo("state", "Pendiente")
                .get()
                .addOnSuccessListener { snapshot ->
                    val unfinishedTasks = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Task::class.java)?.apply { id = doc.id }
                    }

                    unfinishedTasks.forEach { task ->
                        task.member = task.member.map { memberId -> membersMap[memberId] ?: "USUARIO DESCONOCIDO" }
                    }

                    _unfinishedTasksData.postValue(unfinishedTasks)
                }
                .addOnFailureListener { e ->
                    Log.e("UserViewModel", "Error cargando pendientes $homeId: $e")
                    _unfinishedTasksData.postValue(emptyList())
                }
        }
    }

    fun loadHomeMembers(homeId: String, callback: (Map<String, String>) -> Unit) {
        db.collection("homes").document(homeId).get()
            .addOnSuccessListener { document ->
                val memberIds = document.get("memberIds") as? List<String> ?: emptyList()

                if (memberIds.isEmpty()) {
                    _homeMembers.postValue(emptyList())
                    callback(emptyMap())
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), memberIds)
                    .get()
                    .addOnSuccessListener { usersSnapshot ->
                        val members = usersSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.apply { id = doc.id }
                        }
                        _homeMembers.postValue(members)

                        val membersMap = members.associate { it.id to it.name }
                        _userNamesMap.postValue(membersMap)
                        callback(membersMap)
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserViewModel", "Error cargando miembros de la casa $homeId: $e")
                        _homeMembers.postValue(emptyList())
                        callback(emptyMap())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error obteniendo Home para miembros $homeId: $e")
                _homeMembers.postValue(emptyList())
                callback(emptyMap())
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

    fun loadUserTasksGroupedByHome(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val homeIds = document.get("homes") as? List<String> ?: emptyList()

                if (homeIds.isEmpty()) {
                    _userTasksGroupedByHome.postValue(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("homes")
                    .whereIn(FieldPath.documentId(), homeIds)
                    .get()
                    .addOnSuccessListener { homesSnapshot ->
                        val homes = homesSnapshot.toObjects(Home::class.java)

                        if (homes.isEmpty()) {
                            _userTasksGroupedByHome.postValue(emptyList())
                            return@addOnSuccessListener
                        }

                        val resultList = mutableListOf<Pair<Home, List<Task>>>()
                        var completedHomes = 0

                        homes.forEach { home ->
                            db.collection("homes")
                                .document(home.id)
                                .collection("tasks")
                                .whereArrayContains("assignedTo", userId)
                                .get()
                                .addOnSuccessListener { tasksSnapshot ->
                                    val tasks = tasksSnapshot.toObjects(Task::class.java)
                                    resultList.add(Pair(home, tasks))
                                    completedHomes++

                                    if (completedHomes == homes.size) {
                                        _userTasksGroupedByHome.postValue(resultList)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UserViewModel", "Error cargando tareas para home ${home.id}: $e")
                                    resultList.add(Pair(home, emptyList()))
                                    completedHomes++

                                    if (completedHomes == homes.size) {
                                        _userTasksGroupedByHome.postValue(resultList)
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserViewModel", "Error cargando homes: $e")
                        _userTasksGroupedByHome.postValue(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.w("UserViewModel", "Error obteniendo usuario: $e")
                _userTasksGroupedByHome.postValue(emptyList())
            }
    }
}