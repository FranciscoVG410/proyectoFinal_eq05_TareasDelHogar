package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random

class CreateHome : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_home)

        var etHomeName : EditText = findViewById(R.id.et_homeName)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val btnCreate: Button = findViewById(R.id.btnCreate)
        val tvRandomCode: TextView = findViewById(R.id.tv_randomCode)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        tvRandomCode.text = generateRandomCode()


        btnCreate.setOnClickListener {
            if (etHomeName.text.toString().isEmpty()) {
                etHomeName.setBackgroundResource(R.drawable.error_rounded_edit_text)
            } else {
                val code = tvRandomCode.text.toString()
                val intent = Intent(this, HomeCreated::class.java)
                intent.putExtra("invitationCode", code)
                startActivity(intent)
            }
        }

        btnCancel.setOnClickListener {
            startActivity(Intent(this, Configuration::class.java))
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnNav_tasks -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.btnCreateAccount, TasksFragment())
                        .commit()

                    true
                }
                R.id.btnNav_config -> {
                    startActivity(Intent(this, HomeConfiguration::class.java))
                    true
                }
                else -> false
            }
        }
    }
    private fun generateRandomCode(): String {
        val letters = (1..3)
            .map { ('A'..'Z').random() }
            .joinToString("")
        val numbers = (1..3)
            .map { Random.nextInt(0, 9) }
            .joinToString("")
        return letters + numbers
    }
}