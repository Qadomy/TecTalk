package com.qadomy.tectalk.fragments.find_user

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.FRIENDS
import com.qadomy.tectalk.utils.FireStoreUtil

class FindUserViewModel : ViewModel() {

    private val userDocumentsMutableLiveData = MutableLiveData<MutableList<User?>>()

    fun loadUsers(): LiveData<MutableList<User?>> {


        val docRef = FireStoreUtil.firestoreInstance.collection("users")
        docRef.get()
            .addOnSuccessListener {
                //add any user that isn't logged in user to result
                val result = mutableListOf<User?>()
                for (document in it.documents) {
                    if (document.get("uid").toString() != AuthUtil.getAuthId()) {
                        val user = document.toObject(User::class.java)
                        result.add(user)
                    }

                }


                // remove friends of logged in user from result list
                docRef.whereArrayContains(FRIENDS, AuthUtil.getAuthId())
                    .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        if (firebaseFirestoreException == null) {
                            val documents = querySnapshot?.documents
                            if (documents != null) {
                                for (document in documents) {
                                    val user = document.toObject(User::class.java)
                                    result.remove(user)

                                }
                                userDocumentsMutableLiveData.value = result

                            }
                        } else {
                            userDocumentsMutableLiveData.value = null
                        }
                    }


            }
            .addOnFailureListener {
                userDocumentsMutableLiveData.value = null
                Log.e(TAG, "addOnFailureListener: ${it.message}")
            }

        return userDocumentsMutableLiveData
    }

    companion object {
        private const val TAG = "FindUserViewModel"
    }
}