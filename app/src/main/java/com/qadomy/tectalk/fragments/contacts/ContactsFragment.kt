package com.qadomy.tectalk.fragments.contacts

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.adapter.ContactsAdapter
import com.qadomy.tectalk.databinding.ContactsFragmentBinding
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import kotlinx.android.synthetic.main.activity_main.*

class ContactsFragment : Fragment() {

    // binding
    private var _binding: ContactsFragmentBinding? = null
    private val binding get() = _binding!!

    // view model
    private lateinit var viewModel: ContactsViewModel
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var adapter: ContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // enable menu
        setHasOptionsMenu(true)

        // set action bar title name Contacts
        activity?.title = getString(R.string.contacts)

        // data binding
        _binding = ContactsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE

        // init view models
        viewModel = ViewModelProvider(requireActivity()).get(ContactsViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)


        //get user from shared preferences
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)


        //on contact click move to chat fragment
        adapter = ContactsAdapter(object :
            ContactsAdapter.ItemClickCallback {
            override fun onItemClicked(clickedUser: User) {

                println("ContactsFragment.onItemClicked:${clickedUser.username}")
                //turn clicked user to json
                val clickedUser = gson.toJson(clickedUser)

                findNavController().navigate(
                    R.id.action_contactsFragment_to_chatFragment, bundleOf(
                        CLICKED_USER to clickedUser
                    )
                )
            }
        })


        // load friends list from database
        sharedViewModel.loadFriends(loggedUser).observe(viewLifecycleOwner, Observer {
            if (it != null) {
                //user has friends
                showFriends(it)
            } else {
                //user has no friends
                showEmptyLayout()
            }
        })

    }

    // function for display friends list
    private fun showFriends(it: List<User>) {
        binding.noFriendsLayout.visibility = View.GONE
        adapter.submitList(it)
        adapter.usersList = it
        binding.contactsRecycler.adapter = adapter
    }

    // function for display empty layout when there is no friends in the list
    private fun showEmptyLayout() {
        binding.noFriendsLayout.visibility = View.VISIBLE
        binding.addFriendsButton.setOnClickListener { findNavController().navigate(R.id.action_contactsFragment_to_findUserFragment) }
    }


    // region Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)


        inflater.inflate(R.menu.search_menu, menu)

        //do filtering when i type in search or click search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                adapter.filter.filter(queryString)
                return false
            }

            override fun onQueryTextChange(queryString: String?): Boolean {
                adapter.filter.filter(queryString)
                if (queryString != null) {
                    adapter.onChange(queryString)
                }

                return false
            }
        })


    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_search -> {
            Log.d(TAG, "onOptionsItemSelected: ${item.title}")
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }
    // endregion Menu


    companion object {
        private const val TAG = "ContactsFragment"

//        const val USERNAME = "username"
//        const val PROFILE_PICTURE = "profile_picture_url"
//        const val UID = "uid"

        fun newInstance() = ContactsFragment()
    }
}