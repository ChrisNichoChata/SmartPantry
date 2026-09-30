package com.chataika.smartpantry.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.data.AppSettings;
import com.chataika.smartpantry.data.PantryDbHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.time.LocalDate;

/** Settings screen. Preferences are saved to SharedPreferences as soon as they change. */
public class SettingsFragment extends Fragment {

    private static final Integer[] EXPIRY_DAY_OPTIONS = {1, 2, 3, 5, 7};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppSettings settings = new AppSettings(requireContext());

        SwitchMaterial alerts = view.findViewById(R.id.switch_expiry_alerts);
        Spinner days = view.findViewById(R.id.spinner_expiry_days);
        SwitchMaterial ignoreExpired = view.findViewById(R.id.switch_ignore_expired);
        SwitchMaterial showAlmost = view.findViewById(R.id.switch_show_almost);

        // Expiring-soon alerts
        alerts.setChecked(settings.isExpiryAlertsEnabled());
        days.setEnabled(alerts.isChecked());
        alerts.setOnCheckedChangeListener((button, checked) -> {
            settings.setExpiryAlertsEnabled(checked);
            days.setEnabled(checked);
        });

        ArrayAdapter<Integer> daysAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, EXPIRY_DAY_OPTIONS);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        days.setAdapter(daysAdapter);
        days.setSelection(indexOfDays(settings.getExpiryWarningDays()), false);
        days.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                settings.setExpiryWarningDays(EXPIRY_DAY_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Matching options
        ignoreExpired.setChecked(settings.isIgnoreExpired());
        ignoreExpired.setOnCheckedChangeListener((button, checked) -> settings.setIgnoreExpired(checked));
        showAlmost.setChecked(settings.isShowAlmostThere());
        showAlmost.setOnCheckedChangeListener((button, checked) -> settings.setShowAlmostThere(checked));

        // Data tools
        PantryDbHelper db = PantryDbHelper.getInstance(requireContext());
        view.findViewById(R.id.button_sample_pantry).setOnClickListener(v -> {
            db.insertSamplePantry(LocalDate.now());
            Snackbar.make(view, R.string.sample_loaded, Snackbar.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.button_clear_pantry).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.clear_pantry)
                        .setMessage(R.string.clear_pantry_confirm)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            db.clearPantry();
                            Snackbar.make(view, R.string.pantry_cleared, Snackbar.LENGTH_SHORT).show();
                        })
                        .show());

        TextView about = view.findViewById(R.id.text_about);
        about.setText(getString(R.string.about_text, versionName()));
    }

    private static int indexOfDays(int days) {
        for (int i = 0; i < EXPIRY_DAY_OPTIONS.length; i++) {
            if (EXPIRY_DAY_OPTIONS[i] == days) return i;
        }
        return 2; // default: 3 days
    }

    private String versionName() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
