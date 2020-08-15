package com.qadomy.tectalk.fragments.group_create_new

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.CreateGroupFragmentBinding
import com.qadomy.tectalk.fragments.chat_fragment.gson
import com.qadomy.tectalk.model.GroupName
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.LoadState
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.issue_layout.view.*
import java.util.regex.Pattern

class CreateGroupFragment : Fragment() {

    // binding
    private var _binding: CreateGroupFragmentBinding? = null
    private val binding get() = _binding!!

    // VIEW MODEL
    private lateinit var viewModel: CreateGroupViewModel

    private lateinit var pattern: Pattern

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // set action bar title name Group
        activity?.title = getString(R.string.create_new_group)

        // data binding
        _binding = CreateGroupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // init view model
        viewModel = ViewModelProvider(requireActivity()).get(CreateGroupViewModel::class.java)

        // hide BottomNavigationView
        activity?.navView?.visibility = View.GONE

        //regex pattern to check email format
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$"
        pattern = Pattern.compile(emailRegex)


        // when click on create group button
        binding.createGroupButton.setOnClickListener {
            createGroup()
        }


        //hide issue layout on x icon click
        binding.issueLayout.cancelImage.setOnClickListener {
            binding.issueLayout.visibility = View.GONE
        }

        //show proper loading/error ui
        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            when (it!!) {
                LoadState.LOADING -> {
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.issueLayout.visibility = View.GONE
                }
                LoadState.SUCCESS -> {
                    binding.loadingLayout.visibility = View.GONE
                    binding.issueLayout.visibility = View.GONE
                }
                LoadState.FAILURE -> {
                    binding.loadingLayout.visibility = View.GONE
                    binding.issueLayout.visibility = View.VISIBLE
                    binding.issueLayout.textViewIssue.text = ErrorMessage.errorMessage
                }
            }
        })


        //sign up on keyboard done click when focus is on passwordEditText
        binding.newgroupDescriptionEditText.setOnEditorActionListener { _, _, _ ->
            createGroup()
            true
        }
    }


    // function for create new group
    private fun createGroup() {

        binding.groupName.isErrorEnabled = false
        binding.description.isErrorEnabled = false

        // if group name less than 4 character
        if (binding.groupName.editText!!.text.length < 4) {
            binding.groupName.error = "Group name should be at least 4 characters"
            return
        }

        //check if email is empty_box or wrong format
        if (binding.description.editText!!.text.isEmpty()) {
            binding.description.error = "Please Enter Group Description."
            return
        }

        //email and pass are matching requirements now we can register to firebase auth
        viewModel.createdGroupFlag.observe(viewLifecycleOwner, Observer { flag ->

            if (flag) {
                this.findNavController().popBackStack()
            }
        })

        //get user data
        viewModel.loggedUserMutableLiveData.observe(viewLifecycleOwner, Observer { loggedUser ->
            //save logged user data in shared pref to use in other fragments
            val mPrefs: SharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
            val json = gson.toJson(loggedUser)
            prefsEditor.putString("loggedUser", json)
            prefsEditor.apply()
            val groupName = GroupName()
            groupName.description = binding.description.editText!!.text.toString()
            groupName.group_name = binding.groupName.editText!!.text.toString()
            groupName.chat_members_in_group = listOf(loggedUser.uid.toString())


            viewModel.createGroup(
                loggedUser,
                groupName
            )
        })
    }

    companion object {
        fun newInstance() = CreateGroupFragment()
    }
}