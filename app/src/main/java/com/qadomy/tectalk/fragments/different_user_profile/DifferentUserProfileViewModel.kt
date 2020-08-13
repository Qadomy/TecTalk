package com.qadomy.tectalk.fragments.different_user_profile

import android.app.Application
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Log.d
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.qadomy.tectalk.R
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.FRIENDS

class DifferentUserProfileViewModel(val app: Application) : AndroidViewModel(app) {

    var loadedImage = MutableLiveData<RequestBuilder<Drawable>>()

    enum class FriendRequestState { SENT, NOT_SENT, ALREADY_FRIENDS }

    private val friendRequestStateMutableLiveData = MutableLiveData<FriendRequestState>()


    // get document if logged in user and check if other user id is in the sentRequest list
    fun checkIfFriends(recipientId: String?): LiveData<FriendRequestState> {
        val db = FirebaseFirestore.getInstance()

        if (recipientId != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .addSnapshotListener { documentSnapshot, firebaseFireStoreException ->
                        if (firebaseFireStoreException == null) {
                            // get user
                            val user =
                                documentSnapshot?.toObject(User::class.java)

                            /** Check if users already friends */
                            val friendsList = user?.friends
                            // user has friends
                            if (!friendsList.isNullOrEmpty()) {
                                for (friendId in friendsList) {
                                    if (friendId == recipientId) {
                                        friendRequestStateMutableLiveData.value =
                                            FriendRequestState.ALREADY_FRIENDS
                                        return@addSnapshotListener
                                    }
                                }
                            }


                            /** Check if user send friends request to user */
                            val sentRequests = user?.sentRequests
                            if (sentRequests != null) {
                                for (sentRequest in sentRequests) {
                                    if (sentRequest == recipientId) {
                                        friendRequestStateMutableLiveData.value =
                                            FriendRequestState.SENT
                                        return@addSnapshotListener
                                    }
                                }
                                friendRequestStateMutableLiveData.value =
                                    FriendRequestState.NOT_SENT
                            }

                        } else {
                            Log.e(
                                TAG,
                                "checkIfFriends: firebaseFireStoreException-ERROR: ${firebaseFireStoreException.message}"
                            )
                        }
                    }
            }
        }

        return friendRequestStateMutableLiveData
    }

    // function for set image in image view using Glide
    fun downloadProfilePicture(profilePictureUrl: String?) {
        d(TAG, "downloadProfilePicture: $profilePictureUrl")

        if (profilePictureUrl == "null") return
        val load: RequestBuilder<Drawable> =
            Glide.with(app).load(profilePictureUrl).placeholder(R.drawable.anonymous_profile)
        loadedImage.value = load
    }

    fun updateSentRequestsForSender(uid: String?) {

        //add id in sentRequest array for logged in user
        val db = FirebaseFirestore.getInstance()
        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(SENT_REQUEST_ARRAY, FieldValue.arrayUnion(uid)).addOnSuccessListener {
                        //add loggedInUserId in receivedRequest array for other user
                        updateReceivedRequestsForReceiver(db, uid, AuthUtil.getAuthId())
                    }.addOnFailureListener { e ->
                        throw e
                    }
            }
        }
    }

    private fun updateReceivedRequestsForReceiver(
        db: FirebaseFirestore,
        uid: String,
        authId: String
    ) {
        db.collection("users").document(uid)
            .update(RECEIVED_REQUEST_ARRAY, FieldValue.arrayUnion(authId))
            .addOnSuccessListener {
            }.addOnFailureListener {
                throw it
            }
    }

    // function for cancel friend request from database
    fun cancelFriendRequest(uid: String?) {
        //remove id from sentRequest array for logged in user
        val db = FirebaseFirestore.getInstance()

        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(SENT_REQUEST_ARRAY, FieldValue.arrayRemove(uid)).addOnSuccessListener {
                        //remove loggedInUserId from receivedRequest array for other user
                        db.collection("users").document(uid)
                            .update(
                                RECEIVED_REQUEST_ARRAY,
                                FieldValue.arrayRemove(AuthUtil.getAuthId())
                            )
                            .addOnSuccessListener { success ->
                                d(TAG, "cancelFriendRequest: addOnSuccessListener: $success")
                            }.addOnFailureListener { ex ->
                                d(
                                    TAG,
                                    "cancelFriendRequest: addOnFailureListener-1 -> ${ex.message}"
                                )
                            }
                    }.addOnFailureListener { e ->
                        d(TAG, "cancelFriendRequest: addOnFailureListener-2 -> ${e.message}")
                    }
            }
        }
    }

    // function for remove friend request from database
    fun removeFromFriends(uid: String?) {
        //remove id from sentRequest array for logged in user
        val db = FirebaseFirestore.getInstance()

        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(FRIENDS, FieldValue.arrayRemove(uid)).addOnSuccessListener {
                        //remove loggedInUserId from receivedRequest array for other user
                        db.collection("users").document(uid)
                            .update(
                                FRIENDS,
                                FieldValue.arrayRemove(AuthUtil.getAuthId())
                            )
                            .addOnSuccessListener { success ->
                                d(TAG, "cancelFriendRequest: addOnSuccessListener: $success")
                            }.addOnFailureListener { ex ->
                                d(
                                    TAG,
                                    "cancelFriendRequest: addOnFailureListener-1 -> ${ex.message}"
                                )
                            }
                    }.addOnFailureListener { e ->
                        d(TAG, "cancelFriendRequest: addOnFailureListener-2 -> ${e.message}")
                    }
            }
        }
    }


    companion object {
        private const val TAG = "DifferentUserProfileVie"
        const val SENT_REQUEST_ARRAY = "sentRequests"
        const val RECEIVED_REQUEST_ARRAY = "receivedRequests"
    }

}

