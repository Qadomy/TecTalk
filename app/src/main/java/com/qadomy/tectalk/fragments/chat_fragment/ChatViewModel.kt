package com.qadomy.tectalk.fragments.chat_fragment

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qadomy.tectalk.model.*
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.StorageUtil
import java.io.File
import java.util.*

class ChatViewModel(val senderId: String?, val receiverId: String) : ViewModel() {

    private lateinit var mStorageRef: StorageReference
    private val messageCollectionReference = FireStoreUtil.firestoreInstance.collection("messages")
    private val messagesList: MutableList<Message> by lazy { mutableListOf<Message>() }
    private val chatFileMapMutableLiveData = MutableLiveData<Map<String, Any?>>()
    private val messagesMutableLiveData = MutableLiveData<List<Message>>()
    private val chatImageDownloadUriMutableLiveData = MutableLiveData<Uri>()
    val chatRecordDownloadUriMutableLiveData = MutableLiveData<Uri>()


    // function for upload record voice to storage firebase
    fun uploadRecord(filePath: String) {

        // get instance to storage firebase
        mStorageRef = StorageUtil.storageInstance.reference

        // set reference in storage firebase
        val ref = mStorageRef.child("records/" + Date().time)
        val uploadTask = ref.putFile(Uri.fromFile(File(filePath)))

        uploadTask.continueWithTask {
            if (!it.isSuccessful) {
                //error
                Log.d(TAG, "uploadRecord -1: ERROR upload record to storage firebase $it")
            }
            ref.downloadUrl

        }.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                chatRecordDownloadUriMutableLiveData.value = downloadUri
            } else {
                //error
                Log.d(TAG, "uploadRecord -2: ERROR upload record to storage firebase $it")
            }
        }
    }

    // function for pass messages list for recycler to show
    fun loadMessages(): LiveData<List<Message>> {

        if (messagesMutableLiveData.value != null) return messagesMutableLiveData

        messageCollectionReference.addSnapshotListener { querySnapShot, firebaseFirestoreException ->
            if (firebaseFirestoreException == null) {
                messagesList.clear()//clear message list so won't get duplicated with each new message
                querySnapShot?.documents?.forEach {
                    if (it.id == "${senderId}_${receiverId}" || it.id == "${receiverId}_${senderId}") {
                        //this is the chat document we should read messages array
                        val messagesFromFirestore =
                            it.get("messages") as List<HashMap<String, Any>>?
                                ?: throw Exception("My cast can't be done")
                        messagesFromFirestore.forEach { messageHashMap ->

                            val message = when (messageHashMap["type"] as Double?) {
                                0.0 -> {
                                    messageHashMap.toDataClass<TextMessage>()
                                }
                                1.0 -> {
                                    messageHashMap.toDataClass<ImageMessage>()
                                }
                                2.0 -> {
                                    messageHashMap.toDataClass<FileMessage>()
                                }
                                3.0 -> {
                                    messageHashMap.toDataClass<RecordMessage>()
                                }
                                else -> {
                                    throw Exception("unknown type")
                                }
                            }

                            messagesList.add(message)
                        }

                        if (!messagesList.isNullOrEmpty())
                            messagesMutableLiveData.value = messagesList
                    }

                }
            }
        }

        return messagesMutableLiveData
    }

    // function for upload messages that send
    fun sendMessage(message: Message) {
        //so we don't create multiple nodes for same chat
        messageCollectionReference.document("${senderId}_${receiverId}").get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    //this node exists send your message
                    messageCollectionReference.document("${senderId}_${receiverId}")
                        .update("messages", FieldValue.arrayUnion(message.serializeToMap()))

                } else {
                    //senderId_receiverId node doesn't exist check receiverId_senderId
                    messageCollectionReference.document("${receiverId}_${senderId}").get()
                        .addOnSuccessListener { documentSnapshot2 ->

                            if (documentSnapshot2.exists()) {
                                messageCollectionReference.document("${receiverId}_${senderId}")
                                    .update(
                                        "messages",
                                        FieldValue.arrayUnion(message.serializeToMap())
                                    )
                            } else {
                                //no previous chat history(senderId_receiverId & receiverId_senderId both don't exist)
                                //so we create document senderId_receiverId then messages array then add messageMap to messages
                                messageCollectionReference.document("${senderId}_${receiverId}")
                                    .set(
                                        mapOf("messages" to mutableListOf<Message>()),
                                        SetOptions.merge()
                                    ).addOnSuccessListener {
                                        //this node exists send your message
                                        messageCollectionReference.document("${senderId}_${receiverId}")
                                            .update(
                                                "messages",
                                                FieldValue.arrayUnion(message.serializeToMap())
                                            )

                                        //add ids of chat members
                                        messageCollectionReference.document("${senderId}_${receiverId}")
                                            .update(
                                                "chat_members",
                                                FieldValue.arrayUnion(senderId, receiverId)
                                            )

                                    }
                            }
                        }
                }
            }

    }

    // function for upload image to storage firebase and get uri
    fun uploadChatImageByUri(data: Uri?): LiveData<Uri> {
        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_pictures/" + data?.path)
        val uploadTask = data?.let { ref.putFile(it) }

        uploadTask?.continueWithTask {
            if (!it.isSuccessful) {
                //error
                Log.e(TAG, "uploadChatImageByUri: ERROR: $it")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                chatImageDownloadUriMutableLiveData.value = downloadUri

            } else {
                //error
                Log.e(TAG, "uploadChatImageByUri: ERROR UPLOAD: $it")
            }
        }
        return chatImageDownloadUriMutableLiveData
    }

    // function for store the uri with the message chat file was uploaded now
    fun uploadChatFileByUri(filePath: Uri?): LiveData<Map<String, Any?>> {

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_files/" + filePath.toString())
        val uploadTask = filePath?.let { ref.putFile(it) }

        uploadTask?.continueWithTask {
            if (!it.isSuccessful) {
                //error
                Log.d(TAG, "uploadChatFileByUri: ERROR-1 ${it.exception?.message}")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                Log.d(TAG, "uploadChatFileByUri: COMPLETE")
                chatFileMapMutableLiveData.value = mapOf<String, Any?>(
                    "downloadUri" to downloadUri,
                    "fileName" to filePath
                )


            } else {
                //error
                Log.d(TAG, "uploadChatFileByUri: ERROR-2 ${it.exception?.message}")
            }
        }
        return chatFileMapMutableLiveData
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }

}


val gson = Gson()

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}


//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}
