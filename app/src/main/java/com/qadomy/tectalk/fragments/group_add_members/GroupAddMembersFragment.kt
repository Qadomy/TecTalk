package com.qadomy.tectalk.fragments.group_add_members

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.GroupAddMembersFragmentBinding
import com.qadomy.tectalk.model.GroupName
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_GROUP
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import kotlinx.android.synthetic.main.checkable_list_layout.view.*
import kotlinx.android.synthetic.main.group_add_members_fragment.*

class GroupAddMembersFragment : Fragment() {

    // binding
    private var _binding: GroupAddMembersFragmentBinding? = null
    private val binding get() = _binding!!

    // adapter
    lateinit var adapter: FriendsAdapter

    var selectedItems: ArrayList<User>? = null
    private var nonSelectedItems: ArrayList<User>? = null
    private lateinit var clickedGroup: GroupName

    // view model
    private lateinit var viewModel: GroupAddMembersViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title
        activity?.title = getString(R.string.my_profile)

        // data binding
        _binding = GroupAddMembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // init view models
        viewModel = ViewModelProvider(requireActivity()).get(GroupAddMembersViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        //get user from shared preferences
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)
        clickedGroup = gson.fromJson(arguments?.getString(CLICKED_GROUP), GroupName::class.java)
        selectedItems = java.util.ArrayList()
        nonSelectedItems = java.util.ArrayList()

        //create adapter and handle recycle item click callback
        adapter = FriendsAdapter(object :
            FriendsAdapter.ItemClickCallback {
            override fun onItemClicked(user: User, view: View) {
                btAddFreindsToGroup.visibility = VISIBLE

                Log.d(TAG, "selected Item is ${user.username}")

                if (selectedItems?.contains(user)!!) {
                    view.txt_title.isChecked = false
                    selectedItems
                        ?.remove(user)
                } else {
                    selectedItems?.add(
                        user
                    )
                    view.txt_title.isChecked = true

                }
            }
        })

        //load friends of logged in user and show in recycler
        sharedViewModel.loadFriends(loggedUser)
            .observe(viewLifecycleOwner, Observer { friendsList ->
                //hide loading
                binding.loadingImage.visibility = GONE
                if (friendsList != null) {
//                binding.friendsLayout.visibility = View.VISIBLE
//                binding.noFriendsLayout.visibility = View.GONE
                    showFriendsInRecycler(friendsList)
                } else {
//                  will handle later :)
                    Log.d(TAG, "onActivityCreated: will handle later")
                }
            })


        btAddFreindsToGroup.setOnClickListener {
            val newMembersIds = ArrayList<String>()
            for (i in selectedItems!!) {
                newMembersIds.add(i.uid.toString())
                Log.d(TAG, "${i.username} are listed")
            }

            for (i in clickedGroup.chat_members_in_group!!) {
                newMembersIds.add(i)
                Log.d(TAG, "$i are listed old")
            }
            clickedGroup.chat_members_in_group = newMembersIds
            viewModel.updateUserProfileForGroups(clickedGroup.group_name.toString(), newMembersIds)
            this.findNavController().popBackStack()
            //////////////////////  DONT FORGERT TO REMOVE THIS CODE AFTER IMPLEMENTING REFRESH OPTION ////////////////
            this.findNavController().popBackStack()
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        }
    }

    private fun showFriendsInRecycler(it: List<User>) {
        val newFriendliest = it.filterNot {
            clickedGroup.chat_members_in_group?.contains(it.uid)!!
        }
        adapter.setDataSource(newFriendliest)
        binding.recyclerWithCheckboxes.adapter = adapter
    }


    companion object {
        private const val TAG = "GroupAddMembersFragment"
        fun newInstance() = GroupAddMembersFragment()
    }
}