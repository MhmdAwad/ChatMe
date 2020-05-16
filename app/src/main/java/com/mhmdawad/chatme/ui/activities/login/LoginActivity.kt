package com.mhmdawad.chatme.ui.activities.login


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.ActivityLoginBinding
import com.mhmdawad.chatme.ui.activities.main_page.MainPageActivity

class LoginActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityLoginBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_login)

        val loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        binding.loginVM = loginViewModel

        loginViewModel.checkUserLogin().observe(this, Observer {
            Log.d("What", "$it")
            if (it) {
                startActivity(Intent(applicationContext, MainPageActivity::class.java))
                finish()
            }
        })

        loginViewModel.toastMessagesLiveData().observe(this, Observer {
            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
        })

        binding.lifecycleOwner = this
    }


}