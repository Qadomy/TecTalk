package com.qadomy.tectalk.ui.main_avtivity

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


}