package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class JoinHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_home)

        val btnCancel : Button = findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener {
            startActivity(Intent(this, Configuration::class.java))
        }
    }
}