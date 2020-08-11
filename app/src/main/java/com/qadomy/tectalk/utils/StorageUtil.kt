package com.qadomy.tectalk.utils

import android.util.Log
import com.google.firebase.storage.FirebaseStorage

object StorageUtil {

    private const val TAG = "StorageUtil"

    val storageInstance: FirebaseStorage by lazy {
        Log.d(TAG, "StorageUtil.: ")
        FirebaseStorage.getInstance()

    }
}