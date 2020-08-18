package com.qadomy.tectalk.fragments.ar_selfie_home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.ArSelfieHomeBinding
import kotlinx.android.synthetic.main.activity_main.*

class ArSelfieHome : Fragment() {

    // binding
    private var _binding: ArSelfieHomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // display BottomNavigationView
        activity?.navView?.visibility = View.VISIBLE

        // data binding
        _binding = ArSelfieHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.cdArSelfie.setOnClickListener {
            findNavController().navigate(R.id.action_ArSelfieHome_to_arSelfieFragment)
        }
        binding.cdAROnFloor.setOnClickListener {
            findNavController().navigate(R.id.action_ArSelfieHome_to_arFloorFragment)

        }
        binding.cdNormalSelfie.setOnClickListener {
            findNavController().navigate(R.id.action_ArSelfieHome_to_selfieFragment)
        }

    }
}