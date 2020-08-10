package com.qadomy.tectalk.model

data class ChatParticipant(
    var participant: User? = null,
    var lastMessage: String? = null,
    var lastMessageDate: Map<String, Double>? = null,
    var isLoggedUser: Boolean? = null,
    var lastMessageType: Double? = null
)
