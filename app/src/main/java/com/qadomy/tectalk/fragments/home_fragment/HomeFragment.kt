package com.qadomy.tectalk.fragments.home_fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.qadomy.tectalk.R
import com.qadomy.tectalk.adapter.ChatPreviewAdapter
import com.qadomy.tectalk.adapter.ClickListener
import com.qadomy.tectalk.databinding.HomeFragmentBinding
import com.qadomy.tectalk.model.ChatParticipant
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.services.MyFirebaseMessagingService
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.FireStoreUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.min

class HomeFragment : Fragment() {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var sharedViewModel: SharedViewModel

    // for get requests counts to display in notification
    private var receivedRequestsCount: Int? = null
    private lateinit var countBadgeTextView: TextView
    private val gson = Gson()

    // ChatPreviewAdapter
    private val adapter: ChatPreviewAdapter by lazy {
        ChatPreviewAdapter(ClickListener { chatParticipant ->
            //navigate to chat with selected user on chat outer item click
            activity?.navView?.visibility = View.GONE
            val clickedUser = gson.toJson(chatParticipant.participant)
            findNavController().navigate(
                R.id.action_homeFragment_to_chatFragment, bundleOf(
                    CLICKED_USER to clickedUser
                )
            )
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set title on action bar
        activity?.title = "Chats"

        // set menu true
        setHasOptionsMenu(true)

        // data binding
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // display BottomNavigationView
        activity?.navView?.visibility = View.VISIBLE


        //get logged user token and add it to user document (for FCM)
        MyFirebaseMessagingService.getInstanceId()

        //theses intent extras are coming from FCM notification click so we need to move to specific chat if not null
        intentExtrasFromFCM()


        //get user data
        getUserData()

    }


    //theses intent extras are coming from FCM notification click so we need to move to specific chat if not null
    private fun intentExtrasFromFCM() {
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
    }

    //get user data from Home View Model
    private fun getUserData() {
        viewModel.loggedUserMutableLiveData.observe(viewLifecycleOwner, Observer { loggedUser ->
            //save logged user data in shared pref to use in other fragments
            val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
            val json = gson.toJson(loggedUser)
            prefsEditor.putString("loggedUser", json)
            prefsEditor.apply()


            //show notification badge if there is incoming requests
            receivedRequestsCount = loggedUser.receivedRequests?.size ?: 0
            setupBadge(receivedRequestsCount)


            //get user chat history
            viewModel.getChats(loggedUser!!)
                ?.observe(viewLifecycleOwner, Observer { chatParticipantsList ->

                    //Hide loading image
                    binding.loadingChatImageView.visibility = View.GONE
                    if (chatParticipantsList.isNullOrEmpty()) {
                        //show no chat layout
                        binding.noChatLayout.visibility = View.VISIBLE
                    } else {

                        //sort messages by date so newest show on top
                        val sortedChatParticipantsList: List<ChatParticipant> =
                            chatParticipantsList.sortedWith(compareBy { it.lastMessageDate?.get("seconds") })
                                .reversed()

                        // hide icon when there is no chats
                        binding.noChatLayout.visibility = View.GONE

                        // set in adapter
                        binding.recycler.adapter = adapter
                        adapter.submitList(sortedChatParticipantsList)
                        adapter.chatList = sortedChatParticipantsList
                    }

                })

        })
    }


    // region Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)


        inflater.inflate(R.menu.main_menu, menu)
        val menuItem = menu.findItem(R.id.action_incoming_requests)
        val actionView = menuItem?.actionView
        countBadgeTextView = actionView?.findViewById<View>(R.id.count_badge) as TextView
        //if fragment is coming from back stack setupBadge will be called before onCreateOptionsMenu so we have to call setup badge again
        setupBadge(receivedRequestsCount)



        actionView.setOnClickListener { onOptionsItemSelected(menuItem) }

        //do filtering when i type in search or click search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        // ste query for search view
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

        R.id.action_logout -> {
            logout()
            true
        }
        R.id.action_incoming_requests -> {
            findNavController().navigate(R.id.action_homeFragment_to_incomingRequestsFragment)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }


    // function for logout from firebase auth
    private fun logout() {
        removeUserToken()
        FirebaseAuth.getInstance().signOut()
        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)

        // TODO: 8/11/20 prevent to back to home after logout
    }

    // remove token from firestore database in firebase
    private fun removeUserToken() {
        val loggedUserID = AuthUtil.firebaseAuthInstance.currentUser?.uid
        if (loggedUserID != null) {
            FireStoreUtil.firestoreInstance.collection("users").document(loggedUserID)
                .update("token", null)
        }
    }

    // for set number in notification of request friends
    private fun setupBadge(count: Int?) {
        Log.d(TAG, "setupBadge: ")
        if (::countBadgeTextView.isInitialized) {
            if (null == count || count == 0) {
                countBadgeTextView.visibility = View.GONE
            } else {
                countBadgeTextView.visibility = View.VISIBLE
                // make maximum number of badge 99 if there more 99 friend request
                countBadgeTextView.text =
                    min(count, 99).toString()
            }
        }
    }
    // endregion menu


    companion object {
        private const val TAG = "HomeFragment"
        fun newInstance() = HomeFragment()
    }
}