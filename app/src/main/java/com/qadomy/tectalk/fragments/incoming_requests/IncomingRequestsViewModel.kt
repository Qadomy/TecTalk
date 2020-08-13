package com.qadomy.tectalk.fragments.incoming_requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.qadomy.tectalk.fragments.different_user_profile.DifferentUserProfileViewModel.Companion.RECEIVED_REQUEST_ARRAY
import com.qadomy.tectalk.fragments.different_user_profile.DifferentUserProfileViewModel.Companion.SENT_REQUEST_ARRAY
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.Common.FRIENDS
import com.qadomy.tectalk.utils.FireStoreUtil

class IncomingRequestsViewModel : ViewModel() {

    private val usersRef = FireStoreUtil.firestoreInstance.collection("users")
    private val friendRequesterMutableLiveData = MutableLiveData<MutableList<User>?>()

    //get info of the users that sent friend requests
    fun downloadRequests(receivedRequests: List<String>): LiveData<MutableList<User>?> {

        val friendRequester = mutableListOf<User>()

        for (receivedRequest in receivedRequests) {
            usersRef.document(receivedRequest).get().addOnSuccessListener {
                val user = it?.toObject(User::class.java)
                user?.let { users -> friendRequester.add(users) }
                friendRequesterMutableLiveData.value = friendRequester

            }.addOnFailureListener {
                friendRequesterMutableLiveData.value = null
            }
        }
        return friendRequesterMutableLiveData
    }


    fun addToFriends(
        requesterId: String,
        loggedUserId: String
    ) {

        deleteRequest(requesterId, loggedUserId)

        //add id in sentRequest array for logged in user


        FireStoreUtil.firestoreInstance.collection("users").document(requesterId)
            .update(FRIENDS, FieldValue.arrayUnion(loggedUserId)).addOnSuccessListener {
                //add loggedInUserId in receivedRequest array for other user
                FireStoreUtil.firestoreInstance.collection("users").document(loggedUserId)
                    .update(FRIENDS, FieldValue.arrayUnion(requesterId))
                    .addOnSuccessListener {

                    }.addOnFailureListener {

                    }
            }.addOnFailureListener {

            }
    }


    fun deleteRequest(
        requesterId: String,
        loggedUserId: String
    ) {

        //remove id from sentRequest array for logged in user
        FireStoreUtil.firestoreInstance.collection("users").document(loggedUserId)
            .update(RECEIVED_REQUEST_ARRAY, FieldValue.arrayRemove(requesterId))
            .addOnSuccessListener {
                //remove loggedInUserId from receivedRequest array for other user
                FireStoreUtil.firestoreInstance.collection("users").document(requesterId)
                    .update(
                        SENT_REQUEST_ARRAY,
                        FieldValue.arrayRemove(loggedUserId)
                    )
                    .addOnSuccessListener {

                    }.addOnFailureListener {

                    }
            }.addOnFailureListener {

            }
    }

}