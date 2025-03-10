package com.qadomy.tectalk.fragments.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.ProfileFragmentBinding
import com.qadomy.tectalk.model.User
import com.qadomy.tectalk.ui.main_activity.SharedViewModel
import com.qadomy.tectalk.utils.Common.CLICKED_USER
import com.qadomy.tectalk.utils.Common.LOGGED_USER
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.event_buses.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_profile_picture.view.*
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    // binding
    private var _binding: ProfileFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    lateinit var adapter: FriendsAdapter

    // view model
    private lateinit var viewModel: ProfileViewModel
    private lateinit var sharedViewModel: SharedViewModel

    /** onCreateView */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title name Profile
        activity?.title = getString(R.string.my_profile)


        // data binding
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    } // end onCreateView

    /** onActivityCreated */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE

        // init view models
        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        //setup bottom sheet
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        //get user from shared preferences
        val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)

        //show user name & email & bio
        binding.bioTextView.text = loggedUser.bio ?: "No bio yet"
        binding.email.text = loggedUser.email
        binding.name.text = loggedUser.username

        //download profile photo
        setProfileImage(loggedUser.profile_picture_url)


        //create adapter and handle recycle item click callback
        adapter = FriendsAdapter(object :
            FriendsAdapter.ItemClickCallback {
            override fun onItemClicked(user: User) {

                val clickedUserString = gson.toJson(user)

                val bundle = bundleOf(
                    CLICKED_USER to clickedUserString
                )

                findNavController().navigate(
                    R.id.action_profileFragment_to_differentUserProfileFragment,
                    bundle
                )
            }
        })

        // load friends of logged in user and show in recycler
        sharedViewModel.loadFriends(loggedUser)
            .observe(viewLifecycleOwner, Observer { friendsList ->
                //hide loading
                binding.loadingFriendsImageView.visibility = View.GONE
                if (friendsList != null) {
                    binding.friendsLayout.visibility = View.VISIBLE
                    binding.noFriendsLayout.visibility = View.GONE
                    showFriendsInRecycler(friendsList)
                } else {
                    binding.friendsLayout.visibility = View.GONE
                    binding.noFriendsLayout.visibility = View.VISIBLE
                    binding.addFriendsButton.setOnClickListener {
                        this@ProfileFragment.findNavController()
                            .navigate(R.id.action_profileFragment_to_findUserFragment)
                    }
                }

            })


        // open camera when click on camera button
        binding.bottomSheet.cameraButton.setOnClickListener {
            openCamera()
        }

        // open Gallery when click on gallery button
        binding.bottomSheet.galleryButton.setOnClickListener {
            selectFromGallery()
        }

        // when click on hide button
        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        // when click on profile image, and edit image
        binding.profileImage.setOnClickListener { selectProfilePicture() }
        binding.cameraImageView.setOnClickListener { selectProfilePicture() }


        //edit bio handle click
        binding.editTextview.setOnClickListener {
            if (binding.editTextview.text == getString(R.string.edit)) {
                //show edit text to allow user to edit bio and change text view text to submit
                binding.editTextview.text = getString(R.string.submit)
                binding.editTextview.setTextColor(Color.GREEN)
                binding.bioTextView.visibility = View.GONE
                binding.newBioEditText.visibility = View.VISIBLE


            } else if (binding.editTextview.text == getString(R.string.submit)) {
                //hide edit text and upload changes to user document
                binding.editTextview.text = getString(R.string.edit)
                binding.editTextview.setTextColor(Color.parseColor("#b39ddb"))
                binding.bioTextView.visibility = View.VISIBLE
                binding.bioTextView.text = binding.newBioEditText.text
                binding.newBioEditText.visibility = View.GONE
                EventBus.getDefault().post(KeyboardEvent())

                //upload bio to user document
                viewModel.updateBio(binding.newBioEditText.text.toString())

                //hide keyboard
                EventBus.getDefault().post(KeyboardEvent())
            }
        }

    } // end onActivityCreated

    /**
     * region onActivityResult
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        //result of selecting image from gallery
        if (requestCode == SELECT_PROFILE_IMAGE_REQUEST && data != null &&
            resultCode == AppCompatActivity.RESULT_OK
        ) {
            /** set selected image in profile image view and upload it
            upload image...
             */
            viewModel.uploadProfileImageByUri(data.data)
        }


        //result of taking camera image
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
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

    }// endregion onActivityResult


    // function for handle loading layout
    private fun setProfileImageLoadUi(it: LoadState?) {
        when (it) {
            LoadState.SUCCESS -> {
                binding.uploadProgressBar.visibility = View.GONE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.FAILURE -> {
                binding.uploadProgressBar.visibility = View.GONE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.LOADING -> {
                binding.uploadProgressBar.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = .5f
            }
        }
    }

    // function for select profile image from
    private fun selectProfilePicture() {
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // function for open Gallery when click on gallery button
    private fun selectFromGallery() {
        Log.d(TAG, "selectFromGallery: ")

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            SELECT_PROFILE_IMAGE_REQUEST
        )
    }

    // function for open camera when click on camera button
    private fun openCamera() {
        Log.d(TAG, "openCamera: ")

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(
                    takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
            }
        }
    }

    // function for show friends list in recycle view
    private fun showFriendsInRecycler(it: List<User>) {
        adapter.setDataSource(it)
        binding.friendsRecycler.adapter = adapter
        binding.friendsCountTextView.text = it.size.toString()
    }

    // function for set profile image using Glide
    private fun setProfileImage(profilePictureUrl: String?) {
        Glide.with(this).load(profilePictureUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loading_animation)
                    .error(R.drawable.anonymous_profile)
                    .circleCrop()
            )
            .into(binding.profileImage)
    }


    companion object {
        private const val TAG = "ProfileFragment"
        fun newInstance() = ProfileFragment()

        const val SELECT_PROFILE_IMAGE_REQUEST = 5
        const val REQUEST_IMAGE_CAPTURE = 6
    }
}