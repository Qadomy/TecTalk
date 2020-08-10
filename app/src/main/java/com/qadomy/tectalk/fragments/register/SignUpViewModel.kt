package com.qadomy.tectalk.fragments.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.LoadState

class SignUpViewModel : ViewModel() {
    val navigateToHomeMutableLiveData = MutableLiveData<Boolean?>()
    val loadingState = MutableLiveData<LoadState>()


    // function for create new user in firebase auth
    fun registerEmail(
        auth: FirebaseAuth,
        email: String,
        password: String,
        username: String
    ) {

        loadingState.value = LoadState.LOADING
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                storeUserInFirestore(User(it.user?.uid, username, email))
            }.addOnFailureListener {
                ErrorMessage.errorMessage = it.message
                loadingState.value = LoadState.FAILURE
            }

    }

    // function for store new account in firestore
    private fun storeUserInFirestore(user: User) {
        val db = FireStoreUtil.firestoreInstance
        user.uid?.let {
            db.collection("users").document(it).set(user).addOnSuccessListener {
                navigateToHomeMutableLiveData.value = true
            }.addOnFailureListener {
                loadingState.value = LoadState.FAILURE
                ErrorMessage.errorMessage = it.message
            }
        }
    }

    fun doneNavigating() {
        navigateToHomeMutableLiveData.value = null
    }
}