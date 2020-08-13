package com.qadomy.tectalk.fragments.incoming_requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.tectalk.databinding.ReceivedRequestItemBinding
import com.qadomy.tectalk.model.User

class IncomingRequestsAdapter(private val buttonCallback: ButtonCallback) :
    RecyclerView.Adapter<IncomingRequestsAdapter.UserHolder>() {

    interface ButtonCallback {
        fun onConfirmClicked(user: User, position: Int)
        fun onDeleteClicked(user: User, position: Int)
    }

    private var mUsers = listOf<User>()

    class UserHolder private constructor(val binding: ReceivedRequestItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: User,
            buttonCallback: ButtonCallback
        ) {

            binding.user = item
            binding.executePendingBindings()

            //callback to parent fragment when button clicked
            binding.confirmButton.setOnClickListener {
                buttonCallback.onConfirmClicked(item, adapterPosition)
            }
            binding.deleteButton.setOnClickListener {
                buttonCallback.onDeleteClicked(item, adapterPosition)
            }
        }

        companion object {
            fun from(parent: ViewGroup): UserHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceivedRequestItemBinding.inflate(layoutInflater, parent, false)

                return UserHolder(
                    binding
                )
            }
        }


    }

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
        holder.bind(item, buttonCallback)
    }

}
