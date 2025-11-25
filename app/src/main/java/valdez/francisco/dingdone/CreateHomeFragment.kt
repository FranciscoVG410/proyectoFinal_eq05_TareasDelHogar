package valdez.francisco.dingdone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateHomeFragment : Fragment() {

    private lateinit var layoutFail: View
    private lateinit var layoutSucces: View
    private lateinit var textFail: TextView
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_home, container, false)

        layoutFail = inflater.inflate(R.layout.custome_toast_fail, null)
        layoutSucces = inflater.inflate(R.layout.custome_toast_success, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        textSuccess = layoutSucces.findViewById(R.id.txtTextToastS)
        toast = Toast(requireContext())

        val etHomeName: EditText = view.findViewById(R.id.et_homeName)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnCreate: Button = view.findViewById(R.id.btnCreate)
        val tvRandomCode: TextView = view.findViewById(R.id.tv_randomCode)

        btnCreate.setOnClickListener {
            val homeName = etHomeName.text.toString()

            if (homeName.isEmpty()) {
                textFail.text = "Please enter a name for the family"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()
                etHomeName.setBackgroundResource(R.drawable.error_rounded_edit_text)
            } else {
                createHomeInFirestore(homeName) { invitationCode ->
                    // Mostrar código en pantalla
                    tvRandomCode.text = invitationCode

                    // Pasar a siguiente fragmento
                    val fragment = HomeCreatedFragment().apply {
                        arguments = Bundle().apply {
                            putString("invitationCode", invitationCode)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    /**
     * Crea una Home en Firestore y devuelve el código de invitación.
     */
    private fun createHomeInFirestore(homeName: String, callback: (String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("CreateHome", "No authenticated user")
            callback(null)
            return
        }

        val db = FirebaseFirestore.getInstance()

        // 1) Crear documento home con ID automático
        val newHomeRef = db.collection("homes").document()
        val homeId = newHomeRef.id
        val invitationCode = homeId.takeLast(5).uppercase()

        val homeData = hashMapOf(
            "id" to homeId,
            "name" to homeName,
            "ownerId" to userId,
            "members" to listOf(userId),
            "invitationCode" to invitationCode,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // 2) Guardar home
        newHomeRef.set(homeData)
            .addOnSuccessListener {
                Log.d("CreateHome", "Home created: $homeId")

                // 3) Crear relación user_home (tabla mediadora)
                // Puedes usar un id compuesto "userId_homeId" o dejar que Firestore genere uno.
                val userHomeData = hashMapOf(
                    "userId" to userId,
                    "homeId" to homeId,
                    "role" to "owner",
                    "joinedAt" to FieldValue.serverTimestamp()
                )

                db.collection("user_home")
                    .add(userHomeData)
                    .addOnSuccessListener { relDoc ->
                        Log.d("CreateHome", "user_home created: ${relDoc.id}")

                        // 4) Opcional: actualizar documento user con arrayUnion(homeId)
                        val userRef = db.collection("users").document(userId)
                        userRef.update("homes", FieldValue.arrayUnion(homeId))
                            .addOnSuccessListener {
                                Log.d("CreateHome", "User document updated with homeId")
                                // todo ok -> devolver invitation code
                                callback(invitationCode)
                            }
                            .addOnFailureListener { e ->
                                Log.w("CreateHome", "Failed updating user doc: $e")
                                // aunque falle actualizar el user doc, la home y relación existen
                                callback(invitationCode)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.w("CreateHome", "Failed creating user_home: $e")
                        callback(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("CreateHome", "Failed creating home: $e")
                callback(null)
            }
    }

}
