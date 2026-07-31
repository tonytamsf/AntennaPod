package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.plugin.host.PluginDescriptor;
import de.danoeh.antennapod.plugin.host.PluginManager;
import de.danoeh.antennapod.plugin.host.PluginPreferences;

import java.util.List;

public class PluginsPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        List<PluginDescriptor> plugins = PluginManager.discover(requireContext());
        if (plugins.isEmpty()) {
            Preference empty = new Preference(requireContext());
            empty.setTitle(R.string.plugins_none_installed_title);
            empty.setSummary(R.string.plugins_none_installed_summary);
            empty.setSelectable(false);
            screen.addPreference(empty);
            return;
        }

        for (PluginDescriptor plugin : plugins) {
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireContext());
            pref.setKey("plugin_" + plugin.getId());
            pref.setTitle(plugin.getLabel());
            pref.setSummary(plugin.getPackageName());
            pref.setPersistent(false);
            pref.setChecked(PluginPreferences.isEnabled(plugin.getId()));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                PluginPreferences.setEnabled(plugin.getId(), (Boolean) newValue);
                return true;
            });
            screen.addPreference(pref);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.plugins_pref_title);
    }
}
