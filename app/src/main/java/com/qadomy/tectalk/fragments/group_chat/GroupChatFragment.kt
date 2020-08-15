package com.qadomy.tectalk.fragments.group_chat

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.auth.User
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.qadomy.tectalk.R
import com.qadomy.tectalk.R.drawable.ic_broken_image_black_24dp
import com.qadomy.tectalk.R.id.*
import com.qadomy.tectalk.R.string.*
import com.qadomy.tectalk.databinding.GroupChatFragmentBinding
import com.qadomy.tectalk.fragments.chat_fragment.gson
import com.qadomy.tectalk.model.*
import com.qadomy.tectalk.utils.Common.CLICKED_GROUP
import com.qadomy.tectalk.utils.event_buses.PermissionEvent
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus

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

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE

        // init view model
//        viewModel = ViewModelProvider(requireActivity()).get(GroupChatViewModel::class.java)

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


    companion object {
        fun newInstance() = GroupChatFragment()

        const val SELECT_CHAT_IMAGE_REQUEST = 3
        const val CHOOSE_FILE_REQUEST = 4

        private const val TAG = "GroupChatFragment"
    }
}