package com.mhmdawad.chatme.ui.activities.login

import android.content.Intent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.ui.activities.main_page.MainPageActivity
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {


    private lateinit var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var verificationID: String? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var phoneEditText: EditText
    private lateinit var phoneButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        phoneEditText = findViewById(R.id.loginEditText)
        phoneButton = findViewById(R.id.loginButton)
        mAuth = FirebaseAuth.getInstance()

        userLogIn()
        phoneButton.setOnClickListener {
            if (phoneButton.text.toString().toLowerCase() == "submit") {
                val phoneNumber = phoneEditText.text.toString().trim()
                if (phoneNumber.length < 10) {
                    phoneEditText.error = "Enter a valid number"
                    phoneEditText.requestFocus()
                    return@setOnClickListener
                }
                phoneButton.text = "Verify"
                verifyNumber(phoneNumber)
            }else{
                verifyNumberWithCode(verificationID!!, phoneEditText.text.toString())
            }

        }

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
                val code = p0?.smsCode
                if (code != null)
                    signInWithCredential(p0)
            }

            override fun onVerificationFailed(p0: FirebaseException?) {
                Toast.makeText(applicationContext, "An error occurred, Please try again.", Toast.LENGTH_SHORT)
                    .show()
                phoneEditText.hint = "Enter phone number"
                phoneButton.text = "submit"
            }

            override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(p0, p1)
                verificationID = p0
                phoneEditText.text.clear()
                phoneEditText.hint = "Enter verification code"
            }
        }
    }

    private fun verifyNumberWithCode(verificationID: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationID, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val currentUser = mAuth.currentUser!!
                val userDB = FirebaseDatabase.getInstance().reference
                    .child("Users").child(currentUser.uid)
                userDB.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(p0: DataSnapshot) {
                        val user: HashMap<String, Any> = HashMap()
                        user["Phone"] = currentUser.phoneNumber!!
                        user["Uid"] = mAuth.uid!!
                        user["Image"] = ""
                        user["Status"] = "I am using my WhatsApp :)"
                        userDB.updateChildren(user)
                    }
                })
                userLogIn()
            }
        }
    }

    private fun userLogIn() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(applicationContext, MainPageActivity::class.java))
            finish()
        }
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