package com.qadomy.tectalk.fragments.incoming_requests

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.IncomingRequestsFragmentBinding
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import kotlinx.android.synthetic.main.activity_main.*

class IncomingRequestsFragment : Fragment() {

    // binding
    private var _binding: IncomingRequestsFragmentBinding? = null
    private val binding get() = _binding!!

    // adapter
    private lateinit var adapter: IncomingRequestsAdapter
    var sendersList: MutableList<User>? = null

    // view model
    private lateinit var viewModel: IncomingRequestsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title name Contacts
        activity?.title = getString(R.string.friend_requests)

        // data binding
        _binding = IncomingRequestsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE

        // init view model
        viewModel = ViewModelProvider(requireActivity()).get(IncomingRequestsViewModel::class.java)


        //get user from shared preferences
        // TODO: 8/13/20 get it by navigation hos
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)

        //get friend requests if receivedRequest isn't empty_box
        val receivedRequest = loggedUser.receivedRequests
        if (!receivedRequest.isNullOrEmpty()) {
            viewModel.downloadRequests(receivedRequest)
                .observe(viewLifecycleOwner, Observer { requesterList ->
                    //hide loading
                    binding.loadingRequestsImageView.visibility = View.GONE

                    if (requesterList == null) {
                        //error while getting received requests
                        binding.noIncomingRequestsLayout.visibility = View.VISIBLE
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_while_loading),
                            Toast.LENGTH_LONG
                        ).show()

                    } else {
                        //got requests successfully
                        binding.noIncomingRequestsLayout.visibility = View.GONE
                        adapter.setDataSource(requesterList)
                        sendersList = requesterList
                        binding.receivedRequestsRecycler.adapter = adapter
                    }
                })

        } else {
            //no received requests
            binding.noIncomingRequestsLayout.visibility = View.VISIBLE
            binding.loadingRequestsImageView.visibility = View.GONE
        }


        //handle click on item of friend request recycler
        adapter =
            IncomingRequestsAdapter(
                object :
                    IncomingRequestsAdapter.ButtonCallback {
                    override fun onConfirmClicked(user: User, position: Int) {
                        viewModel.addToFriends(user.uid!!, loggedUser.uid!!)

                        Toast.makeText(
                            requireContext(),
                            "${user.username} ${R.string.add_to_your_friends}",
                            Toast.LENGTH_LONG
                        ).show()
                        deleteFromRecycler(position)
                    }

                    override fun onDeleteClicked(user: User, position: Int) {
                        viewModel.deleteRequest(user.uid!!, loggedUser.uid!!)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.request_deleted),
                            Toast.LENGTH_LONG
                        ).show()
                        deleteFromRecycler(position)
                    }


                    //Delete accepted/decline request from recycler
                    private fun deleteFromRecycler(position: Int) {
                        sendersList?.removeAt(position)
                        adapter.setDataSource(sendersList)
                        adapter.notifyItemRemoved(position)
                        //if no requests left (after user accept or delete)show the empty_box layout
                        if (sendersList?.size == 0) {
                            binding.noIncomingRequestsLayout.visibility = View.VISIBLE
                        }
                    }

                })
    }

    companion object {
        fun newInstance() = IncomingRequestsFragment()
    }
}