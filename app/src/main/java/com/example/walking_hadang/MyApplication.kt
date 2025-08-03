package com.example.walking_hadang


import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.multidex.MultiDexApplication
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : MultiDexApplication() {
    companion object{
        lateinit var auth : FirebaseAuth
        lateinit var db : FirebaseFirestore

        var email:String? = null
        fun checkAuth() : Boolean{
            val currentUser = auth.currentUser

            return currentUser?.let{
                email = currentUser.email
                if(currentUser.isEmailVerified){
                    true
                }else{
                    false
                }
            }?:let {
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // EmojiCompat 초기화 추가
        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
    }
}