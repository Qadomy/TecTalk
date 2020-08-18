package com.qadomy.tectalk.fragments.group_add_members

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.MESSAGES_COLLECTIONS
import com.qadomy.tectalk.utils.Common.USERS_COLLECTIONS
import com.qadomy.tectalk.utils.FireStoreUtil

class GroupAddMembersViewModel : ViewModel() {

//    var calledBefore = false
    private val loggedUserMutableLiveData = MutableLiveData<User>()
    private val messageCollectionReference = FireStoreUtil.firestoreInstance.collection(
        MESSAGES_COLLECTIONS
    )

    init {
        getUserData()
    }

    fun updateUserProfileForGroups(groupName: String, members: ArrayList<String>) {

        messageCollectionReference.document(groupName).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    //this node exists send your message
                    messageCollectionReference.document(groupName)
                        .update("chat_members_in_group", members)

                }


                ////////////////////////////////////////////////////////////////////////////////////////////////////
                val userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
                    FireStoreUtil.firestoreInstance.collection(USERS_COLLECTIONS).document(it)
                }
                userDocRef?.update(
                    "groups_in",
                    FieldValue.arrayUnion(groupName, groupName)
                )
                    ?.addOnSuccessListener {
                        // bioLoadState.value = LoadState.SUCCESS
                        Log.d(TAG, "added group in user successfully")
                        Log.d(TAG, "updateUserProfileForGroups: $it")
                    }
                    ?.addOnFailureListener {
                        // bioLoadState.value = LoadState.FAILURE
                        Log.d(TAG, "added group in user failure")
                    }
            }

    }

    private fun getUserData() {
        FireStoreUtil.firestoreInstance.collection(USERS_COLLECTIONS).document(AuthUtil.getAuthId())
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException == null) {
                    val loggedUser = documentSnapshot?.toObject(User::class.java)
                    if (loggedUser != null) {
                        loggedUserMutableLiveData.value = loggedUser
                    }
                } else {
                    Log.d(TAG, "getUserData: ${firebaseFirestoreException.message}")
                }
            }
    }


    companion object {
        private const val TAG = "GroupAddMembersViewMode"
    }
}