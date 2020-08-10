package com.qadomy.tectalk.fragments.register

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.SignUpFragmentBinding
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.event_buses.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.issue_layout.view.*
import org.greenrobot.eventbus.EventBus
import java.util.regex.Matcher
import java.util.regex.Pattern

class SignUpFragment : Fragment() {

    private lateinit var binding: SignUpFragmentBinding
    private lateinit var pattern: Pattern
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.sign_up_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // init SignUpViewModel
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)

        //regex pattern to check email format
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$"
        pattern = Pattern.compile(emailRegex)

        // hide BottomNavigationView when we in Login fragment
        activity?.navView?.visibility = View.GONE

        //handle register click
        binding.registerButton.setOnClickListener {
            signUp()
        }

        //hide issue layout on x icon click
        binding.issueLayout.cancelImage.setOnClickListener {
            binding.issueLayout.visibility = View.GONE
        }

        //show proper loading/error ui
        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            when (it) {
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
                else -> {

                }
            }
        })


        //sign up on keyboard done click when focus is on passwordEditText
        binding.passwordEditText.setOnEditorActionListener { _, _, _ ->
            signUp()
            true
        }

    }

    private fun signUp() {
        Log.d(TAG, "signUp: ")
        EventBus.getDefault().post(KeyboardEvent())

        binding.userName.isErrorEnabled = false
        binding.email.isErrorEnabled = false
        binding.password.isErrorEnabled = false

        // check if username less than 4 characters
        if (binding.userName.editText!!.text.length < 4) {
            binding.userName.error = "User name should be at least 4 characters"
            return
        }

        // check if email is empty_box or wrong format
        if (binding.email.editText!!.text.isNotEmpty()) {
            val matcher: Matcher = pattern.matcher(binding.email.editText!!.text)
            if (!matcher.matches()) {
                binding.email.error = "Email format isn't correct."
                return
            }
        } else if (binding.email.editText!!.text.isEmpty()) {
            binding.email.error = "Email field can't be empty_box."
            return
        }


        // check if password less than 6 characters
        if (binding.password.editText!!.text.length < 6) {
            binding.password.error = "Password should be at least 6 characters"
            return
        }

        //email and pass are matching requirements now we can register to firebase auth
        viewModel.registerEmail(
            AuthUtil.firebaseAuthInstance,
            binding.email.editText!!.text.toString(),
            binding.password.editText!!.text.toString(),
            binding.userName.editText!!.text.toString()
        )


        viewModel.navigateToHomeMutableLiveData.observe(
            viewLifecycleOwner,
            Observer { navigateToHome ->
                if (navigateToHome != null && navigateToHome) {
                    this@SignUpFragment.findNavController()
                        .navigate(R.id.action_signUpFragment_to_chatFragment)
                    Snackbar.make(
                        requireView(),
                        getString(R.string.signup_successfully),
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.doneNavigating()
                }
            })
    }


    companion object {
        private const val TAG = "SignUpFragment"
//        fun newInstance() = SignUpFragment()
    }

}