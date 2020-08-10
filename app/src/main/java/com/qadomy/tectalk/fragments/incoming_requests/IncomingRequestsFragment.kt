package com.qadomy.tectalk.fragments.incoming_requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qadomy.tectalk.R

class IncomingRequestsFragment : Fragment() {

    companion object {
        fun newInstance() = IncomingRequestsFragment()
    }

    private lateinit var viewModel: IncomingRequestsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.incoming_requests_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(IncomingRequestsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}