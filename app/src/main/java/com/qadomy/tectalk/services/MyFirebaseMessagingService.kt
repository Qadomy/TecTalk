package com.qadomy.tectalk.services

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.FireStoreUtil

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MessagingService"

        //FCM uses tokens to identify devices
        fun getInstanceId() {
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        println("MyFirebaseMessagingService.getInstanceId:${task.exception}")
                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token
                    println("MyFirebaseMessagingService.s:${token}")
                    if (token != null) {
                        addTokenToUserDocument(token)
                    }

                })
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "onNewToken: $token")
        addTokenToUserDocument(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO: 8/10/20 Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Handle message within 10 seconds
            //      handleNow()
        }


        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }


        // TODO: 8/10/20  Also if you intend on generating your own notifications as a result of a received FCM
        //         message, here is where that should be initiated. See sendNotification method below.

    }

}


// function for add token to user in firestore database in firebase
fun addTokenToUserDocument(token: String) {
    val loggedUserID = AuthUtil.firebaseAuthInstance.currentUser?.uid
    if (loggedUserID != null) {
        FireStoreUtil.firestoreInstance.collection("users").document(loggedUserID)
            .update("token", token)
    }

}