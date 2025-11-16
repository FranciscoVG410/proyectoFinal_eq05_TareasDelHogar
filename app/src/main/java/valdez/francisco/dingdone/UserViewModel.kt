package valdez.francisco.dingdone

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // LiveData con las homes del usuario
    private val _userHomes = MutableLiveData<List<Home>>()
    val userHomes: LiveData<List<Home>> get() = _userHomes

    // LiveData con mapa homeId → lista de tasks
    private val _homeTasks = MutableLiveData<Map<String, List<Task>>>()
    val homeTasks: LiveData<Map<String, List<Task>>> get() = _homeTasks

    // LiveData para objeto unificado Home + Tasks
    private val _homesWithTasks = MutableLiveData<List<HomeWithTasks>>()
    val homesWithTasks: LiveData<List<HomeWithTasks>> get() = _homesWithTasks

    // LiveData específico para los datos del gráfico de tareas completadas
    private val _completedTasksData = MutableLiveData<List<Task>>()
    val completedTasksData: LiveData<List<Task>> get() = _completedTasksData

    fun loadUserHomes(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("user_home")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { relSnap ->
                val homeIds = relSnap.documents.mapNotNull { it.getString("homeId") }
                if (homeIds.isEmpty()) {
                    _userHomes.postValue(emptyList())
                    return@addOnSuccessListener
                }

                // Firestore whereIn tiene limite de 10 por query; manejalo si hay muchos ids
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

    fun loadCompletedTasksForHome(homeId: String) {
        if (homeId.isEmpty()) {
            _completedTasksData.postValue(emptyList())
            return
        }

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .whereEqualTo("state", "Completada")
            .get()
            .addOnSuccessListener { snapshot ->
                val completedTasks = snapshot.toObjects(Task::class.java)
                _completedTasksData.postValue(completedTasks)
            }
            .addOnFailureListener { e ->
                Log.w("GraphsViewModel", "Error fetching completed tasks for $homeId: $e")
                _completedTasksData.postValue(emptyList())
            }
    }
}
