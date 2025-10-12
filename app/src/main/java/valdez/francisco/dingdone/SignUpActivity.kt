package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        var etNombre : EditText = findViewById(R.id.etRName)
        var etEmail : EditText = findViewById(R.id.etREmail)
        var etPassword : EditText = findViewById(R.id.etRPassword)
        var etConfirmPassword : EditText = findViewById(R.id.etRConfirmPassword)

        var btnCreateAccount : Button = findViewById(R.id.btnCreateAccount)
        var btnBackLogin : Button = findViewById(R.id.btnBackLogin)

        btnCreateAccount.setOnClickListener{

            if(etNombre.text.toString().isEmpty() || etEmail.text.toString().isEmpty() || etPassword.text.toString().isEmpty() || etConfirmPassword.text.toString().isEmpty()){

                etEmail.setBackgroundResource(R.drawable.error_rounded_edit_text)
                etPassword.setBackgroundResource(R.drawable.error_rounded_edit_text)
                etNombre.setBackgroundResource(R.drawable.error_rounded_edit_text)
                etConfirmPassword.setBackgroundResource(R.drawable.error_rounded_edit_text)


            } else if(etPassword.text.toString() == etConfirmPassword.text.toString()){

                var intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("name", etNombre.text.toString())
                intent.putExtra("email", etEmail.text.toString())
                intent.putExtra("password", etPassword.text.toString())
                startActivity(intent)

            }else {


            }

        }

        btnBackLogin.setOnClickListener{

            startActivity(Intent(this, LoginActivity::class.java))

        }

    }
}