package uk.co.borconi.emil.obd2aa;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.HashMap;

import uk.co.borconi.emil.obd2aa.preference.GaugePreference;


public class AppSettings extends PreferenceFragmentCompat {

    private final static int MAX_GAUGE = 15;

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.gauge_preferences);
        findPreferenceByKey("layout_style_spinner").setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            updateGaugeCounterDisabledStatus(newValue.toString());
            return true;
        });
        findPreferenceByKey("gauge_counter").setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            updateGaugeCount(Integer.parseInt(newValue.toString()));
            return true;
        });
        initGaugeCount();
        updateGaugeCounterDisabledStatus(((ListPreference)findPreferenceByKey("layout_style_spinner")).getValue());
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void initGaugeCount() {
        int gaugeCount = Integer.parseInt(((ListPreference) findPreferenceByKey("gauge_counter")).getValue());
        GaugePreference g = new GaugePreference(getContext());
        for (int i = 1; i <= MAX_GAUGE; i++) {
            boolean visible = gaugeCount >= i;
            g.buildPrefs(this.getPreferenceScreen(), i, visible);
        }
    }

    private void updateGaugeCount(int gaugeCount) {
        for (int i = 1; i < MAX_GAUGE; i++) {
            Preference pref = findPreferenceByKey("gauge_group_" + i);
            if (i <= gaugeCount) {
                pref.setVisible(true);
                pref.setIcon(R.drawable.ic_baseline_expand_more_24);
            } else {
                pref.setVisible(false);
            }
            findPreferenceByKey("collapser_" + i).setVisible(false);
        }
    }

    private void updateGaugeCounterDisabledStatus(String newLayoutValue) {
        //ListPreference layoutStylePreference = findPreferenceByKey("layout_style_spinner");
        ListPreference gaugeCounterPreference = findPreferenceByKey("gauge_counter");
        EditTextPreference autoLayoutPreference = findPreferenceByKey("auto_layout_row_gauges");

        //String layoutStyle = layoutStylePreference.getValue();

        if ("AUTO".equalsIgnoreCase(newLayoutValue))
        {
            gaugeCounterPreference.setEnabled(true);
            autoLayoutPreference.setEnabled(true);
            return;
        }
        // else
        gaugeCounterPreference.setEnabled(false);
        autoLayoutPreference.setEnabled(false);

        String newGaugeCount = new HashMap<String, String>() {{
            put("1", "5");
            put("2", "3");
            put("3", "4");
        }}.get(newLayoutValue);

        if (newGaugeCount == null) {
            ((ListPreference)findPreferenceByKey("layout_style_spinner")).setValue("AUTO");
            gaugeCounterPreference.setValue("1");
            gaugeCounterPreference.setEnabled(true);
            newGaugeCount = "1";
        }
        gaugeCounterPreference.setValue(newGaugeCount);
        updateGaugeCount(Integer.parseInt(newGaugeCount));
    }

    public <T extends Preference> T findPreferenceByKey(@NonNull CharSequence key) {
        T preference = super.findPreference(key);
        if (preference == null) {
            throw new RuntimeException(String.format("Preference with key [%s] not found", key));
        }
        return preference;
    }
}