package com.qadomy.tectalk.fragments.group_info

import android.net.Uri
import android.util.Log.d
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.StorageReference
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.PROFILE_PICTURE_URL
import com.qadomy.tectalk.utils.Common.USERS_COLLECTIONS
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.LoadState.*
import com.qadomy.tectalk.utils.StorageUtil
import java.util.*

class GroupInfoViewModel : ViewModel() {

    private var bioLoadState = MutableLiveData<LoadState>()
    private lateinit var mStorageRef: StorageReference

    val uploadImageLoadStateMutableLiveData = MutableLiveData<LoadState>()
    val newImageUriMutableLiveData = MutableLiveData<Uri>()

    private var userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
        FireStoreUtil.firestoreInstance.collection(USERS_COLLECTIONS).document(it)
    }

    fun updateBio(bio: String) {
        userDocRef?.update("bio", bio)
            ?.addOnSuccessListener {
                bioLoadState.value = SUCCESS

            }
            ?.addOnFailureListener {
                bioLoadState.value = FAILURE
            }

    }


    fun uploadProfileImageByUri(data: Uri?) {
        //show upload ui
        uploadImageLoadStateMutableLiveData.value = LOADING

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("profile_pictures/" + data?.lastPathSegment + Date().time)
        val uploadTask = data?.let { ref.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                uploadImageLoadStateMutableLiveData.value = FAILURE
            } else {
                d(TAG, "uploadProfileImageByUri: {$task}")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                saveImageUriInFirebase(downloadUri)
            } else {
                uploadImageLoadStateMutableLiveData.value = FAILURE
                d(TAG, "uploadProfileImageByUri: $it")
            }
        }
    }


    fun uploadImageAsByteArray(bytes: ByteArray) {
        //show upload ui
        uploadImageLoadStateMutableLiveData.value = LOADING

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("profile_pictures/" + System.currentTimeMillis())
        val uploadTask = bytes.let { ref.putBytes(it) }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                uploadImageLoadStateMutableLiveData.value = FAILURE
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                saveImageUriInFirebase(downloadUri)
            } else {
                uploadImageLoadStateMutableLiveData.value = FAILURE
            }
        }

    }


    //save download uri of image in the user document
    private fun saveImageUriInFirebase(downloadUri: Uri?) {

        AuthUtil.getAuthId().let {
            FireStoreUtil.firestoreInstance.collection(USERS_COLLECTIONS).document(it)
                .update(PROFILE_PICTURE_URL, downloadUri.toString())
                .addOnSuccessListener {
                    uploadImageLoadStateMutableLiveData.value = SUCCESS
                    newImageUriMutableLiveData.value = downloadUri

                }
                .addOnFailureListener {
                    uploadImageLoadStateMutableLiveData.value = FAILURE
                }
        }

    }


    companion object {
        private const val TAG = "GroupInfoViewModel"
    }
}