package com.qadomy.tectalk.fragments.group_info

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.createChooser
import android.graphics.Bitmap
import android.graphics.Color.GREEN
import android.graphics.Color.parseColor
import android.os.Bundle
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
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
import com.google.gson.Gson
import com.qadomy.tectalk.R.drawable.anonymous_profile
import com.qadomy.tectalk.R.drawable.loading_animation
import com.qadomy.tectalk.R.id.action_groupFragment_to_differentUserProfileFragment
import com.qadomy.tectalk.R.id.action_profileFragment_to_findUserFragment
import com.qadomy.tectalk.R.string.*
import com.qadomy.tectalk.databinding.GroupInfoFragmentBinding
import com.qadomy.tectalk.fragments.profile.ProfileFragment.Companion.REQUEST_IMAGE_CAPTURE
import com.qadomy.tectalk.fragments.profile.ProfileFragment.Companion.SELECT_PROFILE_IMAGE_REQUEST
import com.qadomy.tectalk.model.GroupName
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_GROUP
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.event_buses.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_profile_picture.view.*
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream

class GroupInfoFragment : Fragment() {

    // binding
    private var _binding: GroupInfoFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    // view model
    private lateinit var viewModel: GroupInfoViewModel
    private lateinit var sharedViewModel: SharedViewModel


    // adapter
    lateinit var adapter: MembersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title Group Information
        activity?.title = getString(group_information)

        // data binding
        _binding = GroupInfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // hide BottomNavigationView
        activity?.navView?.visibility = GONE

        viewModel = ViewModelProvider(requireActivity()).get(GroupInfoViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        //setup bottom sheet
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        //get user from shared preferences
//        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
//        val json: String? = mPrefs.getString(CLICKED_GROUP, null)
        val group = gson.fromJson(arguments?.getString(CLICKED_GROUP), GroupName::class.java)

        //show user name & email & bio
        binding.descriptionTextView.text = group.description ?: getString(no_description_yet)
        binding.groupName.text = group.group_name


        //download profile photo
        setProfileImage(group.imageurl)

        //create adapter and handle recycle item click callback
        adapter = MembersAdapter(object :
            MembersAdapter.ItemClickCallback {
            override fun onItemClicked(user: User) {

                val clickedUserString = gson.toJson(user)

                val bundle = bundleOf(
                    CLICKED_USER to clickedUserString
                )

                findNavController().navigate(
                    action_groupFragment_to_differentUserProfileFragment,
                    bundle
                )
            }
        })

        //load friends of logged in user and show in recycler
        sharedViewModel.loadMembers(group).observe(viewLifecycleOwner, Observer {
            //hide loading
            binding.loadingFriendsImageView.visibility = GONE
            if (it != null) {
                binding.friendsLayout.visibility = VISIBLE
                binding.noFriendsLayout.visibility = GONE
                showFriendsInRecycler(it)
            } else {
                binding.friendsLayout.visibility = GONE
                binding.noFriendsLayout.visibility = VISIBLE
                binding.addFriendsButton.setOnClickListener {
                    this@GroupInfoFragment.findNavController()
                        .navigate(action_profileFragment_to_findUserFragment)
                }
            }

        })


        // handle bottom sheet attachment layout
        binding.bottomSheet.cameraButton.setOnClickListener {
            openCamera()
        }
        binding.bottomSheet.galleryButton.setOnClickListener {
            selectFromGallery()
        }
        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        //show selection bottom sheet when those buttons clicked
        binding.profileImage.setOnClickListener { selectProfilePicture() }
        binding.cameraImageView.setOnClickListener { selectProfilePicture() }


        //edit bio handle click
        binding.editTextview.setOnClickListener {
            if (binding.editTextview.text == getString(edit)) {
                //show edit text to allow user to edit bio and change text view text to submit
                binding.editTextview.text = getString(submit)
                binding.editTextview.setTextColor(GREEN)
                binding.descriptionTextView.visibility = GONE
                binding.newBioEditText.visibility = VISIBLE


            } else if (binding.editTextview.text == getString(submit)) {
                //hide edit text and upload changes to user document
                binding.editTextview.text = getString(edit)
                binding.editTextview.setTextColor(parseColor("#b39ddb"))
                binding.descriptionTextView.visibility = VISIBLE
                binding.descriptionTextView.text = binding.newBioEditText.text
                binding.newBioEditText.visibility = GONE
                EventBus.getDefault().post(KeyboardEvent())
                //upload bio to user document
                viewModel.updateBio(binding.newBioEditText.text.toString())

                //hide keyboard
                EventBus.getDefault().post(KeyboardEvent())
            }
        }
    }


    // function for open gallery on device
    private fun selectFromGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = ACTION_GET_CONTENT
        startActivityForResult(
            createChooser(intent, getString(select_picture)),
            SELECT_PROFILE_IMAGE_REQUEST
        )
    }

    // function for open camera on device
    private fun openCamera() {
        Intent(ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(
                    takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
            }
        }
    }

    private fun selectProfilePicture() {
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    private fun showFriendsInRecycler(it: List<User>) {
        adapter.setDataSource(it)
        binding.friendsRecycler.adapter = adapter
        binding.friendsCountTextView.text = it.size.toString()
    }

    // function for set image using Glide
    private fun setProfileImage(groupImage: String?) {
        Log.d(TAG, " image loading...")
        Glide.with(this).load(groupImage)
            .apply(
                RequestOptions()
                    .placeholder(loading_animation)
                    .error(anonymous_profile)
                    .circleCrop()
            )
            .into(binding.profileImage)
    }


    /** region onActivityResult */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        //result of selecting image from gallery
        if (requestCode == SELECT_PROFILE_IMAGE_REQUEST &&
            data != null &&
            resultCode == AppCompatActivity.RESULT_OK
        ) {

            //set selected image in profile image view and upload it
            //upload image
            viewModel.uploadProfileImageByUri(data.data)
        }

        //result of taking camera image
        if (requestCode == REQUEST_IMAGE_CAPTURE &&
            resultCode == AppCompatActivity.RESULT_OK
        ) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            val byteArrayOutputStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            //upload image
            viewModel.uploadImageAsByteArray(byteArray)
        }

        //show loading layout while uploading
        viewModel.uploadImageLoadStateMutableLiveData.observe(this, Observer {
            setProfileImageLoadUi(it)
        })


        //set new image in profile image view
        viewModel.newImageUriMutableLiveData.observe(this, Observer {
            setProfileImage(it.toString())
        })

    } // endregion onActivityResult

    private fun setProfileImageLoadUi(it: LoadState?) {
        when (it) {
            LoadState.SUCCESS -> {
                binding.uploadProgressBar.visibility = GONE
                binding.uploadText.visibility = GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.FAILURE -> {
                binding.uploadProgressBar.visibility = GONE
                binding.uploadText.visibility = GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.LOADING -> {
                binding.uploadProgressBar.visibility = VISIBLE
                binding.uploadText.visibility = GONE
                binding.profileImage.alpha = .5f

            }
        }
    }

    companion object {
        fun newInstance() = GroupInfoFragment()
        private const val TAG = "GroupInfoFragment"
    }
}