package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var layoutFail: View
    private lateinit var layoutSucces: View
    private lateinit var textFail: TextView
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast
    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        val inflate = layoutInflater
        layoutFail = inflate.inflate(R.layout.custome_toast_fail, null)
        layoutSucces = inflate.inflate(R.layout.custome_toast_success, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        textSuccess = layoutSucces.findViewById(R.id.txtTextToastS)
        toast = Toast(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        etNombre = findViewById(R.id.etRName)
        etEmail = findViewById(R.id.etREmail)
        etPassword = findViewById(R.id.etRPassword)
        etConfirmPassword = findViewById(R.id.etRConfirmPassword)

        val btnCreateAccount: Button = findViewById(R.id.btnCreateAccount)
        val btnBackLogin: Button = findViewById(R.id.btnBackLogin)

        btnCreateAccount.setOnClickListener {
            val name = etNombre.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            etNombre.setBackgroundResource(R.drawable.edittext_bg)
            etEmail.setBackgroundResource(R.drawable.edittext_bg)
            etPassword.setBackgroundResource(R.drawable.edittext_bg)
            etConfirmPassword.setBackgroundResource(R.drawable.edittext_bg)

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {

                textFail.text = "Please fill all fields"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()

                if (name.isEmpty()) etNombre.setBackgroundResource(R.drawable.edittext_bg_error)
                if (email.isEmpty()) etEmail.setBackgroundResource(R.drawable.edittext_bg_error)
                if (password.isEmpty()) etPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                if (confirmPassword.isEmpty()) etConfirmPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                return@setOnClickListener
            }

            if (password != confirmPassword) {

                textFail.text = "Passwords do not match"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()
                etPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                etConfirmPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                return@setOnClickListener
            }

            signIn(email, password)
        }

        btnBackLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun signIn(email: String, password: String) {
        Log.d("INFO", "email: $email, password: $password")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SUCCESS", "createUserWithEmail:success")

                    textSuccess.text = "Account created successfully!"
                    toast.duration = Toast.LENGTH_SHORT
                    toast.view = layoutSucces
                    toast.show()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    Log.w("FAILURE", "createUserWithEmail:failure", task.exception)

                    textFail.text = "Authentication failed: ${task.exception?.message}"
                    toast.duration = Toast.LENGTH_LONG
                    toast.view = layoutFail
                    toast.show()

                    etEmail.setBackgroundResource(R.drawable.edittext_bg_error)
                    etPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                }
            }
    }
}
