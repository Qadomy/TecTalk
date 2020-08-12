package com.qadomy.tectalk.fragments.chat_fragment

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.qadomy.tectalk.R
import com.qadomy.tectalk.adapter.ChatAdapter
import com.qadomy.tectalk.adapter.MessageClickListener
import com.qadomy.tectalk.databinding.ChatFragmentBinding
import com.qadomy.tectalk.model.*
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import com.qadomy.tectalk.utils.event_buses.PermissionEvent
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.attachment_layout.view.*
import org.greenrobot.eventbus.EventBus
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


    // chat adapter
    private val adapter: ChatAdapter by lazy {
        ChatAdapter(context, object : MessageClickListener {
            override fun onMessageClick(position: Int, message: Message) {
                //if clicked item is image open in full screen with pinch to zoom
                if (message.type == 1.0) {

                    binding.fullSizeImageView.visibility = View.VISIBLE

                    StfalconImageViewer.Builder<MyImage>(
                        activity!!,
                        listOf(MyImage((message as ImageMessage).uri!!)),
                        ImageLoader<MyImage> { imageView, myImage ->
                            Glide.with(activity!!)
                                .load(myImage.url)
                                .apply(RequestOptions().error(R.drawable.ic_broken_image_black_24dp))
                                .into(imageView)
                        })
                        .withDismissListener { binding.fullSizeImageView.visibility = View.GONE }
                        .show()


                }
                //show dialog confirming user want to download file then proceed to download or cancel
                else if (message.type == 2.0) {
                    //file message we should download
                    val dialogBuilder = context?.let { AlertDialog.Builder(it) }
                    dialogBuilder?.setMessage("Do you want to download clicked file?")
                        ?.setPositiveButton(
                            "yes"
                        ) { _, _ ->
                            downloadFile(message)
                        }?.setNegativeButton("cancel", null)?.show()

                } else if (message.type == 3.0) {
                    adapter.notifyDataSetChanged()
                }
            }

        })
    }

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

        // set chat adapter to recycle
        binding.recycler.adapter = adapter


        // pass messages list for recycler to show
        viewModel.loadMessages().observe(viewLifecycleOwner, Observer {
            messageList = it as MutableList<Message>
            ChatAdapter.messageList = messageList
            adapter.submitList(it)
            //scroll to last items in recycler (recent messages)
            binding.recycler.scrollToPosition(it.size - 1)
        })


        //handle click of bottomsheet items, when click on send Picture Button
        binding.bottomSheet.sendPictureButton.setOnClickListener {
            selectFromGallery()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // when click on send File Button
        binding.bottomSheet.sendFileButton.setOnClickListener {
            openFileChooser()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        //show bottom sheet
        binding.attachmentImageView.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        // observe when new record is uploaded
        viewModel.chatRecordDownloadUriMutableLiveData.observe(
            viewLifecycleOwner,
            Observer { recordUri ->
                println("observer called")
                viewModel.sendMessage(
                    RecordMessage(
                        AuthUtil.getAuthId(),
                        Timestamp(Date()),
                        3.0,
                        clickedUser.uid,
                        loggedUser.username,
                        recordDuration.toString(),
                        recordUri.toString(),
                        null,
                        null
                    )
                )
            })
    }


    // onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //select file result
        if (requestCode == CHOOSE_FILE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            val filePath = data.data

            showPlaceholderFile(filePath)

            //chat file was uploaded now store the uri with the message
            viewModel.uploadChatFileByUri(filePath).observe(this, Observer { chatFileMap ->
                viewModel.sendMessage(
                    FileMessage(
                        loggedUser.uid,
                        Timestamp(Date()),
                        2.0,
                        clickedUser.uid,
                        loggedUser.username,
                        chatFileMap["fileName"].toString(),
                        chatFileMap["downloadUri"].toString()
                    )
                )

            })

        }

        //select picture result
        if (requestCode == SELECT_CHAT_IMAGE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            //show fake item with image in recycler until image is uploaded
            showPlaceholderPhoto(data.data)

            //upload image to firebase storage
            viewModel.uploadChatImageByUri(data.data)
                .observe(this, Observer { uploadedChatImageUri ->
                    //chat image was uploaded now store the uri with the message
                    viewModel.sendMessage(
                        ImageMessage(
                            loggedUser.uid,
                            Timestamp(Date()),
                            1.0,
                            clickedUser.uid,
                            loggedUser.username,
                            uploadedChatImageUri.toString()
                        )
                    )
                })

        }
    }


    // function for choose file from device
    private fun openFileChooser() {
        Log.d(TAG, "openFileChooser: ")
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        try {
            startActivityForResult(i, CHOOSE_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "No suitable file manager was found on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // function for select image from gallery
    private fun selectFromGallery() {
        Log.d(TAG, "selectFromGallery: ")

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            SELECT_CHAT_IMAGE_REQUEST
        )
    }

    // function for download file from storage firebase
    private fun downloadFile(message: Message) {
        Log.d(TAG, "downloadFile: ")

        //check for storage permission then download if granted
        Dexter.withActivity(requireActivity())
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    //download file
                    val downloadManager =
                        activity!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = Uri.parse((message as FileMessage).uri)
                    val request = DownloadManager.Request(uri)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        uri.lastPathSegment
                    )
                    downloadManager.enqueue(request)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    //notify parent activity that permission denied to show toast for manual permission giving
                    showSnackBar()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    //notify parent activity that permission denied to show toast for manual permission giving
                    EventBus.getDefault().post(PermissionEvent())
                }
            }).check()
    }

    // function for handle record
    private fun handleRecord() {
        Log.d(TAG, "handleRecord: ")

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
        Log.d(TAG, "showPlaceholderRecord: ")

        //show fake item with progress bar while record uploads
        messageList.add(
            RecordMessage(
                AuthUtil.getAuthId(),
                null,
                8.0,
                null,
                null,
                null,
                null,
                null,
                null
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    private fun showPlaceholderFile(data: Uri?) {
        Log.d(TAG, "showPlaceholderFile: ")

        messageList.add(
            FileMessage(
                AuthUtil.getAuthId(),
                null,
                2.0,
                clickedUser.uid,
                loggedUser.username,
                data.toString(),
                data?.path.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    private fun showPlaceholderPhoto(data: Uri?) {
        Log.d(TAG, "showPlaceholderPhoto: ")

        messageList.add(
            ImageMessage(
                AuthUtil.getAuthId(),
                null,
                1.0,
                clickedUser.uid,
                loggedUser.username,
                data.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }


    // function for start recording
    private fun startRecording() {
        Log.d(TAG, "startRecording: ")

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
        Log.d(TAG, "stopRecording: ")

        recorder?.apply {
            stop()
            release()
            recorder = null
        }

        recordDuration = Date().time - recordStart
    }

    // function for send message
    private fun sendMessage() {
        Log.d(TAG, "sendMessage: ")

        if (binding.messageEditText.text.isEmpty()) {
            Toast.makeText(context, getString(R.string.empty_message), Toast.LENGTH_LONG).show()
            return
        }
        viewModel.sendMessage(
            TextMessage(
                loggedUser.uid,
                Timestamp(Date()),
                0.0,
                clickedUser.uid,
                loggedUser.username,
                binding.messageEditText.text.toString()
            )
        )

        binding.messageEditText.setText("")
    }


    companion object {
        private const val TAG = "ChatFragment"
        fun newInstance() = ChatFragment()

        const val SELECT_CHAT_IMAGE_REQUEST = 3
        const val CHOOSE_FILE_REQUEST = 4
    }


}