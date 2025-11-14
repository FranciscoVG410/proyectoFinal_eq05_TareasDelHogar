package valdez.francisco.dingdone

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class TaskViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    fun createTask(homeId: String, task: Task, onComplete: (Boolean) -> Unit) {
        val taskId = db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document().id

        val taskData = hashMapOf(
            "id" to taskId,
            "name" to task.nombre,
            "description" to task.descripcio,
            "members" to task.member,
            "days" to task.date,
            "status" to task.state
        )

        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document(taskId)
            .set(taskData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
