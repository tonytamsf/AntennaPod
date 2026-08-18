package com.example.antennapodretentionplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.plugin.host.IEpisodeRetentionPlugin;
import de.danoeh.antennapod.plugin.host.PluginContract;
import de.danoeh.antennapod.plugin.host.PluginEpisodeInfo;
import de.danoeh.antennapod.plugin.host.PluginRetentionRequest;
import de.danoeh.antennapod.plugin.host.PluginRetentionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the newest N downloaded episodes of every subscription, where N is configurable per
 * podcast in this app (see {@link RetentionSettingsActivity}).
 */
public class RetentionPluginService extends Service {
    private static final String TAG = "RetentionPlugin";
    private static final String PLUGIN_ID = "keep-newest-episodes";

    private RetentionPreferences preferences;

    private final IEpisodeRetentionPlugin.Stub binder = new IEpisodeRetentionPlugin.Stub() {
        @Override
        public String getPluginId() {
            return PLUGIN_ID;
        }

        @Override
        public int getCapabilities() {
            return PluginContract.CAPABILITY_EPISODE_RETENTION;
        }

        @Override
        public PluginRetentionResult selectForDeletion(PluginRetentionRequest request) {
            PluginRetentionResult result = new PluginRetentionResult();
            try {
                result.setEpisodeIdsToDelete(selectSurplusEpisodes(request));
                result.setSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Retention check failed", e);
                result.setSuccess(false);
                result.setMessage(e.getMessage());
            }
            return result;
        }
    };

    private long[] selectSurplusEpisodes(PluginRetentionRequest request) {
        preferences.rememberFeed(request.getFeedId(), request.getFeedTitle());
        int keep = preferences.getKeepCount(request.getFeedId());
        List<PluginEpisodeInfo> episodes = new ArrayList<>(request.getEpisodes());
        Log.d(TAG, "'" + request.getFeedTitle() + "' has " + episodes.size()
                + " downloaded episodes, keeping " + (keep == RetentionPreferences.KEEP_ALL ? "all" : keep));
        if (keep == RetentionPreferences.KEEP_ALL || episodes.size() <= keep) {
            return new long[0];
        }
        Collections.sort(episodes, (lhs, rhs) -> Long.compare(sortDate(rhs), sortDate(lhs)));

        List<Long> surplus = new ArrayList<>();
        int kept = 0;
        for (PluginEpisodeInfo episode : episodes) {
            if (kept < keep) {
                kept++;
                continue;
            }
            // Queued and favorite episodes are kept by AntennaPod anyway, so do not ask for them.
            if (episode.isDeletable()) {
                surplus.add(episode.getId());
            }
        }
        long[] ids = new long[surplus.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = surplus.get(i);
        }
        return ids;
    }

    private static long sortDate(PluginEpisodeInfo episode) {
        return episode.getPublishedMs() > 0 ? episode.getPublishedMs() : episode.getDownloadedMs();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new RetentionPreferences(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
