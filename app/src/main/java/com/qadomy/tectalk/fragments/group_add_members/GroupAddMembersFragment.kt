package com.qadomy.tectalk.fragments.group_add_members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qadomy.tectalk.R

class GroupAddMembersFragment : Fragment() {

    companion object {
        fun newInstance() = GroupAddMembersFragment()
    }

    private lateinit var viewModel: GroupAddMembersViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.group_add_members_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GroupAddMembersViewModel::class.java)
        // TODO: Use the ViewModel
    }

}