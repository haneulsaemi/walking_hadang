package com.example.walking_hadang.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.ActivityEditFieldBinding

class EditFieldActivity : AppCompatActivity() {
    lateinit var binding : ActivityEditFieldBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val key = intent.getStringExtra("key") ?: ""
        val label = intent.getStringExtra("label") ?: ""
        val value = intent.getStringExtra("value") ?: ""
        val inputType = intent.getIntExtra("inputType", InputType.TYPE_CLASS_TEXT)

        binding.tvTitle.text = label
        val et = binding.etValue
        et.setText(value)
        et.inputType = inputType

        binding.btnSave.setOnClickListener {
            val newValue = et.text.toString().trim()
            val data = Intent().apply {
                putExtra("key", key)
                putExtra("value", newValue)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        binding.btnClose.setOnClickListener { finish() }
    }
}