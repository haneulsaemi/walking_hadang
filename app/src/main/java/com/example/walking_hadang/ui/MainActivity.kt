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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.ActivityMainBinding
import com.example.walking_hadang.ui.account.ProfileActivity
import com.example.walking_hadang.ui.store.StoreFragment

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
        // ① 시스템바 공간을 우리가 처리 (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ② Toolbar 세팅
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // ③ 상태바 인셋 → AppBar 상단 패딩으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (top > 0) v.updatePadding(top = top)   // <- 상단 패딩은 여기서만
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = bottom)
            insets
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, HomeFragment())
            .commit()

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_recoding -> switchFragment(RecodingFragment())
                R.id.nav_market -> switchFragment(StoreFragment())
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

    override fun onResume() {
        super.onResume()
    }
}