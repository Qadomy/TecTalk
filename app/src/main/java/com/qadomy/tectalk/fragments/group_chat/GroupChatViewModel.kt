package com.qadomy.tectalk.fragments.group_chat

import android.net.Uri
import android.util.Log
import android.util.Log.d
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qadomy.tectalk.model.*
import com.qadomy.tectalk.utils.Common.CHAT_MEMBER_IN_GROUP
import com.qadomy.tectalk.utils.Common.MESSAGES_COLLECTIONS
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.StorageUtil
import java.io.File
import java.util.*

class GroupChatViewModel(val senderId: String?, val groupName: String) : ViewModel() {

    private lateinit var mStorageRef: StorageReference
    private val messageCollectionReference =
        FireStoreUtil.firestoreInstance.collection(MESSAGES_COLLECTIONS)

    private val messagesMutableLiveData = MutableLiveData<List<Message>>()
    private val messagesList: MutableList<Message> by lazy { mutableListOf<Message>() }
    private val chatFileMapMutableLiveData = MutableLiveData<Map<String, Any?>>()
    private val chatImageDownloadUriMutableLiveData = MutableLiveData<Uri>()

    val chatRecordDownloadUriMutableLiveData = MutableLiveData<Uri>()


    fun loadMessage(): LiveData<List<Message>> {
        if (messagesMutableLiveData.value != null) return messagesMutableLiveData

        messageCollectionReference.addSnapshotListener(EventListener { querySnapShot, firebaseFireStoreException ->
            if (firebaseFireStoreException == null) {
                //clear message list so won't get duplicated with each new message
                messagesList.clear()

                querySnapShot?.documents?.forEach {
                    if (it.id == groupName) {
                        // this is the chat document we should read messages array
                        val messagesFromFirestore =
                            it.get(MESSAGES_COLLECTIONS) as List<HashMap<String, Any>>?
                                ?: return@EventListener

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
        })

        return messagesMutableLiveData
    }

    // function for upload record voice to database fire store in firebase
    fun uploadRecord(filePath: String) {
        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("records/" + Date().time)
        val uploadTask = ref.putFile(Uri.fromFile(File(filePath)))

        uploadTask.continueWithTask {
            if (!it.isSuccessful) {
                d(TAG, "uploadRecord: ERROR $it")

            }
            ref.downloadUrl
        }.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                chatRecordDownloadUriMutableLiveData.value = downloadUri
            } else
                d(TAG, "uploadRecord: ERROR UPLOAD: $it")

        }
    }

    // function for upload message's to database fire store in firebase
    fun sendMessage(message: Message) {
        // so we don't create multiple nodes for same chat
        messageCollectionReference.document(groupName).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    //this node exists send your message
                    messageCollectionReference.document(groupName)
                        .update(
                            MESSAGES_COLLECTIONS,
                            FieldValue.arrayUnion(message.serializeToMap())
                        )

                } else {
                    // senderId_receiverId node doesn't exist check receiverId_senderId
                    /**
                     * user have history chat so we update on it [add new chat to history]
                     */
                    messageCollectionReference.document(groupName).get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                messageCollectionReference.document(groupName)
                                    .update(
                                        MESSAGES_COLLECTIONS,
                                        FieldValue.arrayUnion(message.serializeToMap())
                                    )

                            } else {
                                //no previous chat history(senderId_receiverId & receiverId_senderId both don't exist)
                                //so we create document senderId_receiverId then messages array then add messageMap to messages
                                messageCollectionReference.document(groupName)
                                    .set(
                                        mapOf(MESSAGES_COLLECTIONS to mutableListOf<Message>()),
                                        SetOptions.merge()
                                    ).addOnSuccessListener {
                                        //this node exists send your message
                                        messageCollectionReference.document(groupName)
                                            .update(
                                                MESSAGES_COLLECTIONS,
                                                FieldValue.arrayUnion(message.serializeToMap())
                                            )

                                        //add ids of chat members
                                        messageCollectionReference.document(groupName)
                                            .update(
                                                CHAT_MEMBER_IN_GROUP,
                                                FieldValue.arrayUnion(senderId)
                                            )
                                    }
                            }
                        }
                }
            }
    }


    fun uploadChatFileByUri(filePath: Uri?): LiveData<Map<String, Any?>> {

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_files/" + filePath.toString())
        val uploadTask = filePath?.let { ref.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                //error
                d(TAG, "uploadChatFileByUri [ERROR]: ${task.exception?.message}")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                d(TAG, "uploadChatFileByUri: [COMPLETE]")
                chatFileMapMutableLiveData.value = mapOf<String, Any?>(
                    "downloadUri" to downloadUri,
                    "fileName" to filePath
                )

            } else {
                //error
                Log.e(TAG, "uploadChatFileByUri: [ERROR] ${it.exception?.message}")
            }
        }
        return chatFileMapMutableLiveData
    }

    fun uploadChatImageByUri(data: Uri?): LiveData<Uri> {
        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_pictures/" + data?.path)
        val uploadTask = data?.let { ref.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                //error
                d(TAG, "uploadChatImageByUri: [ERROR] $task")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                chatImageDownloadUriMutableLiveData.value = downloadUri

            } else {
                //error
                Log.e(TAG, "uploadChatImageByUri: [ERROR UPLOAD] $it")
            }
        }
        return chatImageDownloadUriMutableLiveData
    }

    companion object {
        private const val TAG = "GroupChatViewModel"
    }
}

/**
 *
 *
 *
 */
val gson = Gson()

//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}