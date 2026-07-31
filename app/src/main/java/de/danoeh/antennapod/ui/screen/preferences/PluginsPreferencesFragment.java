package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.debug.PluginDebugOverlay;
import de.danoeh.antennapod.plugin.host.PluginDescriptor;
import de.danoeh.antennapod.plugin.host.PluginManager;
import de.danoeh.antennapod.plugin.host.PluginPreferences;

import java.util.List;

public class PluginsPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        addDebugOverlayPreference(screen);

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

    private void addDebugOverlayPreference(PreferenceScreen screen) {
        SwitchPreferenceCompat debugPref = new SwitchPreferenceCompat(requireContext());
        debugPref.setKey("plugin_debug_overlay");
        debugPref.setTitle(R.string.plugins_debug_overlay_title);
        debugPref.setSummary(R.string.plugins_debug_overlay_summary);
        debugPref.setPersistent(false);
        debugPref.setChecked(PluginDebugOverlay.getInstance().isShowing());
        debugPref.setOnPreferenceChangeListener((preference, newValue) -> {
            PluginDebugOverlay overlay = PluginDebugOverlay.getInstance();
            if ((Boolean) newValue) {
                if (!overlay.ensurePermission(requireActivity())) {
                    return false;
                }
                overlay.show(requireContext());
            } else {
                overlay.hide();
            }
            return true;
        });
        screen.addPreference(debugPref);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.plugins_pref_title);
        Preference debugPref = findPreference("plugin_debug_overlay");
        if (debugPref instanceof SwitchPreferenceCompat) {
            ((SwitchPreferenceCompat) debugPref).setChecked(PluginDebugOverlay.getInstance().isShowing());
        }
    }
}
