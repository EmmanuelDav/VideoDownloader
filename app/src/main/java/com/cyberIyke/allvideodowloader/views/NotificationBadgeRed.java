package com.cyberIyke.allvideodowloader.views;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.cyberIyke.allvideodowloader.R;

public class NotificationBadgeRed {
    public static BadgeRed getBadge(BottomNavigationView bottomNavView, int indexAt) {
        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) bottomNavView.getChildAt(0);
        if (bottomNavigationMenuView.getChildCount() > indexAt) {
            View v = bottomNavigationMenuView.getChildAt(indexAt);
            BottomNavigationItemView itemView = (BottomNavigationItemView) v;
            if (itemView.findViewById(R.id.notifications_nav_badge) == null) {
                View badge = LayoutInflater.from(bottomNavigationMenuView.getContext()).inflate(R.layout.layout_nav_bottom_badge, itemView, true);
                return badge.findViewById(R.id.notifications_nav_badge);
            } else {
                return itemView.findViewById(R.id.notifications_nav_badge);
            }

        } else {
            throw new IndexOutOfBoundsException(String.format("No menu item at %d", indexAt));
        }
    }
}
