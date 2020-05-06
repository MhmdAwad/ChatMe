package com.mhmdawad.chatme

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val login: Button = findViewById(R.id.login)
        val userName: EditText = findViewById(R.id.userName)
        val password: EditText = findViewById(R.id.password)
        mAuth = FirebaseAuth.getInstance()
        checkLogged()
        login.setOnClickListener {
            mAuth.signInWithEmailAndPassword(userName.text.toString(), password.text.toString())
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        checkLogged()
                    }else
                        Toast.makeText(applicationContext, "An error occurred..", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkLogged(){
        val currentUser = mAuth.currentUser
        if(currentUser!=null){
            Toast.makeText(applicationContext, "Login successfully..", Toast.LENGTH_SHORT).show()
            startActivity(Intent(applicationContext, MainPageActivity::class.java))
            finish()
        }
    }

}
