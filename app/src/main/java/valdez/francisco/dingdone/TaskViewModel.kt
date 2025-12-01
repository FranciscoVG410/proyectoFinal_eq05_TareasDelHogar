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

    fun updateTaskStatus(homeId: String, taskId: String, newStatus: String, onComplete: (Boolean) -> Unit) {

        val taskRef = db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document(taskId)

        val completionDateValue: Long? = if (newStatus == "Completada") {
            System.currentTimeMillis()
        } else {
            null
        }

        val updates = hashMapOf<String, Any?>(
            "state" to newStatus,
            "completionDate" to completionDateValue
        )

        taskRef.update(updates as Map<String, Any>)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateEditableMembers(homeId: String, taskId: String, editableBy: List<String>, onComplete: (Boolean) -> Unit) {
        db.collection("homes")
            .document(homeId)
            .collection("tasks")
            .document(taskId)
            .update("editableBy", editableBy)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}