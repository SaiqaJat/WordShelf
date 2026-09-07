package com.saiqa.dictionary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.saiqa.dictionary.theme.ThemeSettings;

public class SettingsFragment extends Fragment {

    private RadioButton radioLight;
    private RadioButton radioDark;
    private RadioButton radioSystem;
    private MaterialButton btnClearHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        radioLight = view.findViewById(R.id.settings_radio_light);
        radioDark = view.findViewById(R.id.settings_radio_dark);
        radioSystem = view.findViewById(R.id.settings_radio_system);
        btnClearHistory = view.findViewById(R.id.btn_clear_search_history_settings);

        updateRadioSelections();

        radioLight.setOnClickListener(v -> {
            ThemeSettings.setTheme(requireContext(), ThemeSettings.THEME_LIGHT);
            updateRadioSelections();
        });

        radioDark.setOnClickListener(v -> {
            ThemeSettings.setTheme(requireContext(), ThemeSettings.THEME_DARK);
            updateRadioSelections();
        });

        radioSystem.setOnClickListener(v -> {
            ThemeSettings.setTheme(requireContext(), ThemeSettings.THEME_SYSTEM);
            updateRadioSelections();
        });

        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> {
                DictionaryDatabaseHelper db = new DictionaryDatabaseHelper(requireContext());
                db.clearSearchHistory();
                Toast.makeText(requireContext(), "Search history cleared", Toast.LENGTH_SHORT).show();
            });
        }

        return view;
    }

    private void updateRadioSelections() {
        int currentTheme = ThemeSettings.getSavedTheme(requireContext());
        if (radioLight != null) radioLight.setChecked(currentTheme == ThemeSettings.THEME_LIGHT);
        if (radioDark != null) radioDark.setChecked(currentTheme == ThemeSettings.THEME_DARK);
        if (radioSystem != null) radioSystem.setChecked(currentTheme == ThemeSettings.THEME_SYSTEM);
    }
}
