package com.mhmdawad.chatme.ui.activities.login


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class LoginViewModel : ViewModel() {


    var editTextValue = MutableLiveData<String>().apply {
        value = ""
    }
    var editTextHint = MutableLiveData<String>().apply {
        value = "Enter phone number"
    }
    var progressbarVisibility = MutableLiveData<Int>().apply {
        value = 8
    }

    private var toastMsg = MutableLiveData<String>()
    private var userLogin = MutableLiveData<Boolean>()
    var buttonText = MutableLiveData<String>().apply {
        value = "Submit"
    }
    private val mAuth = FirebaseAuth.getInstance()
    private var verificationID: String? = null

    fun toastMessagesLiveData(): LiveData<String> = toastMsg
    fun checkUserLogin(): LiveData<Boolean> {
        userLogIn()
        return userLogin
    }

    fun login() {
        if (buttonText.value == "Verify") {
            verifyNumberWithCode()
        } else {
            val phoneNumber = editTextValue.value!!.trim()
            if (phoneNumber.length < 10) {
                toastMsg.value = "Enter a valid number"
                return
            }
            verifyNumber()
        }
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
            } else {
                toastMsg.value = "Verification code is wrong, Please try again."
                stopLoading("Verify")
            }

        }
    }

    private fun loadingView(){
        progressbarVisibility.value = 0
        buttonText.value = ""
    }
    private fun stopLoading(text: String){
        progressbarVisibility.value = 8
        buttonText.value = text
    }

    private fun verifyNumberWithCode() {
        if(editTextValue.value!!.isEmpty()){
            toastMsg.value = "type verification code!"
            return
        }

        loadingView()
        val credential =
            PhoneAuthProvider.getCredential(verificationID!!, editTextValue.value.toString())
        signInWithCredential(credential)
    }


    private fun userLogIn() {
        userLogin.value = FirebaseAuth.getInstance().currentUser != null
    }

    private fun verifyNumber() {
        loadingView()
        buttonText.value = ""
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            editTextValue.value.toString(),
            60,
            TimeUnit.SECONDS,
            TaskExecutors.MAIN_THREAD,
            loginCallback()
        )
    }

    private fun loginCallback(): PhoneAuthProvider.OnVerificationStateChangedCallbacks {
        return object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
                val code = p0?.smsCode
                if (code != null)
                    signInWithCredential(p0)
            }

            override fun onVerificationFailed(p0: FirebaseException?) {
                toastMsg.value = "An error occurred, Please try again."
                editTextHint.value = "Enter phone number"
                stopLoading("submit")
            }

            override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(p0, p1)
                verificationID = p0
                editTextValue.value = ""
                editTextHint.value = "Enter verification code"
                stopLoading("Verify")
            }
        }
    }

}