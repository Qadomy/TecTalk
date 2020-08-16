package com.qadomy.tectalk.fragments.group_chat

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.createChooser
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
import android.util.Log.d
import android.view.*
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import com.qadomy.tectalk.R.animator.increase_size
import com.qadomy.tectalk.R.animator.regain_size
import com.qadomy.tectalk.R.drawable.*
import com.qadomy.tectalk.R.id.*
import com.qadomy.tectalk.R.string.*
import com.qadomy.tectalk.databinding.GroupChatFragmentBinding
import com.qadomy.tectalk.fragments.chat_fragment.gson
import com.qadomy.tectalk.model.*
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.Common.CLICKED_GROUP
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import com.qadomy.tectalk.utils.event_buses.PermissionEvent
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.attachment_layout.view.*
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.util.*

class GroupChatFragment : Fragment() {

    // binding
    private var _binding: GroupChatFragmentBinding? = null
    private val binding get() = _binding!!

    // view model, view Model Factory
    private lateinit var viewModel: GroupChatViewModel
    private lateinit var viewModelFactory: GroupChatViewModelFactory

    // record
    private var recordStart = 0L
    private var recordDuration = 0L
    private var recorder: MediaRecorder? = null
    var isRecording = false //whether is recoding now or not
    var isRecord = true //whether it is text message or record


    // logged user, clicked group
    private lateinit var loggedUser: User
    private lateinit var clickedGroup: GroupName

    // message
    private var messageList = mutableListOf<Message>()

    // adapter
    private val adapter: ChatAdapter by lazy {
        ChatAdapter(context, object : MessageClickListener {
            override fun onMessageClick(position: Int, message: Message) {
                when (message.type) {
                    //if clicked item is image open in full screen with pinch to zoom
                    1.0 -> {
                        // display full size image
                        binding.fullSizeImageView.visibility = View.VISIBLE

                        // apply zoom in image
                        StfalconImageViewer.Builder<MyImage>(
                            requireContext(),
                            listOf(MyImage((message as ImageMessage).uri!!)),
                            ImageLoader<MyImage> { imageView, myImage ->
                                Glide.with(activity!!)
                                    .load(myImage.url)
                                    .apply(RequestOptions().error(ic_broken_image_black_24dp))
                                    .into(imageView)
                            })
                            .withDismissListener {
                                binding.fullSizeImageView.visibility = View.GONE
                            }
                            .show()
                    }
                    //show dialog confirming user want to download file then proceed to download or cancel
                    2.0 -> {
                        // file message we should download
                        val dialogBuilder = requireActivity().let { AlertDialog.Builder(it) }
                        dialogBuilder.setMessage(getString(do_you_want_download_file))
                            ?.setPositiveButton(
                                getString(yes)
                            ) { _, _ ->
                                downloadFile(message)
                            }?.setNegativeButton("cancel", null)?.show()

                    }
                    // Notify any registered observers that the data set has changed.
                    3.0 -> {
                        adapter.notifyDataSetChanged()
                    }
                }

            }
        })
    }


    // bottom sheet behaviour
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    /**
     * onCreateView
     * */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // enable menu
        setHasOptionsMenu(true)

        // data binding
        _binding = GroupChatFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * onActivityCreated
     * */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide pin bar
        binding.pinbar.visibility = View.GONE

        // setup Bottom sheet, for attachment
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        //set record view
        handleRecord()

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE


        //get logged user from shared preferences
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        loggedUser = gson.fromJson(json, User::class.java)


        //get receiver data from contacts fragment(NOTE:IF NAVIGATING FROM FCM-NOTIFICATION USER ONLY HAS id,username)
        clickedGroup = gson.fromJson(arguments?.getString(CLICKED_GROUP), GroupName::class.java)


        // set action bar title group name
        activity?.title = clickedGroup.group_name


        //user view model factory to pass ids on creation of view model
        viewModelFactory = GroupChatViewModelFactory(loggedUser.uid, clickedGroup.group_name!!)
        viewModel =
            ViewModelProvider(
                requireActivity(),
                viewModelFactory
            ).get(GroupChatViewModel::class.java)


        // Move layouts up when soft keyboard is shown
        // TODO: 8/16/20 [DEPRECATED] SOFT_INPUT_ADJUST_RESIZE
        requireActivity().window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)


        // send message on keyboard done click
        binding.messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }


        // set recycle adapter
        binding.recycler.adapter = adapter


        // pass messages list for recycler to show
        viewModel.loadMessage().observe(viewLifecycleOwner, Observer { mMessagesList ->
            messageList = mMessagesList as MutableList<Message>
            ChatAdapter.messageList = messageList
            adapter.submitList(mMessagesList)

            /**
             * scroll to last items in recycler (recent messages)
             *  */
            binding.recycler.scrollToPosition(mMessagesList.size - 1)
        })


        // show bottom sheet, when click on attachment button
        binding.attachmentImageView.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        // handle click of bottom sheet items
        binding.bottomSheet.sendPictureButton.setOnClickListener {
            selectFromGallery()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        binding.bottomSheet.sendFileButton.setOnClickListener {
            openFileChooser()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        // observe when new record is uploaded
        viewModel.chatRecordDownloadUriMutableLiveData.observe(
            viewLifecycleOwner,
            Observer { recordUri ->
                d(TAG, "onActivityCreated: observer called, when new record is uploaded")
                viewModel.sendMessage(
                    RecordMessage(
                        AuthUtil.getAuthId(),
                        Timestamp(Date()),
                        3.0,
                        clickedGroup.group_name,
                        loggedUser.username,
                        recordDuration.toString(),
                        recordUri.toString(),
                        null,
                        null
                    )
                )
            })
    }


    // function for open file manager in device for choose file
    private fun openFileChooser() {
        val i = Intent(ACTION_GET_CONTENT)
        i.type = "*/*"
        try {
            startActivityForResult(i, CHOOSE_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                getString(no_suitable_file_on_this_device),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // function for open gallery in device and choose image
    private fun selectFromGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = ACTION_GET_CONTENT
        startActivityForResult(
            createChooser(intent, getString(select_picture)),
            SELECT_CHAT_IMAGE_REQUEST
        )
    }

    // function for handle fab button for mic and send icon
    private fun handleRecord() {

        // change fab button depend on text message if empty or not
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                isRecord = if (text.isNullOrEmpty()) {
                    // text is empty, set mic icon -> set record true
                    binding.recordFab.setImageResource(mic)
                    true
                } else {
                    // there is text message, set send icon -> set record false
                    binding.recordFab.setImageResource(sendicon)
                    false
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        // handle when click on fab button
        binding.recordFab.setOnClickListener {
            if (isRecord) {
                // click in mic button, start recording
                if (isRecording) {
                    // recording voice
                    // change size and color or button so user know its finished recording
                    val regainer = AnimatorInflater.loadAnimator(
                        requireContext(),
                        regain_size
                    ) as AnimatorSet
                    regainer.setTarget(binding.recordFab)
                    regainer.start()

                    // change fab background
                    binding.recordFab.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#b39ddb"))

                    // stop recording
                    stopRecording()
                    showPlaceholderRecord()

                    // upload recording to database
                    viewModel.uploadRecord("${requireActivity().externalCacheDir?.absolutePath}/audiorecord.3gp")
                    Toast.makeText(
                        requireContext(),
                        getString(finished_recording),
                        Toast.LENGTH_SHORT
                    ).show()

                    // set opposite recording
                    isRecording = !isRecording

                } else {
                    // no permission
                    Dexter.withActivity(requireActivity())
                        .withPermission(Manifest.permission.RECORD_AUDIO)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                // change size and color or button so user know its recording
                                val increaser = AnimatorInflater.loadAnimator(
                                    context,
                                    increase_size
                                ) as AnimatorSet
                                increaser.setTarget(binding.recordFab)
                                increaser.start()
                                binding.recordFab.backgroundTintList =
                                    ColorStateList.valueOf(Color.parseColor("#EE4B4B"))

                                //start recording
                                startRecording()
                                Toast.makeText(
                                    requireContext(),
                                    getString(recording),
                                    Toast.LENGTH_SHORT
                                ).show()
                                isRecording = !isRecording
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                p0: PermissionRequest?,
                                token: PermissionToken?
                            ) {
                                token?.continuePermissionRequest()
                                //notify parent activity that permission denied to show toast for manual permission giving
                                showSnackBar()
                            }

                            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                //notify parent activity that permission denied to show toast for manual permission giving
                                showSnackBar()
                            }
                        }).check()

                }

            } else {
                // click on send button -> send message
                sendMessage()
            }
        }
    }

    // function for start recording
    private fun startRecording() {
        // name of the file where record will be stored
        val fileName = "${requireActivity().externalCacheDir?.absolutePath}/audiorecord.3gp"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()

            } catch (e: IOException) {
                d(TAG, "startRecording: ${e.message}")
            }

            start()
            recordStart = Date().time
        }
    }

    // function for show fake item with [@@AUDIO] in recycler until image is uploaded
    private fun showPlaceholderRecord() {
        // show fake item with progress bar while record uploads
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

    // function for show fake item with [@@Link FILE] in recycler until image is uploaded
    private fun showPlaceholderFile(data: Uri?) {
        messageList.add(
            FileMessage(
                AuthUtil.getAuthId(),
                null,
                2.0,
                clickedGroup.group_name,
                loggedUser.username,
                data.toString(),
                data?.path.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    // function for show fake item with [@@IMAGE] in recycler until image is uploaded
    private fun showPlaceholderPhoto(data: Uri?) {
        messageList.add(
            ImageMessage(
                AuthUtil.getAuthId(),
                null,
                1.0,
                clickedGroup.group_name,
                loggedUser.username,
                data.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    // function for stop recording
    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
            recorder = null
        }

        recordDuration = Date().time - recordStart
    }

    // function for send message and save it in database fire store in firebase
    private fun sendMessage() {
        // check if text message empty
        if (binding.messageEditText.text.isEmpty()) {
            Toast.makeText(requireContext(), getString(empty_message), Toast.LENGTH_LONG).show()
            return
        }
        viewModel.sendMessage(
            TextMessage(
                loggedUser.uid,
                Timestamp(Date()),
                0.0,
                clickedGroup.group_name,
                loggedUser.username,
                binding.messageEditText.text.toString()
            )
        )

        // empty text message after send message
        binding.messageEditText.setText("")
    }

    // download file media to storage in device
    private fun downloadFile(message: Message) {
        // check for storage permission then download if granted
        Dexter.withActivity(requireActivity())
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    // download file
                    val downloadManager =
                        requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

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
                    p0: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    //notify parent activity that permission denied to show toast for manual permission giving
                    showSnackBar()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    //notify parent activity that permission denied to show toast for manual permission giving
                    EventBus.getDefault().post(PermissionEvent())
                }
            }).check()
    }

    // function for show snack bar when denied permission for storage permission
    private fun showSnackBar() {
        Snackbar.make(requireView(), getString(permission_required), Snackbar.LENGTH_LONG)
            .setAction(getString(grant)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }.show()
    }


    /*** region Menu */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)


        inflater.inflate(R.menu.chat_menu_room, menu)
        val menuItem = menu.findItem(action_incoming_requests)
        val actionView = menuItem?.actionView


        actionView?.setOnClickListener { onOptionsItemSelected(menuItem) }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        action_addmembers -> {
            Toast.makeText(
                requireContext(),
                "Number of members  are ${clickedGroup.chat_members_in_group?.size.toString()} ",
                Toast.LENGTH_SHORT
            ).show()
            val clickedGroup = gson.toJson(clickedGroup)
            findNavController().navigate(
                action_groupChatFragment_to_groupAddMembersFragment, bundleOf(
                    CLICKED_GROUP to clickedGroup
                )
            )
            true
        }
        action_information -> {
            Toast.makeText(
                requireContext(),
                "Number of members  are ${clickedGroup.chat_members_in_group?.size.toString()} ",
                Toast.LENGTH_SHORT
            ).show()
            val clickedGroup = gson.toJson(clickedGroup)
            findNavController().navigate(
                action_groupChatFragment_to_groupInfoFragment, bundleOf(
                    CLICKED_GROUP to clickedGroup
                )
            )

            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }
    // endregion


    /*** region onActivityResult */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //select file result
        if (requestCode == CHOOSE_FILE_REQUEST &&
            data != null &&
            resultCode == AppCompatActivity.RESULT_OK
        ) {
            val filePath = data.data
            showPlaceholderFile(filePath)

            //chat file was uploaded now store the uri with the message
            viewModel.uploadChatFileByUri(filePath).observe(this, Observer { chatFileMap ->
                viewModel.sendMessage(
                    FileMessage(
                        loggedUser.uid,
                        Timestamp(Date()),
                        2.0,
                        clickedGroup.group_name,
                        loggedUser.username,
                        chatFileMap["fileName"].toString(),
                        chatFileMap["downloadUri"].toString()
                    )
                )

            })
        }


        //select picture result
        if (requestCode == SELECT_CHAT_IMAGE_REQUEST &&
            data != null &&
            resultCode == AppCompatActivity.RESULT_OK
        ) {

            // show fake item with image in recycler until image is uploaded
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
                            clickedGroup.group_name,
                            loggedUser.username,
                            uploadedChatImageUri.toString()
                        )
                    )
                })

        }

    } // endregion onActivityResult


    companion object {
        fun newInstance() = GroupChatFragment()

        const val SELECT_CHAT_IMAGE_REQUEST = 3
        const val CHOOSE_FILE_REQUEST = 4

        private const val TAG = "GroupChatFragment"
    }
}