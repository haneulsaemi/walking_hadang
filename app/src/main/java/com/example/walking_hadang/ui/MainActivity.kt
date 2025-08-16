package com.example.walking_hadang.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.ActivityMainBinding
import com.example.walking_hadang.ui.account.ProfileActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding

    override fun onStart() {
        super.onStart()
        if (!MyApplication.checkAuth()) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
            return
        }
        invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val toolbar = binding.toolbar.toolbar
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.findViewById<TextView>(R.id.toolbarTitle).text = ""


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
        inflater.inflate(R.menu.menu_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_profile ->{
//                showProfilePopup()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun showProfilePopup() {
        val anchorView = findViewById<View>(R.id.menu_profile)
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_profile_popup, popup.menu)

        val currentUser = MyApplication.auth.currentUser
        val isLoggedIn = currentUser != null

        popup.menu.findItem(R.id.menu_login).isVisible = !isLoggedIn
        popup.menu.findItem(R.id.menu_logout).isVisible = isLoggedIn

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_login -> {
                    // 로그인 화면 이동 등
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    MyApplication.auth.signOut()
                    Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show()
                    if (!MyApplication.checkAuth()) {
                        startActivity(
                            Intent(this, LoginActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        )
                        finish()
                    }
                    invalidateOptionsMenu() // 메뉴 갱신
                    true
                }
                else -> false
            }
        }

        popup.show()
    }



    override fun onResume() {
        super.onResume()
    }
}