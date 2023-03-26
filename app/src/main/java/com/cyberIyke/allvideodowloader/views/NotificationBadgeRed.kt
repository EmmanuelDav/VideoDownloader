package com.cyberIyke.allvideodowloader.views


import android.view.LayoutInflater
import android.view.View
import com.cyberIyke.allvideodowloader.R
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView


object NotificationBadgeRed {
    fun getBadge(bottomNavView: BottomNavigationView, indexAt: Int): BadgeRed {
        val bottomNavigationMenuView = bottomNavView.getChildAt(0) as BottomNavigationMenuView
        return if (bottomNavigationMenuView.childCount > indexAt) {
            val v = bottomNavigationMenuView.getChildAt(indexAt)
            val itemView = v as BottomNavigationItemView
            if (itemView.findViewById<View?>(R.id.notifications_nav_badge) == null) {
                val badge = LayoutInflater.from(bottomNavigationMenuView.context)
                    .inflate(R.layout.layout_nav_bottom_badge, itemView, true)
                badge.findViewById(R.id.notifications_nav_badge)
            } else {
                itemView.findViewById(R.id.notifications_nav_badge)
            }
        } else {
            throw IndexOutOfBoundsException(String.format("No menu item at %d", indexAt))
        }
    }
}