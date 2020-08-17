package com.qadomy.tectalk.fragments.group_add_members

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.tectalk.databinding.CheckableListLayoutBinding
import com.qadomy.tectalk.model.User

class FriendsAdapter(private val itemClickCallback: ItemClickCallback) :
    RecyclerView.Adapter<FriendsAdapter.UserHolder>() {

    private var mUsers = listOf<User>()

    fun setDataSource(users: List<User>?) {
        if (users != null) {
            mUsers = users
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

    class UserHolder private constructor(val binding: CheckableListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
//        private var selectedItems: ArrayList<User>? = null

        //        var nonselectedItems: ArrayList<User>? = null
        fun bind(
            item: User,
            itemClickCallback: ItemClickCallback
        ) {

            binding.user = item
            binding.executePendingBindings()

            //callback to parent fragment when button clicked
            binding.txtTitle.setOnClickListener { view ->
                itemClickCallback.onItemClicked(item, view)
            }

        }

//        public fun returnTotalMembers(): ArrayList<User>? {
//            return selectedItems
//        }

        companion object {
            fun from(parent: ViewGroup): UserHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = CheckableListLayoutBinding.inflate(layoutInflater, parent, false)

                return UserHolder(
                    binding
                )
            }
        }


    }


    interface ItemClickCallback {
        fun onItemClicked(user: User, view: View)
    }
}
