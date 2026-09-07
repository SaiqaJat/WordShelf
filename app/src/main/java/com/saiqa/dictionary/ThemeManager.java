package com.saiqa.dictionary;

import android.content.Context;

import com.saiqa.dictionary.theme.ThemeSettings;

public class ThemeManager {

    public static final int THEME_LIGHT = ThemeSettings.THEME_LIGHT;
    public static final int THEME_DARK = ThemeSettings.THEME_DARK;
    public static final int THEME_SYSTEM = ThemeSettings.THEME_SYSTEM;

    public static void applyTheme(Context context) {
        ThemeSettings.applyTheme(context);
    }

    public static void setTheme(Context context, int themeMode) {
        ThemeSettings.setTheme(context, themeMode);
    }

    public static int getSavedTheme(Context context) {
        return ThemeSettings.getSavedTheme(context);
    }

    public static boolean isDarkMode(Context context) {
        return ThemeSettings.isDarkMode(context);
    }
}
