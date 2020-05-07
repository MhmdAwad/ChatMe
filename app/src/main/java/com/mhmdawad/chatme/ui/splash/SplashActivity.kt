package com.mhmdawad.chatme.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.ui.main.MainActivity


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            val mainIntent = Intent(applicationContext, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, 2000)
    }

//    private fun checkLogged(){
//        val currentUser = mAuth.currentUser
//        if(currentUser!=null){
//            Toast.makeText(applicationContext, "Login successfully..", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(applicationContext, MainPageActivity::class.java))
//            finish()
//        }
//    }

}
