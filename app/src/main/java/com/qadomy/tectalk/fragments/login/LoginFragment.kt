package com.qadomy.tectalk.fragments.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.LoginFragmentBinding
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.ErrorMessage
import com.qadomy.tectalk.utils.LoadState
import com.qadomy.tectalk.utils.event_buses.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.issue_layout.view.*
import org.greenrobot.eventbus.EventBus

class LoginFragment : Fragment() {

    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // data binding
        _binding = LoginFragmentBinding.inflate(inflater, container, false)

        try {
            //check if user has previously logged in
            if (AuthUtil.firebaseAuthInstance.currentUser != null) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        } catch (e: Exception) {
            Log.d(TAG, "onCreateView: ${e.message}")
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        // hide BottomNavigationView when we in Login fragment
        activity?.navView?.visibility = View.GONE


        //Report text change to viewModel and Observe if email format is correct
        binding.emailEditText.afterTextChanged { email ->
            viewModel.isEmailFormatCorrect(email)
                .observe(viewLifecycleOwner, Observer { isEmailFormatCorrect ->
                    if (!isEmailFormatCorrect)
                    //email format is not correct
                        binding.email.error = getString(R.string.wrong_email_format)
                    else
                    // show error in email input text
                        binding.email.isErrorEnabled = false


                })
        }

        //password length must be at least 6 characters
        binding.passwordEditText.afterTextChanged {
            if (it.length < 6)
            // password size less than 6 characters
                binding.password.error = getString(R.string.password_size)
            else
            // show error in password input text
                binding.password.isErrorEnabled = false

        }

        //handle login click
        binding.loginButton.setOnClickListener {
            login()
        }


        //login on keyboard done click when focus is on passwordEditText
        binding.passwordEditText.setOnEditorActionListener { _, _, _ ->
            login()
            true
        }
    }


    // login
    private fun login() {
        EventBus.getDefault().post(KeyboardEvent())

        if (binding.email.error != null ||
            binding.password.error != null ||
            binding.email.editText!!.text.isEmpty() ||
            binding.password.editText!!.text.isEmpty()
        ) {
            //name or password doesn't match format
            Snackbar.make(
                requireView(),
                "Check email and password then retry",
                Snackbar.LENGTH_LONG
            ).show()

        } else {

            //All fields are correct we can login
            viewModel.login(
                AuthUtil.firebaseAuthInstance,
                binding.email.editText!!.text.toString(),
                binding.password.editText!!.text.toString()
            ).observe(viewLifecycleOwner, Observer {

                when (it) {
                    LoadState.SUCCESS -> {   //triggered when login with email and password is successful
                        this@LoginFragment.findNavController()
                            .navigate(R.id.action_loginFragment_to_homeFragment)
                        Toast.makeText(context, "Login successful", Toast.LENGTH_LONG).show()
                        viewModel.doneNavigating()
                    }
                    LoadState.LOADING -> {
                        // while logging display logging layout
                        binding.loadingLayout.visibility = View.VISIBLE
                        binding.issueLayout.visibility = View.GONE
                    }
                    LoadState.FAILURE -> {
                        // if logging failure display issue layout
                        binding.loadingLayout.visibility = View.GONE
                        binding.issueLayout.visibility = View.VISIBLE
                        binding.issueLayout.textViewIssue.text = ErrorMessage.errorMessage
                    }
                    else -> {

                    }
                }
            })

        }
    }

    /**
     * Extension function to simplify setting an afterTextChanged action to EditText components.
     */
    private fun TextInputEditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

    }


    companion object {
        private const val TAG = "LoginFragment"
        fun newInstance() = LoginFragment()
    }
}