package com.qadomy.tectalk.utils

import com.google.firebase.auth.FirebaseAuth

object AuthUtil {

    val firebaseAuthInstance: FirebaseAuth by lazy {
        println("firebaseAuthInstance.:")
        FirebaseAuth.getInstance()
    }


    // get current authenticated user id
    fun getAuthId(): String {
        return firebaseAuthInstance.currentUser?.uid.toString()
    }

}
