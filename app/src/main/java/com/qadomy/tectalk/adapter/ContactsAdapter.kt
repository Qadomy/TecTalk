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
import com.qadomy.tectalk.databinding.ContactItemBinding
import com.qadomy.tectalk.fragments.find_user.OnQueryTextChange
import com.qadomy.tectalk.fragments.home_fragment.mQuery
import com.qadomy.tectalk.model.User
import java.util.*

class ContactsAdapter(private val itemClickCallback: ItemClickCallback) :
    ListAdapter<User, ContactsAdapter.UserHolder>(DiffCallbackContacts())
    , Filterable,
    OnQueryTextChange {

    interface ItemClickCallback {
        fun onItemClicked(user: User)
    }


    private var filteredUserList = mutableListOf<User>()
    var usersList = listOf<User>()


    class UserHolder private constructor(val binding: ContactItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: User,
            itemClickCallback: ItemClickCallback
        ) {

            //if query text isn't empty_box set the selected text with sky blue+bold
            if (mQuery.isEmpty()) {
                binding.nameTextView.text = item.username
            } else {
                var index = item.username!!.indexOf(mQuery, 0, true)
                val sb = SpannableStringBuilder(item.username)
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
                    index = item.username.indexOf(mQuery, index + 1)
                }
                binding.nameTextView.text = sb
            }

            binding.user = item
            binding.executePendingBindings()

            //callback to parent fragment when button clicked
            binding.parentLayout.setOnClickListener {
                itemClickCallback.onItemClicked(item)
            }

        }

        companion object {
            fun from(parent: ViewGroup): UserHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ContactItemBinding.inflate(layoutInflater, parent, false)

                return UserHolder(
                    binding
                )
            }
        }


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        return UserHolder.from(
            parent
        )
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, itemClickCallback)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val charString = charSequence.toString()
                filteredUserList = mutableListOf()
                if (charString.isEmpty()) {
                    filteredUserList = usersList as MutableList<User>


                } else {
                    for (user in usersList) {
                        if (user.username?.toLowerCase(Locale.ENGLISH)?.contains(
                                charString.toLowerCase(Locale.ENGLISH)
                            )!!
                        ) {
                            filteredUserList.add(user)
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredUserList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, filterResults: FilterResults?) {
                try {
                    // TODO: 8/12/20 there is error -> non null
                    val mutableList = filterResults!!.values as MutableList<User>
                    submitList(mutableList)
                    notifyItemRangeChanged(0, mutableList.size)
                } catch (e: Exception) {
                    Log.e(TAG, "publishResults: ${e.message}")
                }
            }
        }

    }

    companion object {
        private const val TAG = "ContactsAdapter"
    }

    override fun onChange(query: String) {
        mQuery = query
    }
}


/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minumum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class DiffCallbackContacts : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}