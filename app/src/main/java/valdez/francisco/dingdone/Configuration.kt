package valdez.francisco.dingdone

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Configuration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to TasksActivity
        val intent = Intent(this, TasksActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}