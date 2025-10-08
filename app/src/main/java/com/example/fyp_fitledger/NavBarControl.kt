package com.example.fyp_fitledger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavBarControl {

    fun setupBottomNavigation(activity: Activity, bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener { item ->

            resetAllNavItemColors(bottomNavigationView)

            when (item.itemId) {
                R.id.nav_home -> {
                    if (activity !is HomeActivity) {
                        val intent = Intent(activity, HomeActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    }
                    setNavItemColor(bottomNavigationView, R.id.nav_home, R.color.icon_color_2)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_workout -> {
                    if (activity !is WorkoutActivity) {
                        val intent = Intent(activity, WorkoutActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    }
                    setNavItemColor(bottomNavigationView, R.id.nav_workout, R.color.icon_color_2)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_diet -> {
                    if (activity !is DietActivity) {
                        val intent = Intent(activity, DietActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    }
                    setNavItemColor(bottomNavigationView, R.id.nav_diet, R.color.icon_color_2)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_more -> {
                    if (activity !is MoreActivity) {
                        val intent = Intent(activity, MoreActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    }
                    setNavItemColor(bottomNavigationView, R.id.nav_more, R.color.icon_color_2)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }

        when (activity) {
            is HomeActivity -> {
                bottomNavigationView.selectedItemId = R.id.nav_home
                setNavItemColor(bottomNavigationView, R.id.nav_home, R.color.icon_color_2)
            }
            is WorkoutActivity -> {
                bottomNavigationView.selectedItemId = R.id.nav_workout
                setNavItemColor(bottomNavigationView, R.id.nav_workout, R.color.icon_color_2)
            }
            is DietActivity -> {
                bottomNavigationView.selectedItemId = R.id.nav_diet
                setNavItemColor(bottomNavigationView, R.id.nav_diet, R.color.icon_color_2)
            }
            is MoreActivity -> {
                bottomNavigationView.selectedItemId = R.id.nav_more
                setNavItemColor(bottomNavigationView, R.id.nav_more, R.color.icon_color_2)
            }
        }
    }


    private fun resetAllNavItemColors(bottomNavigationView: BottomNavigationView) {
        for (i in 0 until bottomNavigationView.menu.size()) {
            val menuItem = bottomNavigationView.menu.getItem(i)
            menuItem.icon?.let { icon ->  // Use a more descriptive name like 'icon'
                val wrappedIcon = DrawableCompat.wrap(icon).mutate() // Wrap and mutate
                DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(bottomNavigationView.context, R.color.icon_color_1))
                menuItem.icon = wrappedIcon

                //it.clearColorFilter() // Clear the color filter
            }
        }
    }


    private fun setNavItemColor(bottomNavigationView: BottomNavigationView, itemId: Int, colorRes: Int) {
        val menuItem = bottomNavigationView.menu.findItem(itemId)
        menuItem?.icon?.let {icon ->  // Use a more descriptive name like 'icon'
            val wrappedIcon = DrawableCompat.wrap(icon).mutate() // Wrap and mutate
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(bottomNavigationView.context, colorRes))
            menuItem.icon = wrappedIcon // Set the tinted drawable back to the menu item
        }
    }
}