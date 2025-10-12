package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeCreated : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_created)

        val btnGoHome :Button = findViewById(R.id.btnGoHome)
        val tvInvitationCode : TextView = findViewById(R.id.tv_codeCreated)

        val codigo = intent.getStringExtra("invitationCode")
        tvInvitationCode.text = codigo ?: ""

        btnGoHome.setOnClickListener {
            //Cambiar que en vez del mainActivity te mande a las tasks directamente
            startActivity(Intent(this, Configuration::class.java))
        }
    }
}