package com.qadomy.tectalk.fragments.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.StorageReference
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.PROFILE_PICTURE_URL
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.StorageUtil
import java.util.*

class ProfileViewModel : ViewModel() {

    val uploadImageLoadStateMutableLiveData = MutableLiveData<LoadState>()
    val newImageUriMutableLiveData = MutableLiveData<Uri>()
    private lateinit var mStorageRef: StorageReference

    private var bioLoadState = MutableLiveData<LoadState>()

    private var userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
        FireStoreUtil.firestoreInstance.collection("users").document(it)
    }

    // function for update bio in firestore database in firebase
    fun updateBio(bio: String) {
        userDocRef?.update("bio", bio)
            ?.addOnSuccessListener {
                bioLoadState.value = LoadState.SUCCESS

            }
            ?.addOnFailureListener {
                bioLoadState.value = LoadState.FAILURE
            }
    }

    // function for upload image to storage firebase from [gallery]
    fun uploadProfileImageByUri(data: Uri?) {
        Log.d(TAG, "uploadProfileImageByUri: ")

        //show upload ui
        uploadImageLoadStateMutableLiveData.value = LoadState.LOADING

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("profile_pictures/" + data?.lastPathSegment + Date().time)
        val uploadTask = data?.let { ref.putFile(it) }

        uploadTask?.continueWithTask {
            if (!it.isSuccessful) {
                uploadImageLoadStateMutableLiveData.value = LoadState.FAILURE
                Log.d(TAG, "uploadProfileImageByUri: $it")
            }
            ref.downloadUrl
        }?.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                saveImageUriInFirebase(downloadUri)
            } else {
                uploadImageLoadStateMutableLiveData.value = LoadState.FAILURE
                Log.d(TAG, "uploadProfileImageByUri: ERROR UPLOAD: $it")
            }
        }
    }

    // function for upload image to storage firebase from [camera]
    fun uploadImageAsByteArray(bytes: ByteArray) {
        Log.d(TAG, "uploadImageAsByteArray: ")

        //show upload ui
        uploadImageLoadStateMutableLiveData.value = LoadState.LOADING

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("profile_pictures/" + System.currentTimeMillis())
        val uploadTask = bytes.let { ref.putBytes(it) }

        uploadTask.continueWithTask {
            if (!it.isSuccessful) {
                uploadImageLoadStateMutableLiveData.value = LoadState.FAILURE
                Log.d(TAG, "uploadImageAsByteArray: $it")
            }
            ref.downloadUrl
        }.addOnCompleteListener {
            if (it.isSuccessful) {
                val downloadUri = it.result
                saveImageUriInFirebase(downloadUri)
            } else {
                uploadImageLoadStateMutableLiveData.value = LoadState.FAILURE
                Log.d(TAG, "uploadImageAsByteArray: ERROR UPLOAD: $it")
            }
        }

    }


    // function for save image url from storage in firestore database in firebase
    private fun saveImageUriInFirebase(downloadUri: Uri?) {
        Log.d(TAG, "saveImageUriInFirebase: ")

        AuthUtil.getAuthId().let {
            FireStoreUtil.firestoreInstance.collection("users").document(it)
                .update(PROFILE_PICTURE_URL, downloadUri.toString())
                .addOnSuccessListener {
                    uploadImageLoadStateMutableLiveData.value = LoadState.SUCCESS
                    newImageUriMutableLiveData.value = downloadUri

                }
                .addOnFailureListener {
                    uploadImageLoadStateMutableLiveData.value = LoadState.FAILURE
                }
        }

    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}