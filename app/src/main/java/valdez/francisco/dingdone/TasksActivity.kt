package valdez.francisco.dingdone

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class TasksActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val userViewModel: UserViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.itemIconTintList = null

        if (savedInstanceState == null) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                userViewModel.loadUserHomes(userId)
                userViewModel.userHomes.observe(this) { homes ->
                    if (homes.isEmpty()) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, ConfigurationFragment())
                            .commit()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, TasksFragmentNew())
                            .commit()
                    }
                }
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TasksFragmentNew())
                    .commit()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnNav_tasks -> {
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TasksFragmentNew())
                        .commit()
                    true
                }
                R.id.btnNavGraphs -> {
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, GraphsFragment())
                        .commit()
                    true
                }
                R.id.btnNav_config -> {
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ConfigurationFragment())
                        .commit()
                    true
                }
                R.id.btnNav_profile -> {
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
