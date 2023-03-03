package com.kunkunapp.allvideodowloader.views;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kunkunapp.allvideodowloader.R;

public class NotificationBadge {
    public static Badge getBadge(BottomNavigationView bottomNavView, int indexAt) {
        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) bottomNavView.getChildAt(0);
        if (bottomNavigationMenuView.getChildCount() > indexAt) {
            View v = bottomNavigationMenuView.getChildAt(indexAt);
            BottomNavigationItemView itemView = (BottomNavigationItemView) v;
            if (itemView.findViewById(R.id.notifications_nav_badge) == null) {
                View badge = LayoutInflater.from(bottomNavigationMenuView.getContext()).inflate(R.layout.bottom_tab_layout, itemView, true);
                return badge.findViewById(R.id.notifications_nav_badge);
            } else {
                return itemView.findViewById(R.id.notifications_nav_badge);
            }

        } else {
            throw new IndexOutOfBoundsException(String.format("No menu item at %d", indexAt));
        }
    }
}
