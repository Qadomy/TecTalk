package com.qadomy.tectalk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.tectalk.databinding.FriendItemBinding
import com.qadomy.tectalk.model.User

class FriendsAdapter(private val itemClickCallback: ItemClickCallback) :
    RecyclerView.Adapter<FriendsAdapter.UserHolder>() {

    private var mUsers = listOf<User>()

    interface ItemClickCallback {
        fun onItemClicked(user: User)
    }

    class UserHolder private constructor(val binding: FriendItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: User,
            itemClickCallback: ItemClickCallback
        ) {

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
                val binding = FriendItemBinding.inflate(layoutInflater, parent, false)

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

    override fun getItemCount() = mUsers.size

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val item = mUsers[position]
        holder.bind(item, itemClickCallback)
    }


    fun setDataSource(users: List<User>?) {
        if (users != null) {
            mUsers = users
        }
    }
}
