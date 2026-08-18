package de.danoeh.antennapod.plugin.host;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PluginPreferences {
    private static final String PREF_NAME = "PluginPreferences";
    private static final String PREF_ENABLED_IDS = "enabledPluginIds";

    private static SharedPreferences prefs;

    private PluginPreferences() {
    }

    public static void init(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(@NonNull String pluginId) {
        return prefs.getStringSet(PREF_ENABLED_IDS, Collections.emptySet()).contains(pluginId);
    }

    public static void setEnabled(@NonNull String pluginId, boolean enabled) {
        Set<String> ids = new HashSet<>(prefs.getStringSet(PREF_ENABLED_IDS, Collections.emptySet()));
        if (enabled) {
            ids.add(pluginId);
        } else {
            ids.remove(pluginId);
        }
        prefs.edit().putStringSet(PREF_ENABLED_IDS, ids).apply();
    }
}
