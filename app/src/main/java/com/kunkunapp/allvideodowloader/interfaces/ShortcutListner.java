package com.kunkunapp.allvideodowloader.interfaces;

import com.kunkunapp.allvideodowloader.database.ShortcutTable;

public interface ShortcutListner {
    void shortcutClick(ShortcutTable shortcutTable);
    void shortcutRemoveClick(ShortcutTable shortcutTable);
}
