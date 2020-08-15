package com.qadomy.tectalk.fragments.group_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GroupChatViewModelFactory(
    private val senderId: String?,
    private val groupName: String
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupChatViewModel::class.java)) {
            return GroupChatViewModel(senderId, groupName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
