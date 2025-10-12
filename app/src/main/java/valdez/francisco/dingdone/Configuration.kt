package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Configuration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        var tvHome: TextView = findViewById(R.id.tvHome)

        val btnCreateHome : Button = findViewById(R.id.btnCreateHome)
        val btnJoinHome : Button = findViewById(R.id.btnJoinHome)

        btnCreateHome.setOnClickListener{
            startActivity(Intent(this, CreateHome::class.java))
        }

        btnJoinHome.setOnClickListener {
            startActivity(Intent(this, JoinHome::class.java))
        }
    }
}