package com.qadomy.tectalk.fragments.find_user

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.FindUserFragmentBinding
import com.qadomy.tectalk.utils.Common.CLICKED_USER

class FindUserFragment : Fragment() {

    // binding
    private var _binding: FindUserFragmentBinding? = null
    private val binding get() = _binding!!

    // user adapter
    private lateinit var adapter: UserAdapter

    // view model
    private lateinit var viewModel: FindUserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // enable menu
        setHasOptionsMenu(true)

        // set action bar title name Find Friends
        activity?.title = getString(R.string.search_for_friends)


        // data binding
        _binding = FindUserFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // init view model
        viewModel = ViewModelProvider(requireActivity()).get(FindUserViewModel::class.java)

        // get list of users
        viewModel.loadUsers().observe(viewLifecycleOwner, Observer {
            //hide loading
            binding.loadingImage.visibility = View.GONE

            if (it.isNullOrEmpty()) {
                // if there is no friends
                binding.noUsersLayout.visibility = View.VISIBLE
            } else {
                adapter.submitList(it)
                adapter.userList = it
            }


        })


        //setup recycler
        adapter = UserAdapter(UserClickListener {
            val gson = Gson()
            val clickedUser = gson.toJson(it)

            val bundle = bundleOf(
                CLICKED_USER to clickedUser
            )

            // when click on items in friends list
            findNavController().navigate(
                R.id.action_findUserFragment_to_differentUserProfileFragment,
                bundle
            )
        })
        binding.recycler.adapter = adapter
    }


    /**
     *  region Menu
     */
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

            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }

    // endregion

    companion object {
        fun newInstance() = FindUserFragment()
    }
}