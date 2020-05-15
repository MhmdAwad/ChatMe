package com.mhmdawad.chatme.ui.fragments.main_fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter

class FragmentTabsAdapter(fm: FragmentManager): FragmentPagerAdapter(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragmentList = ArrayList<Fragment>()
    private val fragmentNameList = ArrayList<String>()

    fun addFragment(fragment: Fragment, name: String){
        fragmentList.add(fragment)
        fragmentNameList.add(name)
    }

    override fun getItem(position: Int): Fragment = fragmentList[position]

    override fun getCount(): Int = fragmentList.size

    override fun getPageTitle(position: Int): CharSequence?  = fragmentNameList[position]
}