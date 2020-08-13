package com.qadomy.tectalk.fragments.different_user_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.adapter.FriendsAdapter
import com.qadomy.tectalk.databinding.DifferentUserProfileFragmentBinding
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import kotlinx.android.synthetic.main.activity_main.*

class DifferentUserProfileFragment : Fragment() {

    // binding
    private var _binding: DifferentUserProfileFragmentBinding? = null
    private val binding get() = _binding!!

    // adapter
    private val adapter: FriendsAdapter by lazy {
        FriendsAdapter(object : FriendsAdapter.ItemClickCallback {
            override fun onItemClicked(user: User) {
                Toast.makeText(requireContext(), "${user.username} clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    // view model
    private lateinit var viewModel: DifferentUserProfileViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // data binding
        _binding = DifferentUserProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE


        // init view models
        viewModel =
            ViewModelProvider(requireActivity()).get(DifferentUserProfileViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // get data of clicked user from find user fragment
        // TODO: 8/13/20 use here nav host from navigation fragment
        val gson = Gson()
        val user = gson.fromJson(arguments?.getString(CLICKED_USER), User::class.java)

        // set user name in action bar title
        activity?.title = "${user.username}'s profile"


        // check if already in Friends list
        viewModel.checkIfFriends(user.uid).observe(viewLifecycleOwner, Observer {
            when (it!!) {
                //change button color and icon to show that a request is sent or not
                DifferentUserProfileViewModel.FriendRequestState.SENT -> {
                    showButtonAsSentRequest()
                }
                DifferentUserProfileViewModel.FriendRequestState.NOT_SENT -> {
                    showButtonAsRequestNotSent()
                }
                DifferentUserProfileViewModel.FriendRequestState.ALREADY_FRIENDS -> {
                    showButtonAsAlreadyFriends()
                }
            }
        })


        // set data to views and download image
        binding.bioTextView.text = user.bio ?: "No bio yet"
        binding.name.text = user.username
        viewModel.downloadProfilePicture(user.profile_picture_url)


        // show downloaded image in profile image view
        viewModel.loadedImage.observe(viewLifecycleOwner, Observer {
            it.into(binding.profileImage)
        })


        // handle what happen when click send friend request
        binding.sendFriendRequestButton.setOnClickListener {
            //add id to sentRequests document in user
            when (binding.sendFriendRequestButton.text) {
                getString(R.string.friend_request_not_sent) -> {
                    viewModel.updateSentRequestsForSender(user.uid)
                    showButtonAsSentRequest()
                }
                getString(R.string.cancel_request) -> {
                    viewModel.cancelFriendRequest(user.uid)
                    showButtonAsRequestNotSent()
                }
                getString(R.string.delete_from_friends) -> {
                    viewModel.removeFromFriends(user.uid)
                    showButtonAsRequestNotSent()
                }
            }
        }


        //load friends of that user
        sharedViewModel.loadFriends(user).observe(viewLifecycleOwner, Observer { friendsList ->
            if (friendsList.isNullOrEmpty()) {
                binding.friendsTextView.text = getString(R.string.no_friends)
            } else {
                binding.friendsTextView.text = getString(R.string.friends)
                binding.friendsCountTextView.text = friendsList.size.toString()
                showFriendsInRecycler(friendsList)
            }
        })

    }


    // function for display an friends list for new user
    private fun showFriendsInRecycler(friendsList: List<User>?) {
        adapter.setDataSource(friendsList)
        binding.friendsRecycler.adapter = adapter
    }


    //change button to show that users are friends
    private fun showButtonAsAlreadyFriends() {
        binding.sendFriendRequestButton.text =
            getString(R.string.delete_from_friends)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_remove_circle_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { ContextCompat.getColorStateList(it, R.color.red) }
    }

    //change sent button to show that no request is sent
    private fun showButtonAsRequestNotSent() {
        binding.sendFriendRequestButton.text =
            getString(R.string.friend_request_not_sent)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_person_add_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { ContextCompat.getColorStateList(it, R.color.grey) }
    }

    //change sent button to show that  request is sent
    private fun showButtonAsSentRequest() {
        binding.sendFriendRequestButton.text = getString(R.string.cancel_request)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_done_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { ContextCompat.getColorStateList(it, R.color.green) }
    }


    companion object {
        fun newInstance() = DifferentUserProfileFragment()
    }

}