package valdez.francisco.dingdone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class JoinHomeFragment : Fragment() {

    private lateinit var etInvitationCode: EditText
    private lateinit var btnJoinHome: Button
    private lateinit var btnCancel: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val homeShareViewModel: HomeShareViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_join_home, container, false)

        etInvitationCode = view.findViewById(R.id.et_invitationCode)
        btnJoinHome = view.findViewById(R.id.btnJoinHome)
        btnCancel = view.findViewById(R.id.btnCancel)

        btnJoinHome.setOnClickListener {
            val code = etInvitationCode.text.toString().uppercase()

            if (code.length != 5) {
                Toast.makeText(requireContext(), "El código debe tener 5 caracteres.", Toast.LENGTH_SHORT).show()
                etInvitationCode.setBackgroundResource(R.drawable.error_rounded_edit_text)
            } else {
                etInvitationCode.setBackgroundResource(R.drawable.rounded_edit_text)
                joinHome(code)
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun joinHome(invitationCode: String) {

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return

        }

        db.collection("homes")
            .whereEqualTo("invitationCode", invitationCode)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "Home not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val homeDocument = querySnapshot.documents.first()
                val homeId = homeDocument.id
                val currentMembers = homeDocument.get("members") as? List<String> ?: emptyList()

                if (currentMembers.contains(userId)) {
                    Toast.makeText(requireContext(), "You are already a member of this Home", Toast.LENGTH_SHORT).show()

                    homeShareViewModel.selectHome(homeId)

                    parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TasksFragmentNew())
                        .commit()
                    return@addOnSuccessListener
                }

                updateUserAndHome(userId, homeId, homeDocument.reference)
            }
            .addOnFailureListener { e ->
                Log.e("JoinHome", "Error finding home: ${e.message}", e)
                Toast.makeText(requireContext(), "Error conection", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserAndHome(userId: String, homeId: String, homeRef: com.google.firebase.firestore.DocumentReference) {

        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            transaction.update(homeRef, "members", FieldValue.arrayUnion(userId))
            transaction.update(userRef, "homes", FieldValue.arrayUnion(homeId))
            null
        }.addOnSuccessListener {

            Log.d("JoinHome", "Usuario $userId succesfully joined in the Home $homeId")
            Toast.makeText(requireContext(), "Welcome to your new Home", Toast.LENGTH_LONG).show()

            homeShareViewModel.selectHome(homeId)

            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TasksFragmentNew())
                .commit()

        }.addOnFailureListener { e ->
            Log.e("JoinHome", "Transaction failed: ${e.message}", e)
            Toast.makeText(requireContext(), "Join failed, try again", Toast.LENGTH_SHORT).show()
        }
    }
}