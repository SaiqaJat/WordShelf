package com.saiqa.dictionary.theme;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.saiqa.dictionary.R;

public class ThemeSelectionDialog {

    public interface OnThemeChangeListener {
        void onThemeChanged(int selectedTheme);
    }

    public static void show(@NonNull Context context, final OnThemeChangeListener listener) {
        final Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_theme_selection, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioButton radioLight = view.findViewById(R.id.radio_light);
        RadioButton radioDark = view.findViewById(R.id.radio_dark);
        RadioButton radioSystem = view.findViewById(R.id.radio_system);
        View btnClose = view.findViewById(R.id.btn_close_theme_dialog);

        int currentTheme = ThemeSettings.getSavedTheme(context);
        if (currentTheme == ThemeSettings.THEME_LIGHT) {
            radioLight.setChecked(true);
        } else if (currentTheme == ThemeSettings.THEME_DARK) {
            radioDark.setChecked(true);
        } else {
            radioSystem.setChecked(true);
        }

        radioLight.setOnClickListener(v -> {
            ThemeSettings.setTheme(context, ThemeSettings.THEME_LIGHT);
            dialog.dismiss();
            if (listener != null) listener.onThemeChanged(ThemeSettings.THEME_LIGHT);
        });

        radioDark.setOnClickListener(v -> {
            ThemeSettings.setTheme(context, ThemeSettings.THEME_DARK);
            dialog.dismiss();
            if (listener != null) listener.onThemeChanged(ThemeSettings.THEME_DARK);
        });

        radioSystem.setOnClickListener(v -> {
            ThemeSettings.setTheme(context, ThemeSettings.THEME_SYSTEM);
            dialog.dismiss();
            if (listener != null) listener.onThemeChanged(ThemeSettings.THEME_SYSTEM);
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
