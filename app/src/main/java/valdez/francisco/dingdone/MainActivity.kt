package valdez.francisco.dingdone

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        var nombre = intent.getStringExtra("name") ?: ""
        var email = intent.getStringExtra("email") ?: ""
        var password = intent.getStringExtra("password") ?: ""

        var tvNombre : TextView = findViewById(R.id.tvNombre)
        var tvEmial : TextView = findViewById(R.id.tvEmail)
        var tvPassword : TextView = findViewById(R.id.tvPassword)
        tvNombre.text = nombre
        tvEmial.text = email
        tvPassword.text = password

    }
}