package com.qadomy.tectalk.fragments.different_user_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qadomy.tectalk.R

class DifferentUserProfileFragment : Fragment() {

    companion object {
        fun newInstance() = DifferentUserProfileFragment()
    }

    private lateinit var viewModel: DifferentUserProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.different_user_profile_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DifferentUserProfileViewModel::class.java)
        // TODO: Use the ViewModel
    }

}