package com.qadomy.tectalk.fragments.chat_fragment

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.ChatFragmentBinding
import com.qadomy.tectalk.model.Message
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

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

        //setup bottom sheet
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        //set record view
        handleRecord()

        //get logged user from shared preferences
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        loggedUser = gson.fromJson(json, User::class.java)


        //get receiver data from contacts fragment(NOTE:IF NAVIGATING FROM FCM-NOTIFICATION USER ONLY HAS id,username)
        clickedUser = gson.fromJson(arguments?.getString(CLICKED_USER), User::class.java)


        // set the user who chat with as activity title
        activity?.title = "Chatting with ${clickedUser.username}"


        //user view model factory to pass ids on creation of view model
        if (clickedUser.uid != null) {
            viewModelFactory = ChatViewModelFactory(loggedUser.uid, clickedUser.uid.toString())
            viewModel =
                ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)
        }

        //Move layouts up when soft keyboard is shown
        // TODO: 8/11/20 check SOFT_INPUT_ADJUST_RESIZE deprecated in API 30 I guss
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        //send message on keyboard done click
        binding.messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        binding.recycler.adapter = adapter
    }


    // function for handle record
    private fun handleRecord() {
        //change fab icon depending on is text message empty_box or not
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                isRecord = if (editable.isNullOrEmpty()) {
                    // if not typing any thing in edit text -> mic icon still in fab button
                    // and set record true
                    binding.recordFab.setImageResource(R.drawable.mic)
                    true
                } else {
                    // if typing any thing in edit text -> send icon will be in fab button
                    // and set record false
                    binding.recordFab.setImageResource(R.drawable.sendicon)
                    false
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        // when click on fab button after set icon
        binding.recordFab.setOnClickListener {
            if (isRecord) {
                /**
                 * if record is on and that's mean mic button enabled
                 */
                if (isRecording) {
                    // if recording voice true

                    // change size and color or button so user know its finished recording
                    val regainer = AnimatorInflater.loadAnimator(
                        context,
                        R.animator.regain_size
                    ) as AnimatorSet
                    regainer.setTarget(binding.recordFab)
                    regainer.start()

                    // change color of fab button
                    binding.recordFab.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#b39ddb"))

                    //stop recording and upload record
                    stopRecording()
                    //show fake item with progress bar while record uploads
                    showPlaceholderRecord()

                    // upload record to storage in firebase
                    viewModel.uploadRecord("${requireActivity().externalCacheDir?.absolutePath}/audiorecord.3gp")

                    Toast.makeText(requireContext(), "Finished recording", Toast.LENGTH_SHORT)
                        .show()
                    // set isRecording opposite
                    isRecording = !isRecording

                } else {
                    // if recording voice false, we ask permission for using record
                    Dexter.withActivity(activity)
                        .withPermission(Manifest.permission.RECORD_AUDIO)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                                // change size and color or button so user know its recording
                                val increaser = AnimatorInflater.loadAnimator(
                                    context,
                                    R.animator.increase_size
                                ) as AnimatorSet
                                increaser.setTarget(binding.recordFab)
                                increaser.start()

                                // change color of fab button
                                binding.recordFab.backgroundTintList =
                                    ColorStateList.valueOf(Color.parseColor("#EE4B4B"))

                                //start recording
                                startRecording()
                                Toast.makeText(context, "Recording...", Toast.LENGTH_SHORT).show()

                                // set isRecording opposite
                                isRecording = !isRecording
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest?,
                                token: PermissionToken?
                            ) {
                                token?.continuePermissionRequest()
                                //notify parent activity that permission denied to show toast for manual permission giving
                                showSnackBar()
                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                                //notify parent activity that permission denied to show toast for manual permission giving
                                showSnackBar()
                            }

                        }).check()
                }

            } else {
                /**
                 * if record off and that's mean type text in edit text we send message when click on send button
                 */
                sendMessage()
            }
        }
    }


    // function for display snackBar for asking permission
    private fun showSnackBar() {
        Snackbar.make(
            binding.coordinator,
            "Permission is needed for this feature to work",
            Snackbar.LENGTH_LONG
        ).setAction(
            "Grant"
        ) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }.show()
    }

    // function for show fake item with progress bar while record uploads
    private fun showPlaceholderRecord() {

    }

    // function for start recording
    private fun startRecording() {
        //name of the file where record will be stored
        val fileName = "${requireActivity().externalCacheDir?.absolutePath}/audiorecord.3gp"

        recorder = MediaRecorder().apply {
            // set mic as recorder tool
            setAudioSource(MediaRecorder.AudioSource.MIC)
            // set output format
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            /**
             * Sets the audio encoder to be used for recording.
             * If this method is not called, the output file will not contain an audio track
             */
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                // Prepares the recorder to begin capturing and encoding data
                prepare()

            } catch (e: IOException) {
                Log.d(TAG, "startRecording Exception: ${e.message}")
                Toast.makeText(requireContext(), "${e.message}", Toast.LENGTH_SHORT).show()
            }

            // Begins capturing and encoding data to the file specified
            start()
            recordStart = Date().time
        }

    }

    // function for stop record voice
    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
            recorder = null
        }

        recordDuration = Date().time - recordStart
    }

    private fun sendMessage() {

    }


    companion object {
        private const val TAG = "ChatFragment"
//        fun newInstance() = ChatFragment()

        const val SELECT_CHAT_IMAGE_REQUEST = 3
        const val CHOOSE_FILE_REQUEST = 4
    }


}