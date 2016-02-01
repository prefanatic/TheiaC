package com.example.theia;

import android.util.Size;

import java.util.Comparator;

public class AreaComparator implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
    }
}
