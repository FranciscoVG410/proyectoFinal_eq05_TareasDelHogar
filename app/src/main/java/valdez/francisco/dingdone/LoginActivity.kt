package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        var users = mutableListOf<User>()

        var emailNew = intent.getStringExtra("email") ?: ""
        var nameNew = intent.getStringExtra("name") ?: ""
        var passwordNew = intent.getStringExtra("password") ?: ""

        if(!emailNew.isEmpty() || !passwordNew.isEmpty() || !nameNew.isEmpty()){

            users.add(User(nameNew, emailNew, passwordNew))

        }


        val btnLogin : Button = findViewById(R.id.btnLogin)
        val btnRegister : Button = findViewById(R.id.btnRegister)

        val email: EditText = findViewById(R.id.etEmail)
        val password : EditText = findViewById(R.id.etPassword)

        users.add(User("Amos Heli", "amospro@gmail.com", "123456"))

        btnLogin.setOnClickListener { 

            if(email.text.toString().isEmpty() || password.text.toString().isEmpty()){

                email.setBackgroundResource(R.drawable.error_rounded_edit_text)
                password.setBackgroundResource(R.drawable.error_rounded_edit_text)

            }



            for(user in users){

                if(email.text.toString() == user.email && password.text.toString() == user.password){

                    var intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("email", user.email)
                    intent.putExtra("name", user.name)
                    intent.putExtra("password", user.password)
                    startActivity(intent)

                }

            }


        }
        btnRegister.setOnClickListener{

            startActivity(Intent(this, SignUpActivity::class.java))

        }



    }
}