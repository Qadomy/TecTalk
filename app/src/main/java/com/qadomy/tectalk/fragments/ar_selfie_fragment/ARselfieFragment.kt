package com.qadomy.tectalk.fragments.ar_selfie_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qadomy.tectalk.R

class ARselfieFragment : Fragment() {

    companion object {
        fun newInstance() = ARselfieFragment()
    }

    private lateinit var viewModel: ARselfieViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.a_rselfie_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ARselfieViewModel::class.java)
        // TODO: Use the ViewModel
    }

}