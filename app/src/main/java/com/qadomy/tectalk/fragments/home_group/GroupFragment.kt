package com.qadomy.tectalk.fragments.home_group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qadomy.tectalk.R
import kotlinx.android.synthetic.main.activity_main.*

class GroupFragment : Fragment() {

    companion object {
        fun newInstance() = GroupFragment()
    }

    private lateinit var viewModel: GroupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // display BottomNavigationView
        activity?.navView?.visibility = View.VISIBLE

        return inflater.inflate(R.layout.group_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GroupViewModel::class.java)
        // TODO: Use the ViewModel
    }

}