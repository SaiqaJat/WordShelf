package com.saiqa.dictionary.theme;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.saiqa.dictionary.R;

public class ThemeColorPalette {

    public static int getBackgroundColor(Context context) {
        return ContextCompat.getColor(context, R.color.background_color);
    }

    public static int getSurfaceColor(Context context) {
        return ContextCompat.getColor(context, R.color.surface_card);
    }

    public static int getPrimaryColor(Context context) {
        return ContextCompat.getColor(context, R.color.color_primary);
    }

    public static int getTextPrimaryColor(Context context) {
        return ContextCompat.getColor(context, R.color.text_color);
    }

    public static int getTextSecondaryColor(Context context) {
        return ContextCompat.getColor(context, R.color.text_secondary);
    }

    public static int getStrokeColor(Context context) {
        return ContextCompat.getColor(context, R.color.surface_stroke);
    }

    public static int getAccentColor(Context context) {
        return ContextCompat.getColor(context, R.color.color_accent);
    }
}
