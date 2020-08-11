package com.qadomy.tectalk.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.tectalk.databinding.ItemChatOneToOneBinding
import com.qadomy.tectalk.model.ChatParticipant
import java.util.*

var mQuery = ""

class ChatPreviewAdapter(private val clickListener: ClickListener) :
    ListAdapter<ChatParticipant, ChatPreviewAdapter.ViewHolder>(
        DiffCallbackUsers()
    )
    , Filterable, OnQueryTextChange {

    var chatList = listOf<ChatParticipant>()
    var filteredChatList = mutableListOf<ChatParticipant>()

    class ViewHolder private constructor(private val binding: ItemChatOneToOneBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: ClickListener, chatParticipant: ChatParticipant) {
            Log.d(TAG, "bind: ViewHolder.bind:")

            binding.chatParticipant = chatParticipant
            binding.clickListener = clickListener

            /** if query text isn't empty_box set the selected text with sky blue+bold */
            val username = chatParticipant.participant?.username
            if (mQuery.isEmpty()) {
                binding.nameTextView.text = username
            } else {
                var index = username?.indexOf(mQuery, 0, true)!!
                val sb = SpannableStringBuilder(username)
                while (index >= 0) {
                    val fcs = ForegroundColorSpan(Color.rgb(135, 206, 235))
                    sb.setSpan(
                        fcs,
                        index,
                        index + mQuery.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    sb.setSpan(
                        StyleSpan(Typeface.BOLD),
                        index,
                        index + mQuery.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    index = username.indexOf(mQuery, index + 1)
                }
                binding.nameTextView.text = sb
            }

            binding.executePendingBindings()

        }

        companion object {
            private const val TAG = "ChatPreviewAdapter"

            fun from(parent: ViewGroup): ViewHolder {
                // inflate layout for set in recycle view
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemChatOneToOneBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(
                    binding
                )
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(
            parent
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(clickListener, item)
    }

    override fun onChange(query: String) {
        mQuery = query
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                filteredChatList = mutableListOf()
                if (charString.isEmpty()) {
                    filteredChatList = chatList as MutableList<ChatParticipant>

                } else {
                    for (chatParticipant in chatList) {
                        if (chatParticipant.participant?.username?.toLowerCase(Locale.ENGLISH)
                                ?.contains(
                                    charString.toLowerCase(Locale.ENGLISH)
                                )!!
                        ) {
                            filteredChatList.add(chatParticipant)
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredChatList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {

                val mutableList = filterResults.values as MutableList<ChatParticipant?>?
                submitList(mutableList)
                mutableList?.size?.let { notifyItemRangeChanged(0, it) }
                notifyItemChanged(0)


            }
        }
    }
}


/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minimum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class DiffCallbackUsers : DiffUtil.ItemCallback<ChatParticipant>() {
    override fun areItemsTheSame(oldItem: ChatParticipant, newItem: ChatParticipant): Boolean {
        return oldItem.lastMessageDate == newItem.lastMessageDate
    }

    override fun areContentsTheSame(oldItem: ChatParticipant, newItem: ChatParticipant): Boolean {
        return oldItem == newItem
    }
}


class ClickListener(val clickListener: (chatParticipant: ChatParticipant) -> Unit) {
    fun onClick(chatParticipant: ChatParticipant) {
        return clickListener(chatParticipant)
    }
}


interface OnQueryTextChange {
    fun onChange(query: String)
}