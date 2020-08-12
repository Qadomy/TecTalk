package com.qadomy.tectalk.ui.main_activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.FireStoreUtil

class SharedViewModel : ViewModel() {

    private var friendsListMutableLiveData =
        MutableLiveData<List<User>>()
    private var usersCollectionRef: CollectionReference =
        FireStoreUtil.firestoreInstance.collection("users")


    // function for get freinds list from firestore database in firebase
    fun loadFriends(loggedUser: User): MutableLiveData<List<User>> {

        val friendsIds = loggedUser.friends
        if (!friendsIds.isNullOrEmpty()) {
            val mFriendList = mutableListOf<User>()
            for (friendId in friendsIds) {
                usersCollectionRef.document(friendId).get()
                    .addOnSuccessListener { friendUser ->
                        val friend =
                            friendUser.toObject(User::class.java)
                        friend?.let { user -> mFriendList.add(user) }
                        friendsListMutableLiveData.value = mFriendList
                    }
            }
        } else {
            //user has no friends
            friendsListMutableLiveData.value = null
        }

        return friendsListMutableLiveData
    }
}