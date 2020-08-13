package com.qadomy.tectalk.fragments.home_group

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.GroupFragmentBinding
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.services.MyFirebaseMessagingService
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_GROUP
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import kotlinx.android.synthetic.main.activity_main.*

class GroupFragment : Fragment() {

    // binding
    private var _binding: GroupFragmentBinding? = null
    private val binding get() = _binding!!

    // view model
    private lateinit var viewModel: GroupViewModel
    private lateinit var sharedViewModel: SharedViewModel

    //
    private var receivedRequestsCount: Int? = null
    private lateinit var countBadgeTextView: TextView
    private val gson = Gson()

    // adapter
    private val adapter: ChatPreviewAdapterRoom by lazy {
        ChatPreviewAdapterRoom(ClickListener { groupName ->
            //navigate to chat with selected user on chat outer item click
            val clickedGroup = gson.toJson(groupName)
            findNavController().navigate(
                R.id.action_groupFragment_to_roomChatFragment, bundleOf(
                    CLICKED_GROUP to clickedGroup
                )
            )
        })
    }

    /** onCreateView */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title name Group
        activity?.title = getString(R.string.groups)

        // enable menu
        setHasOptionsMenu(true)

        // display BottomNavigationView
        activity?.navView?.visibility = View.VISIBLE

        // data binding
        _binding = GroupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    } // end onCreateView


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // init view model
        viewModel = ViewModelProvider(requireActivity()).get(GroupViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        //get logged user token and add it to user document (for FCM)
        MyFirebaseMessagingService.getInstanceId()
//       viewModelRoom.createRoom("TestGroup1") // TODO


        //theses intent extras are coming from FCM notification click so we need to move to specific chat if not null
        val senderId = requireActivity().intent.getStringExtra("senderId")
        val senderName = requireActivity().intent.getStringExtra("senderName")
        if (senderId != null && senderName != null) {
            val receiverUser =
                User(uid = senderId, username = senderName)
            findNavController().navigate(
                R.id.action_homeFragment_to_chatFragment, bundleOf(
                    CLICKED_USER to gson.toJson(receiverUser)
                )
            )
            val nullSting: CharSequence? = null
            requireActivity().intent.putExtra("senderId", nullSting)
            requireActivity().intent.putExtra("senderName", nullSting)
        }


        //get user data
        viewModel.loggedUserMutableLiveData.observe(viewLifecycleOwner, Observer {
            //save logged user data in shared pref to use in other fragments
            val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
            val json = gson.toJson(it)
            prefsEditor.putString("loggedUser", json)
            prefsEditor.apply()


            //get user chat history
            viewModel.getRooms(it!!)
                .observe(viewLifecycleOwner, Observer { groupList ->

                    //Hide loading image
                    binding.loadingChatImageView.visibility = View.GONE
                    if (groupList.isNullOrEmpty()) {
                        //show no chat layout
                        binding.noChatLayout.visibility = View.VISIBLE

                    } else {
                        binding.noChatLayout.visibility = View.GONE
                        binding.recycler.adapter = adapter
                        adapter.submitList(groupList)
                        adapter.chatList = groupList
                    }
                })
        })
    }


    /**
     * region Menu
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)


        inflater.inflate(R.menu.main_menu_room, menu)
        val menuItem = menu.findItem(R.id.action_incoming_requests)
        val actionView = menuItem?.actionView


        actionView?.setOnClickListener { onOptionsItemSelected(menuItem) }

        //do filtering when i type in search or click search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryString: kotlin.String?): Boolean {
                adapter.filter.filter(queryString)
                return false
            }

            override fun onQueryTextChange(queryString: kotlin.String?): Boolean {
                adapter.filter.filter(queryString)
                if (queryString != null) {
                    adapter.onChange(queryString)
                }

                return false
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_create_group -> {
            findNavController().navigate(R.id.action_groupFragment_to_createGroupFragment)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }

    // endregion menu

    companion object {
        fun newInstance() = GroupFragment()
    }
}