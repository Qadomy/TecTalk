package com.qadomy.tectalk.fragments.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.LoadState
import java.util.regex.Matcher
import java.util.regex.Pattern

class LoginViewModel : ViewModel() {
    private val loadingState = MutableLiveData<LoadState>()
    private val emailMatch = MutableLiveData<Boolean>()
    private val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$"


    // function for check validate of email formatted
    fun isEmailFormatCorrect(it: String): LiveData<Boolean> {

        val pattern: Pattern = Pattern.compile(emailRegex)
        val matcher: Matcher = pattern.matcher(it)
        emailMatch.value = matcher.matches()

        return emailMatch
    }


    // function for sign in to firebase
    fun login(auth: FirebaseAuth, email: String, password: String): LiveData<LoadState> {
        loadingState.value = LoadState.LOADING

        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            loadingState.value = LoadState.SUCCESS
        }.addOnFailureListener {
            ErrorMessage.errorMessage = it.message
            loadingState.value = LoadState.FAILURE
        }
        return loadingState
    }

    fun doneNavigating() {
        loadingState.value = null
    }
}