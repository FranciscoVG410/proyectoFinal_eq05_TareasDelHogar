package valdez.francisco.dingdone

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

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
    val userNamesMap: LiveData<Map<String, String>> get() = _userNamesMap // Este es el LiveData clave

    private val _totalLifetimeCompleted = MutableLiveData<Int>()
    val totalLifetimeCompleted: LiveData<Int> get() = _totalLifetimeCompleted

    private val _homeOwnerName = MutableLiveData<String>()
    val homeOwnerName: LiveData<String> get() = _homeOwnerName

    private val _homeMemberCount = MutableLiveData<Int>()
    val homeMemberCount: LiveData<Int> get() = _homeMemberCount


    // =========================================================================
    // LÓGICA: Carga un mapa de ID de usuario -> Nombre para una casa
    // =========================================================================
    fun loadUserNamesMapForHome(homeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Obtener la lista de miembros (IDs) de la casa
                val homeDoc = db.collection("homes").document(homeId).get().await()

                // CORRECCIÓN: Usar "members" en lugar de "memberIds"
                val memberIds = homeDoc.get("members") as? List<String> ?: emptyList()

                if (memberIds.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _userNamesMap.value = emptyMap()
                    }
                    return@launch
                }

                // 2. Consultar la colección de usuarios para obtener los datos de todos los miembros
                // Nota: usamos .take(10) para evitar superar el límite de 10 elementos de Firestore en el 'whereIn'.
                val usersSnapshot = db.collection("users")
                    .whereIn(FieldPath.documentId(), memberIds.take(10))
                    .get().await()

                val namesMap = mutableMapOf<String, String>()

                // 3. Construir el mapa de ID -> Nombre
                usersSnapshot.documents.forEach { doc ->
                    val userId = doc.id
                    // Buscamos "name" o "nombre" para ser robustos.
                    val userName = doc.getString("name") ?: doc.getString("nombre") ?: "Usuario Desconocido"
                    namesMap[userId] = userName
                }

                // 4. Publicar el mapa en el LiveData
                withContext(Dispatchers.Main) {
                    _userNamesMap.value = namesMap
                    Log.d("UserViewModel", "Mapa de nombres cargado: ${namesMap.size} usuarios")
                }

            } catch (e: Exception) {
                Log.e("UserViewModel", "Error cargando mapa de nombres para casa $homeId", e)
                withContext(Dispatchers.Main) {
                    _userNamesMap.value = emptyMap()
                }
            }
        }
    }
    // =========================================================================
    // FIN LÓGICA DE RESOLUCIÓN DE NOMBRES
    // =========================================================================


    fun loadHomeMembers(homeId: String) {
        db.collection("homes").document(homeId).get()
            .addOnSuccessListener { document ->
                // CORRECCIÓN: Usar "members" en lugar de "memberIds"
                val memberIds = document.get("members") as? List<String> ?: emptyList()

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
                        // Llama a la función de utilidad para actualizar el mapa de nombres
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
            // Asume que la clase User tiene un campo 'name' o que 'nombre' es lo que quieres
            currentMap[user.id] = user.name
        }
        _userNamesMap.postValue(currentMap)
    }

    fun loadUserNamesForHome(homeId: String) {
        // Redirigimos la llamada a la nueva función basada en coroutines para asegurar la carga
        loadUserNamesMapForHome(homeId)
    }

    fun loadHomeProfileFullDetails(homeId: String) {
        if (homeId.isEmpty()) return

        db.collection("homes").document(homeId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    val home = document.toObject(Home::class.java)?.apply { id = document.id }
                    _currentHomeDetails.value = home

                    // Asumiendo que la clase Home está bien mapeada al campo 'members'
                    val memberIds = home?.members ?: emptyList()
                    _homeMemberCount.value = memberIds.size

                    val ownerId = home?.ownerId ?: ""
                    if (ownerId.isNotEmpty()) {
                        db.collection("users").document(ownerId).get()
                            .addOnSuccessListener { userDoc ->
                                val ownerName = userDoc.getString("name") ?: "Desconocido"
                                _homeOwnerName.value = ownerName
                            }
                            .addOnFailureListener {
                                _homeOwnerName.value = "Desconocido"
                            }
                    } else {
                        _homeOwnerName.value = "Sin dueño"
                    }

                    if (memberIds.isNotEmpty()) {

                        db.collection("users")
                            .whereIn(FieldPath.documentId(), memberIds.take(10))
                            .get()
                            .addOnSuccessListener { usersSnapshot ->
                                val membersList = usersSnapshot.documents.mapNotNull { doc ->
                                    doc.toObject(User::class.java)?.apply { id = doc.id }
                                }
                                _homeMembers.value = membersList
                                // Carga el mapa de nombres aquí también
                                updateUserNamesMap(membersList)
                            }
                    } else {
                        _homeMembers.value = emptyList()
                    }

                } else {

                    _currentHomeDetails.value = null
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error cargando detalles de casa $homeId", e)
            }
    }


    fun loadHomeDetails(homeId: String) {
        db.collection("homes").document(homeId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    val home = document.toObject(Home::class.java)
                    val members = home?.members ?: emptyList()
                    _homeMemberCount.value = members.size
                    val ownerId = home?.ownerId ?: ""

                    if (ownerId.isNotEmpty()) {

                        db.collection("users").document(ownerId).get()
                            .addOnSuccessListener { userDoc ->

                                val ownerName = userDoc.getString("name") ?: "Desconocido"
                                _homeOwnerName.value = ownerName

                            }
                            .addOnFailureListener {

                                _homeOwnerName.value = "Error al cargar"

                            }
                    } else {

                        _homeOwnerName.value = "Sin dueño"

                    }
                }
            }
            .addOnFailureListener {

                _homeMemberCount.value = 0
                _homeOwnerName.value = "Error"

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

    private val firebaseDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    fun parseHistoryDate(dateStr: String): Long? = try {
        firebaseDateFormat.parse(dateStr)?.time
    } catch (e: Exception) {
        null
    }

    // Funciones de utilidad para el progreso (mantengo las que enviaste)
    private fun getTodayDateCode(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    private fun getMonthCode(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
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
            PeriodType.DAILY -> Unit
            PeriodType.WEEKLY -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // Ajusta para que lunes sea el día 2 independientemente de localización
                val diff = if (dayOfWeek == Calendar.SUNDAY) {
                    -6   // domingo → restar 6 días para ir al lunes pasado
                } else {
                    Calendar.MONDAY - dayOfWeek
                }

                calendar.add(Calendar.DAY_OF_MONTH, diff)
            }
            PeriodType.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    private fun getEndTime(startTime: Long, period: PeriodType): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startTime
        }

        when (period) {
            PeriodType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // mañana 00:00
            }
            PeriodType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1) // siguiente lunes 00:00
            }
            PeriodType.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1) // primer día del siguiente mes 00:00
            }
        }

        // Regresamos 1 ms para incluir todo el último día
        return calendar.timeInMillis - 1
    }

    fun loadCompletedTasksForHome(homeId: String, period: PeriodType) {
        if (homeId.isEmpty()) {
            _completedTasksData.postValue(emptyList())
            return
        }

        val now = System.currentTimeMillis()
        val startTime = getStartTime(now, period)
        val endTime = getEndTime(startTime, period)

        val memberNameCache = mutableMapOf<String, String>()

        fun getMemberName(memberId: String, onResult: (String) -> Unit) {
            // 1) Revisa caché primero
            memberNameCache[memberId]?.let {
                onResult(it)
                return
            }

            // 2) Consulta en Firestore
            db.collection("users")
                .document(memberId)
                .get()
                .addOnSuccessListener { snap ->
                    val name = snap.getString("name") ?: memberId
                    memberNameCache[memberId] = name
                    onResult(name)
                }
                .addOnFailureListener {
                    onResult(memberId) // fallback: regresa el id si falla
                }
        }

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.toObjects(Task::class.java)

                val completedTasksForChart = mutableListOf<Task>()
                var processedTasks = 0

                if (tasks.isEmpty()) {
                    _completedTasksData.postValue(emptyList())
                    return@addOnSuccessListener
                }

                tasks.forEach { task ->
                    db.collection("homes").document(homeId)
                        .collection("tasks").document(task.id)
                        .collection("history")
                        .whereEqualTo("status", "Completada")
                        .get()
                        .addOnSuccessListener { historySnap ->

                            val historyDocs = historySnap.documents
                            if (historyDocs.isEmpty()) {
                                processedTasks++
                                if (processedTasks == tasks.size) {
                                    _completedTasksData.postValue(completedTasksForChart)
                                }
                                return@addOnSuccessListener
                            }

                            var processedHistory = 0
                            historyDocs.forEach { entry ->
                                val completedAtStr = entry.getString("completedAt")
                                val completedById = entry.getString("completedBy")

                                if (completedAtStr != null && completedById != null) {
                                    val completedAt = parseHistoryDate(completedAtStr) // Long?

                                    // 🔹 FILTRO POR PERIODO (daily/weekly/monthly)
                                    if (completedAt != null &&
                                        completedAt in startTime..endTime
                                    ) {
                                        // Aquí convertimos ID → nombre
                                        getMemberName(completedById) { memberName ->
                                            val taskClone = task.copy(
                                                state = "Completada",
                                                // ahora member lleva el nombre
                                                member = listOf(memberName)
                                            )
                                            completedTasksForChart.add(taskClone)

                                            processedHistory++
                                            if (processedHistory == historyDocs.size) {
                                                processedTasks++
                                                if (processedTasks == tasks.size) {
                                                    _completedTasksData.postValue(
                                                        completedTasksForChart
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        processedHistory++
                                        if (processedHistory == historyDocs.size) {
                                            processedTasks++
                                            if (processedTasks == tasks.size) {
                                                _completedTasksData.postValue(
                                                    completedTasksForChart
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    processedHistory++
                                    if (processedHistory == historyDocs.size) {
                                        processedTasks++
                                        if (processedTasks == tasks.size) {
                                            _completedTasksData.postValue(
                                                completedTasksForChart
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("UVM", "Error cargando historial de tarea ${task.id}: $e")
                            processedTasks++
                            if (processedTasks == tasks.size) {
                                _completedTasksData.postValue(completedTasksForChart)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UVM", "Error cargando tareas para completed: $e")
                _completedTasksData.postValue(emptyList())
            }
    }

    fun loadUnfinishedTasksForHome(homeId: String, period: PeriodType) {
        if (homeId.isEmpty()) {
            _unfinishedTasksData.postValue(emptyList())
            return
        }

        val memberNameCache = mutableMapOf<String, String>()

        fun getMemberName(memberId: String, onResult: (String) -> Unit) {
            memberNameCache[memberId]?.let {
                onResult(it)
                return
            }

            db.collection("users").document(memberId).get()
                .addOnSuccessListener { snap ->
                    val name = snap.getString("name") ?: memberId
                    memberNameCache[memberId] = name
                    onResult(name)
                }
                .addOnFailureListener {
                    onResult(memberId)
                }
        }

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.toObjects(Task::class.java)
                val unfinishedList = mutableListOf<Task>()
                var processedTasks = 0

                if (tasks.isEmpty()) {
                    _unfinishedTasksData.postValue(emptyList())
                    return@addOnSuccessListener
                }

                tasks.forEach { task ->

                    db.collection("homes").document(homeId)
                        .collection("tasks").document(task.id)
                        .collection("history")
                        .get()
                        .addOnSuccessListener { historySnap ->

                            val historyDocs = historySnap.documents

                            val assignedList = if (task.assignedTo.isNotEmpty()) task.assignedTo else task.member

                            // Si no hay historial → Uncompleted con assignedList
                            if (historyDocs.isEmpty()) {

                                val memberNames = mutableListOf<String>()
                                var processedMembers = 0

                                if (assignedList.isEmpty()) {
                                    unfinishedList.add(task.copy(state = "Pendiente", member = emptyList()))
                                    processedTasks++
                                    if (processedTasks == tasks.size)
                                        _unfinishedTasksData.postValue(unfinishedList)
                                    return@addOnSuccessListener
                                }

                                assignedList.forEach { memberIdOrName ->
                                    fun addMember(name: String) {
                                        memberNames.add(name)
                                        processedMembers++
                                        if (processedMembers == assignedList.size) {
                                            unfinishedList.add(task.copy(state = "Pendiente", member = memberNames))
                                            processedTasks++
                                            if (processedTasks == tasks.size)
                                                _unfinishedTasksData.postValue(unfinishedList)
                                        }
                                    }

                                    if (memberIdOrName.length < 20) {
                                        addMember(memberIdOrName)
                                    } else {
                                        getMemberName(memberIdOrName) { name ->
                                            addMember(name)
                                        }
                                    }
                                }

                            } else {
                                // Si hay historial, revisar si tiene completadas
                                val wasCompleted = historyDocs.any { entry ->
                                    entry.getString("completedBy") != null
                                }

                                if (!wasCompleted) {
                                    val memberNames = mutableListOf<String>()
                                    var processedMembers = 0

                                    if (assignedList.isEmpty()) {
                                        unfinishedList.add(task.copy(state = "Pendiente", member = emptyList()))
                                        processedTasks++
                                        if (processedTasks == tasks.size)
                                            _unfinishedTasksData.postValue(unfinishedList)
                                        return@addOnSuccessListener
                                    }

                                    assignedList.forEach { memberIdOrName ->
                                        fun addMember(name: String) {
                                            memberNames.add(name)
                                            processedMembers++
                                            if (processedMembers == assignedList.size) {
                                                unfinishedList.add(task.copy(state = "Pendiente", member = memberNames))
                                                processedTasks++
                                                if (processedTasks == tasks.size)
                                                    _unfinishedTasksData.postValue(unfinishedList)
                                            }
                                        }

                                        if (memberIdOrName.length < 20) {
                                            addMember(memberIdOrName)
                                        } else {
                                            getMemberName(memberIdOrName) { name ->
                                                addMember(name)
                                            }
                                        }
                                    }
                                } else {
                                    // Historial tiene completadas → no agregar
                                    processedTasks++
                                    if (processedTasks == tasks.size)
                                        _unfinishedTasksData.postValue(unfinishedList)
                                }
                            }

                        }
                        .addOnFailureListener {
                            processedTasks++
                            if (processedTasks == tasks.size)
                                _unfinishedTasksData.postValue(unfinishedList)
                        }
                }
            }
            .addOnFailureListener {
                _unfinishedTasksData.postValue(emptyList())
            }
    }

    fun loadUserAssignedTasksProgress(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val userDoc = db.collection("users").document(userId).get().await()

                val homesSnapshot = db.collection("homes")
                    .whereArrayContains("members", userId)
                    .get().await()

                var totalExpectedThisWeek = 0
                var totalCompletedThisWeek = 0

                val weekDatesMap = getCurrentWeekMap()

                for (homeDoc in homesSnapshot.documents) {
                    val homeId = homeDoc.id

                    val tasksSnapshot = db.collection("homes").document(homeId)
                        .collection("tasks")
                        .get().await()

                    val allTasks = tasksSnapshot.toObjects(Task::class.java)
                    val myTasks = allTasks.filter { task ->
                        task.assignedTo.contains(userId)
                    }

                    for (task in myTasks) {

                        val daysScheduled = task.date

                        for (dayName in daysScheduled) {

                            val dateCode = weekDatesMap.entries.find {

                                it.key.equals(dayName, ignoreCase = true)

                            }?.value

                            if (dateCode != null) {

                                totalExpectedThisWeek++

                                // Se accede directamente al documento de historial usando el dateCode (ej: "2025-11-30")
                                val historySnap = db.collection("homes").document(homeId)
                                    .collection("tasks").document(task.id)
                                    .collection("history").document(dateCode)
                                    .get().await()

                                if (historySnap.exists() && historySnap.getString("status") == "Completada") {

                                    totalCompletedThisWeek++

                                }

                            }

                        }

                    }

                }

                val percentage = if (totalExpectedThisWeek > 0) {

                    (totalCompletedThisWeek * 100) / totalExpectedThisWeek

                } else {

                    0

                }

                withContext(Dispatchers.Main) {

                    _userTaskProgress.value = UserProgres(

                        completedTasks = totalCompletedThisWeek,
                        totalTasks = totalExpectedThisWeek,
                        progressPercentage = percentage

                    )

                    Log.d("UVM_Progress", "Semanal: $totalCompletedThisWeek hechos de $totalExpectedThisWeek")

                }

            } catch (e: Exception) {

                Log.e("UVM_Progress", "Error cargando progreso semanal", e)

            }
        }
    }

    fun loadTotalCompletedTasks(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val querySnapshot = db.collectionGroup("history")
                    .whereEqualTo("completedBy", userId)
                    .whereEqualTo("status", "Completada")
                    .get()
                    .await()

                val count = querySnapshot.size()

                withContext(Dispatchers.Main) {
                    _totalLifetimeCompleted.value = count
                    Log.d("UVM_History", "Total histórico completado por mí: $count")
                }
            } catch (e: Exception) {

                Log.e("UVM_History", "Error cargando histórico. ¿Falta índice?", e)
                withContext(Dispatchers.Main) {
                    _totalLifetimeCompleted.value = 0
                }
            }
        }
    }

    private fun getCurrentWeekMap(): Map<String, String> {

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNames = mapOf(

            Calendar.MONDAY to "Lunes",
            Calendar.TUESDAY to "Martes",
            Calendar.WEDNESDAY to "Miercoles",
            Calendar.THURSDAY to "Jueves",
            Calendar.FRIDAY to "Viernes",
            Calendar.SATURDAY to "Sabado",
            Calendar.SUNDAY to "Domingo"

        )

        val weekMap = mutableMapOf<String, String>()

        for (i in 0..6) {

            val dayInt = calendar.get(Calendar.DAY_OF_WEEK)
            val dayString = dayNames[dayInt] ?: ""
            val dateCode = dateFormat.format(calendar.time)

            if (dayString.isNotEmpty()) {

                weekMap[dayString] = dateCode

            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)

        }

        return weekMap
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