package com.example.walking_hadang

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.walking_hadang.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setTitle("워킹하당")
        binding.toolbar.setTitleTextColor(Color.parseColor("#ffffff"))


        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, HomeFragment())
            .commit()

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_recoding -> switchFragment(RecodingFragment())
                R.id.nav_map -> switchFragment(MapFragment())
                R.id.nav_community -> switchFragment(CommunityFragment())
            }
            true
        }
    }

    fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.menu_login -> {
                MyApplication.auth.signOut()
                MyApplication.email = null

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }
//            R.id.menu_alarm_settings ->{
//                switchFragment(AlarmSettingsFragment())
//                true
//            }
//            R.id.menu_setting ->{
//                val intent = Intent(this, SettingActivity::class.java)
//                startActivity(intent)
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item = menu?.findItem(R.id.menu_login)
        val user = menu?.findItem(R.id.menu_user)

        val currentUser = MyApplication.auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            MyApplication.email = currentUser.email
            item?.title = "Logout"
            user?.isVisible = true
            user?.title = MyApplication.email
        }
        else {
            item?.title = "Login"
            user?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
    }
}