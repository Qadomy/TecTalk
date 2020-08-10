package com.qadomy.tectalk.fragments.chat_fragment

import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.qadomy.tectalk.databinding.ChatFragmentBinding
import com.qadomy.tectalk.model.Message
import com.qadomy.tectalk.model.User
import kotlinx.android.synthetic.main.activity_main.*

class ChatFragment : Fragment() {

    // data binding
    private var _binding: ChatFragmentBinding? = null
    private val binding get() = _binding!!

    // view model, view model factory
    private lateinit var viewModel: ChatViewModel
    private lateinit var viewModelFactory: ChatViewModelFactory


    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var messageList = mutableListOf<Message>()

    private lateinit var loggedUser: User
    private lateinit var clickedUser: User

    // record
    private var recordStart = 0L
    private var recordDuration = 0L
    private var recorder: MediaRecorder? = null
    var isRecording = false //whether is recoding now or not
    var isRecord = true //whether it is text message or record


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // enable menu
        setHasOptionsMenu(true)

        // data binding
        _binding = ChatFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE


    }


    companion object {
        private const val TAG = "ChatFragment"
//        fun newInstance() = ChatFragment()

        const val SELECT_CHAT_IMAGE_REQUEST = 3
        const val CHOOSE_FILE_REQUEST = 4
    }


}