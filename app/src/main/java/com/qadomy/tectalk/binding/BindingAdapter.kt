package com.qadomy.tectalk.binding

import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.qadomy.tectalk.R
import com.qadomy.tectalk.model.ChatParticipant
import com.qadomy.tectalk.model.User
import kotlinx.android.synthetic.main.issue_layout.view.*

// Navigate to signup fragment
@BindingAdapter("android:navigateToSignUpFragment")
fun navigateToSignUpFragment(view: View, navigate: Boolean) {
    view.setOnClickListener {
        if (navigate) {
            view.findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }
}


// Navigate to contacts fragment
@BindingAdapter("android:navigateToContactsFragment")
fun navigateToContactsFragment(floatingActionButton: FloatingActionButton, navigate: Boolean) {
    floatingActionButton.setOnClickListener {
        if (navigate) {
            floatingActionButton.findNavController()
                .navigate(R.id.action_homeFragment_to_contactsFragment)
        }
    }
}


// close issue layout when display in Login and sign up fragment
@BindingAdapter("android:closeIssueLayout")
fun closeIssueLayout(view: View, click: Boolean) {
    view.cancelImage.setOnClickListener {
        if (click)
            view.visibility = View.GONE
    }
}


// load image form url into image using Glide
@BindingAdapter("setRoundImageFromChatParticipant")
fun setRoundImageFromChatParticipant(imageView: ImageView, chatParticipant: ChatParticipant) {

    Glide.with(imageView.context)
        .load(chatParticipant.participant!!.profile_picture_url)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.anonymous_profile)
                .circleCrop()
        )
        .into(imageView)
}


// set what last thing message send
@BindingAdapter("setLastMessageText")
fun setLastMessageText(textView: TextView, chatParticipant: ChatParticipant) {

    //format last message to show like you:hello OR amr:Hi depending on sender OR you sent photo OR amr sent photo
    //depending on sender and is it text or image message

    if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 0.0) {
        //format last message to show like you:hello
        textView.text = textView.context.getString(R.string.you, chatParticipant.lastMessage)

    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 0.0) {
        //format last message to show like amr:hello
        textView.text = textView.context.getString(
            R.string.other,
            chatParticipant.participant!!.username!!.split("\\s".toRegex())[0],
            chatParticipant.lastMessage
        )
    } else if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 1.0) {
        //format last message to show like you sent an image
        textView.text = textView.context.getString(R.string.you_sent_image)
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 1.0) {
        //format last message to show like amr sent an image
        textView.text = textView.context.getString(
            R.string.other_image,
            chatParticipant.participant!!.username!!.split("\\s".toRegex())[0]
        )
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 2.0) {
        //format last message to show like amr sent a file
        textView.text = textView.context.getString(
            R.string.other_file,
            chatParticipant.participant!!.username!!.split("\\s".toRegex())[0]
        )
    } else if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 2.0) {
        //format last message to show like you sent a file
        textView.text = textView.context.getString(R.string.you_sent_file)
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 3.0) {
        //format last message to show like amr sent a voice record
        textView.text = textView.context.getString(
            R.string.other_record,
            chatParticipant.participant!!.username!!.split("\\s".toRegex())[0]
        )
    } else if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 3.0) {
        //format last message to show like you  sent a voice record
        textView.text = textView.context.getString(R.string.you_sent_record)
    }

}


@BindingAdapter("formatDateFromMap")
fun formatDateFromMap(textView: TextView, map: Map<String, Double>?) {
    val time = (map?.get("seconds"))
    if (time != null) textView.text = DateUtils.getRelativeTimeSpanString(time.toLong() * 1000)

}


@BindingAdapter("formatDate")
fun formatDate(textView: TextView, timestamp: Timestamp?) {
    textView.text = timestamp?.seconds?.let { DateUtils.getRelativeTimeSpanString(it * 1000) }
}


@BindingAdapter("setUnderlinedText")
fun setUnderlinedText(textView: TextView, text: String) {
    val content = SpannableString(text)
    content.setSpan(UnderlineSpan(), 0, content.length, 0)
    textView.text = content
}


@BindingAdapter("setChatImage")
fun setChatImage(imageView: ImageView, imageUri: String) {
    Glide.with(imageView.context)
        .load(imageUri)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_poor_connection_black_24dp)
        )
        .into(imageView)

}

@BindingAdapter("setRoundImage")
fun setRoundImage(imageView: ImageView, item: User) {
    item.let {
        val imageUri = it.profile_picture_url
        Glide.with(imageView.context)
            .load(imageUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loading_animation)
                    .error(R.drawable.anonymous_profile)
                    .circleCrop()
            )
            .into(imageView)
    }

}


@BindingAdapter("setDuration")
fun setDuration(textView: TextView, timeInMillis: String?) {

    if (timeInMillis == null) return

    val h = (timeInMillis.toInt().div(3600000))
    val m = (timeInMillis.toInt().div(60000).rem(60))
    val s = (timeInMillis.toInt().div(1000).rem(60))

    val sp = when (h) {
        0 -> {
            StringBuilder().append(m).append(":").append(s)
        }
        else -> {
            StringBuilder().append(h).append(":").append(m).append(":").append(s)
        }
    }
    textView.text = sp
}