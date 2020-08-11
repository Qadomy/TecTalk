package com.qadomy.tectalk.fragments.chat_fragment

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.StorageReference
import com.qadomy.tectalk.utils.StorageUtil
import java.io.File
import java.util.*

class ChatViewModel(val senderId: String?, val receiverId: String) : ViewModel() {

    private lateinit var mStorageRef: StorageReference
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


    companion object {
        private const val TAG = "ChatViewModel"
    }

}