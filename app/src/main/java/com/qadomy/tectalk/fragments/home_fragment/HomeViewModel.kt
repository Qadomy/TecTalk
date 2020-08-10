package com.qadomy.tectalk.fragments.home_fragment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.qadomy.tectalk.model.ChatParticipant
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.FireStoreUtil
import java.util.*

class HomeViewModel : ViewModel() {
    private var calledBefore = false

    init {
        getUserData()
    }

    val loggedUserMutableLiveData = MutableLiveData<User>()
    private val chatParticipantList: MutableList<ChatParticipant> by lazy { mutableListOf<ChatParticipant>() }
    private val chatParticipantsListMutableLiveData =
        MutableLiveData<MutableList<ChatParticipant>>()


    // function for get user chat history
    fun getChats(loggedUser: User): LiveData<MutableList<ChatParticipant>>? {

        /**
         * This method is called each time user document changes but
         * i want to attach listener only once so check with calledBefore
         * */
        if (calledBefore) {
            return chatParticipantsListMutableLiveData
        }

        calledBefore = true

        // set logged user id
        val loggedUserId = loggedUser.uid.toString()

        // create query for set collection for messages
        val query: Query = FireStoreUtil.firestoreInstance.collection("messages")
            .whereArrayContains("chat_members", loggedUserId)


        // set query to database firestore
        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException == null) {
                chatParticipantList.clear()

                if (!querySnapshot?.documents.isNullOrEmpty()) {
                    //user has chats , now get last message and receiver user
                    querySnapshot?.documents?.forEach { messageDocument ->
                        val chatParticipant = ChatParticipant()

                        //get last message & last message sender
                        val messagesList =
                            messageDocument.get("messages") as List<HashMap<String, Any>>?
                        val lastMessage = messagesList?.get(messagesList.size - 1)

                        //get message or photo url depending on last message type
                        val lastMessageType = lastMessage?.get("type") as Double?
                        chatParticipant.lastMessage = lastMessage?.get("text") as String?
                        chatParticipant.lastMessageType = lastMessageType
                        chatParticipant.lastMessageDate =
                            lastMessage?.get("created_at") as HashMap<String, Double>?
//                        println("HomeViewModel.getChats:${chatParticipant.lastMessageDate?.get("seconds")}")
                        Log.d(
                            TAG,
                            "getChats: HomeViewModel.getChats:${chatParticipant.lastMessageDate?.get(
                                "seconds"
                            )}"
                        )
                        val lastMessageOwnerId = lastMessage?.get("from") as String?


                        //set isLoggedUser to know if logged user typed last message or not
                        chatParticipant.isLoggedUser = (lastMessageOwnerId == loggedUserId)

                        //get other chat participant id and use it to get his information
                        if (lastMessageOwnerId == loggedUserId) {
                            val recipient = lastMessage?.get("to") as String?
                            if (recipient != null) {
                                FireStoreUtil.firestoreInstance.collection("users")
                                    .document(recipient).get()
                                    .addOnSuccessListener {
                                        FireStoreUtil.firestoreInstance.collection("users")
                                            .document(recipient).get().addOnSuccessListener {
                                                val participant = it.toObject(User::class.java)
                                                chatParticipant.participant = participant
                                                chatParticipantList.add(chatParticipant)
                                                chatParticipantsListMutableLiveData.value =
                                                    chatParticipantList

                                            }.addOnFailureListener {
                                                Log.d(
                                                    TAG,
                                                    "getChats: addOnFailureListener: ${it.message}"
                                                )
                                            }
                                    }
                            }
                        } else {
                            val sender = lastMessage?.get("from") as String?
                            if (sender != null) {
                                FireStoreUtil.firestoreInstance.collection("users")
                                    .document(sender).get()
                                    .addOnSuccessListener {
                                        FireStoreUtil.firestoreInstance.collection("users")
                                            .document(sender).get().addOnSuccessListener {
                                                val participant = it.toObject(User::class.java)
                                                chatParticipant.participant = participant
                                                chatParticipantList.add(chatParticipant)
                                                chatParticipantsListMutableLiveData.value =
                                                    chatParticipantList

                                            }.addOnFailureListener {
                                                Log.d(
                                                    TAG,
                                                    "getChats: addOnFailureListener: ${it.message}"
                                                )
                                            }
                                    }
                            }
                        }
                    }

                } else {
                    //user has no chats
                    chatParticipantsListMutableLiveData.value = null
                }

            } else {
                /** there is an error */
//                println("HomeViewModel.getChats:${firebaseFirestoreException.message}")
                Log.d(
                    TAG,
                    "getChats Error: HomeViewModel.getChats:${firebaseFirestoreException.message}"
                )
                chatParticipantsListMutableLiveData.value = null
            }
        }


        return chatParticipantsListMutableLiveData
    }


    // function for get user data from firestore database
    private fun getUserData() {
        FireStoreUtil.firestoreInstance.collection("users").document(AuthUtil.getAuthId())
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException == null) {
                    /**
                     * toObject: returns the contents of the document converted to a POJO or {@code null} if the document
                     * doesn't exist.
                     */
                    val loggedUser = documentSnapshot?.toObject(User::class.java)
                    if (loggedUser != null) {
                        loggedUserMutableLiveData.value = loggedUser
                    }
                } else {
//                    println("HomeViewModel.getUserData:${firebaseFirestoreException.message}")
                    Log.d(
                        TAG,
                        "getUserData: HomeViewModel.getUserData:${firebaseFirestoreException.message}"
                    )
                }
            }
    }


    companion object {
        private const val TAG = "HomeViewModel"
    }
}