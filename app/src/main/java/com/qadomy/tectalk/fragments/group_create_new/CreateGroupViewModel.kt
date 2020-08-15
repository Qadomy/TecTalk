package com.qadomy.tectalk.fragments.group_create_new

import android.util.Log.d
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.qadomy.tectalk.model.GroupName
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.LoadState

class CreateGroupViewModel : ViewModel() {
    //    val navigateToHomeMutableLiveData = MutableLiveData<Boolean?>()
    val loadingState = MutableLiveData<LoadState>()
    val loggedUserMutableLiveData = MutableLiveData<User>()
    val createdGroupFlag = MutableLiveData<Boolean>()

    //    private val groupCollectionReference = FireStoreUtil.firestoreInstance.collection("messages")
    private var userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
        FireStoreUtil.firestoreInstance.collection("users").document(it)
    }

    init {
        getUserData()
    }

    private fun getUserData() {
        FireStoreUtil.firestoreInstance.collection("users").document(AuthUtil.getAuthId())
            .addSnapshotListener { documentSnapshot, firebaseFireStoreException ->
                if (firebaseFireStoreException == null) {
                    val loggedUser = documentSnapshot?.toObject(User::class.java)
                    if (loggedUser != null) {
                        loggedUserMutableLiveData.value = loggedUser
                    }
                } else {
                    d(TAG, "getUserData: ${firebaseFireStoreException.message}")
                }
            }
    }

    private fun updateUserProfileForGroups(groupName: String) {
        userDocRef?.update(
            "groups_in",
            FieldValue.arrayUnion(groupName, groupName)
        )
            ?.addOnSuccessListener {
                // bioLoadState.value = LoadState.SUCCESS
                createdGroupFlag.value = true
                d(TAG, "Added group in user successfully")

            }
            ?.addOnFailureListener {
                // bioLoadState.value = LoadState.FAILURE
                d(TAG, "Added group in user failure")
            }
    }

    fun createGroup(
        user: User,
        groupName: GroupName
    ) {
        val db = FireStoreUtil.firestoreInstance
        groupName.group_name?.let { name ->
            db.collection("groups").document(name).set(groupName).addOnSuccessListener {
                d(TAG, "Created group successfully")
                updateUserProfileForGroups(groupName.group_name.toString())
            }.addOnFailureListener {
                loadingState.value = LoadState.FAILURE
                ErrorMessage.errorMessage = it.message
            }
        }

        d(TAG, "Yes created room")
    }


    companion object {
        private const val TAG = "CreateGroupViewModel"
    }
}