package com.example.fyp_fitledger

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DemographicPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = fragments.size
    var fragments: MutableList<Fragment> = mutableListOf()
    var fragmentAvailable: MutableList<Fragment> = mutableListOf()

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
        notifyItemInserted(fragments.size - 1)
    }

    fun addAvailableFragment(fragment: Fragment) {
        fragmentAvailable.add(fragment)
        notifyItemInserted(fragmentAvailable.size - 1)
    }

    fun addFragment(name: String) {
        val fragment = findFragmentReturn(name)
        if (fragment != null) {
            fragments.add(fragment)
            notifyItemInserted(fragments.size - 1)
            Log.d("DemoAdapter", "Fragment created")
        }
    }

    fun findFragmentReturn(name: String): Fragment? {
        for (fragment in fragmentAvailable) {
            if (fragment.javaClass.simpleName == name) {
                Log.d("DemoAdapter", "Fragment found")
                return fragment
            }
        }
        Log.d("DemoAdapter", "Fragment not found")
        return null
    }

    fun findFragmentIndex(name: String): Int {
        for ((index, fragment) in fragments.withIndex()) {  // Loop with index
            if (fragment.javaClass.simpleName == name)
                return index  // Return the index if found
        }
        return -1  // Return -1 if not found
    }


    //Find Fragment Exist
    fun findFragment(name: String): Boolean {
        for (fragment in fragments) {
            if (fragment.javaClass.simpleName == name)
                return true
        }
        return false
    }

    fun findFragment(n: Int): Boolean {
        if (n <= fragments.size)
            return true
        return false
    }



    // Remove a Fragment at a Specific Location
    fun removeFragmentAt(position: Int) {
        if (position in 0 until fragments.size) {
            fragments.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fragments.size - position)
            Log.d("FragmentStateAdapter", "Removed fragment at position $position")
        }
        Log.d("FragmentStateAdapter", "Fragment at position $position unfound")
    }

    // Remove a Fragment and All Fragments Behind It
    fun removeFragmentAndBehind(position: Int) {
        if (position in 0 until fragments.size) {
            val removedCount = fragments.size - position
            for (i in fragments.size - 1 downTo position) {
                fragments.removeAt(i)
            }
            notifyItemRangeRemoved(position, removedCount)
            Log.d("FragmentStateAdapter", "Removed fragment from position $position to $position+$removedCount")
        }
        Log.d("FragmentStateAdapter", "Fragment at position $position unfound")
    }

    // Remove the Last Fragment
    fun removeLastFragment() {
        if (fragments.isNotEmpty()) {
            val lastPosition = fragments.size - 1
            fragments.removeAt(lastPosition)
            notifyItemRemoved(lastPosition)
        }
    }
}
