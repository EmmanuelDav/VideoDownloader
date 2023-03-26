package com.cyberIyke.allvideodowloader.interfaces;

import com.cyberIyke.allvideodowloader.database.ShortcutTable;

public interface ShortcutListner {
    void shortcutClick(ShortcutTable shortcutTable);
    void shortcutRemoveClick(ShortcutTable shortcutTable);
}
