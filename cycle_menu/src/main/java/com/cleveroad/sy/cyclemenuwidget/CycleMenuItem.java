package com.cleveroad.sy.cyclemenuwidget;

import android.graphics.drawable.Drawable;

/**
 * Model class for menu items
 */
public class CycleMenuItem {
    private Drawable mIcon;
    private int mId;
    private int mColor;

    public CycleMenuItem(int id, Drawable icon) {
        mId = id;
        mIcon = icon;

    }

    public CycleMenuItem(int color) {
        mColor = color;
    }

    public int getId() {
        return mId;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public int getColor() {
        return mColor;
    }
}
