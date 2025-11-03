package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password : EditText

    private lateinit var layoutFail: View
    private lateinit var layoutSucces: View
    private lateinit var textFail: TextView
    private lateinit var textSuccess: TextView
    private lateinit var toast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {

        val inflate = layoutInflater
        layoutFail = inflate.inflate(R.layout.custome_toast_fail, null)
        layoutSucces = inflate.inflate(R.layout.custome_toast_success, null)
        textFail = layoutFail.findViewById(R.id.txtTextToastF)
        textSuccess = layoutSucces.findViewById(R.id.txtTextToastS)
        toast = Toast(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        auth.signOut()

//        var users = mutableListOf<User>()

//        var emailNew = intent.getStringExtra("email") ?: ""
//        var nameNew = intent.getStringExtra("name") ?: ""
//        var passwordNew = intent.getStringExtra("password") ?: ""

//        if(!emailNew.isEmpty() || !passwordNew.isEmpty() || !nameNew.isEmpty()){
//
//            users.add(User(nameNew, emailNew, passwordNew))
//
//        }


        val btnLogin : Button = findViewById(R.id.btnLogin)
        val btnRegister : Button = findViewById(R.id.btnRegister)

        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)

//        users.add(User("Amos Heli", "amos", "amos"))

        btnLogin.setOnClickListener { 

            if(email.text.toString().isEmpty() || password.text.toString().isEmpty()){

                textFail.text = "Pleas fill all fields"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()
                errorMarco()

            }else {

                login(email.text.toString(), password.text.toString())

            }





//            for(user in users){
//
//                if(email.text.toString() == user.email && password.text.toString() == user.password){
//
//                    val intent = Intent(this, Configuration::class.java)
//                    intent.putExtra("email", user.email)
//                    intent.putExtra("name", user.name)
//                    intent.putExtra("password", user.password)
//                    startActivity(intent)
//                }
//
//            }


        }
        btnRegister.setOnClickListener{

            startActivity(Intent(this, SignUpActivity::class.java))

        }



    }

    fun login(email: String, password: String){

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) {

            accion -> if(accion.isSuccessful){

                val user = auth.currentUser
                textSuccess.text = "Welcome " + email.substringBefore("@")
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutSucces
                toast.show()
                goToMain(user!!)

            }else {

                textFail.text = "Incorrect credentials. Please try again"
                toast.duration = Toast.LENGTH_SHORT
                toast.view = layoutFail
                toast.show()
                errorMarco()

            }

        }

    }

    fun errorMarco(){

        email.setBackgroundResource(R.drawable.error_rounded_edit_text)
        password.setBackgroundResource(R.drawable.error_rounded_edit_text)


    }

    fun goToMain(currentUser: FirebaseUser){

        val intent = Intent(this, TasksActivity::class.java)
        intent.putExtra("email", currentUser.email)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

    }

    public override fun onStart(){

        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){

            goToMain(currentUser)

        }

    }

    fun eliminateGmail(correo : String){



    }

}