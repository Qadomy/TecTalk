package com.qadomy.tectalk.fragments.home_group

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.qadomy.tectalk.model.GroupName
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.FireStoreUtil

class GroupViewModel : ViewModel() {

    init {
        getUserData()
    }

    private val messageCollectionReference = FireStoreUtil.firestoreInstance.collection("messages")

    private var calledBefore = false

    val loggedUserMutableLiveData = MutableLiveData<User>()

    private val groupList = MutableLiveData<MutableList<GroupName>>()
    private val groupParticipantList: MutableList<GroupName> by lazy { mutableListOf<GroupName>() }


    //
    fun getRooms(loggedUser: User): MutableLiveData<MutableList<GroupName>> {

        //this method is called each time user document changes but i want to attach listener only once so check with calledBefore
        if (calledBefore) {
            return groupList
        }

        calledBefore = true

        val loggedUserId = loggedUser.uid.toString()

        val query: Query = FireStoreUtil.firestoreInstance.collection("groups")
            .whereArrayContains("chat_members_in_group", loggedUserId)

        query.addSnapshotListener { querySnapshot, firebaseFireStoreException ->
            if (firebaseFireStoreException == null) {
                groupParticipantList.clear()

                if (!querySnapshot?.documents.isNullOrEmpty()) {
                    querySnapshot?.documents?.forEach { document ->
                        val groupName = GroupName()
                        groupName.group_name = document.get("group_name") as String?
                        groupName.imageurl = document.get("imageurl") as String?
                        groupName.description = document.get("description") as String?
                        groupName.chat_members_in_group =
                            document.get("chat_members_in_group") as List<String>?
                        groupParticipantList.add(groupName)
                        groupList.value = groupParticipantList
                    }
                } else {
                    //user has no chats
                    groupList.value = null
                }
            } else {
                //error
                Log.d(TAG, "getRooms: ${firebaseFireStoreException.message}")
                groupList.value = null
            }
        }
        return groupList
    }


    fun createRoom(name: String) {
        messageCollectionReference.document(name)
            .update(
                "chat_members_in_group",
                FieldValue.arrayUnion(name, name)
            )
        print("Yes created room")
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
                    println("HomeViewModel.getUserData:${firebaseFireStoreException.message}")
                }
            }
    }

    companion object {
        private const val TAG = "GroupViewModel"
    }
}