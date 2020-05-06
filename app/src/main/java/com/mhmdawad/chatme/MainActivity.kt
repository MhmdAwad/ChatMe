package com.mhmdawad.chatme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private lateinit var mCallbacks:PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private  var verificationID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneEditText = findViewById<EditText>(R.id.et_phone)
        val phoneButton = findViewById<Button>(R.id.bt_send)
        val verifyEditText = findViewById<EditText>(R.id.et_verify)
        val verifyButton = findViewById<Button>(R.id.bt_verify)

        userLogIn()

        verifyButton.setOnClickListener {
            verifyNumberWithCode(verificationID!!, verifyEditText.text.toString())
        }
        phoneButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString().trim()
            if (phoneNumber.length < 10 || phoneNumber.isEmpty()) {
                phoneEditText.error = "Enter a valid number"
                phoneEditText.requestFocus()
                return@setOnClickListener
            }
            verifyNumber(phoneNumber)
        }

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
                val code = p0?.smsCode
                if (code != null)
                    signInWithCredential(p0)

                Toast.makeText(applicationContext,"onVerificationCompleted",Toast.LENGTH_SHORT).show()
            }

            override fun onVerificationFailed(p0: FirebaseException?) {
                Toast.makeText(applicationContext,"onVerificationFailed",Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(p0, p1)
                verificationID = p0
                Toast.makeText(applicationContext,"onCodeSent",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyNumberWithCode(verificationID: String, code: String){
        val credential = PhoneAuthProvider.getCredential(verificationID,code)
        signInWithCredential(credential)

    }
    private fun  signInWithCredential(credential: PhoneAuthCredential){
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful)
                userLogIn()
        }
    }

    private fun userLogIn() {
        if(FirebaseAuth.getInstance().currentUser != null)
            Toast.makeText(this,"YES",Toast.LENGTH_SHORT).show()
    }

    private fun verifyNumber(number: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number,
            60,
            TimeUnit.SECONDS,
            TaskExecutors.MAIN_THREAD,
            mCallbacks
        )
    }
}
