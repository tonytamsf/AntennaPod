package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.plugin.api.EpisodeRetentionPolicy;
import de.danoeh.antennapod.plugin.api.PluginDebugLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteEpisodeRetentionPolicy implements EpisodeRetentionPolicy {
    private static final String TAG = "RemoteRetentionPolicy";
    private static final long BIND_TIMEOUT_MS = 15000;

    private final Context appContext;
    private final PluginDescriptor descriptor;

    public RemoteEpisodeRetentionPolicy(@NonNull Context appContext, @NonNull PluginDescriptor descriptor) {
        this.appContext = appContext;
        this.descriptor = descriptor;
    }

    @NonNull
    @Override
    public String getId() {
        return "remote:" + descriptor.getId();
    }

    @Override
    public boolean shouldApply(@NonNull Feed feed) {
        if (!PluginPreferences.isEnabled(descriptor.getId())) {
            PluginDebugLog.debug(TAG, "Skipping '" + descriptor.getId() + "': disabled in settings");
            return false;
        }
        return descriptor.hasCapability(PluginContract.CAPABILITY_EPISODE_RETENTION);
    }

    @NonNull
    @Override
    public List<FeedItem> selectForDeletion(@NonNull Context context, @NonNull Feed feed,
                                            @NonNull List<FeedItem> downloadedItems) {
        Intent intent = new Intent(PluginContract.ACTION_EPISODE_RETENTION);
        intent.setComponent(new ComponentName(descriptor.getPackageName(), descriptor.getServiceName()));
        BlockingServiceConnection connection = new BlockingServiceConnection();
        boolean bound = false;
        try {
            PluginDebugLog.info(TAG, "Binding to " + descriptor.getPackageName() + "/"
                    + descriptor.getServiceName() + " for '" + descriptor.getId() + "'");
            bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                Log.e(TAG, "Failed to bind plugin service " + descriptor.getId());
                PluginDebugLog.error(TAG, "bindService returned false for '" + descriptor.getId()
                        + "'. Check the plugin's permission and that the service is exported", null);
                return Collections.emptyList();
            }
            IBinder binder = connection.awaitBinder(BIND_TIMEOUT_MS);
            if (binder == null) {
                Log.e(TAG, "Timed out binding plugin service " + descriptor.getId());
                PluginDebugLog.error(TAG, "Timed out after " + BIND_TIMEOUT_MS + "ms binding '"
                        + descriptor.getId() + "'", null);
                return Collections.emptyList();
            }
            IEpisodeRetentionPlugin plugin = IEpisodeRetentionPlugin.Stub.asInterface(binder);
            PluginRetentionRequest request = buildRequest(feed, downloadedItems);
            PluginDebugLog.info(TAG, "→ Asking '" + descriptor.getId() + "' about " + downloadedItems.size()
                    + " downloaded episode(s) of '" + feed.getTitle() + "'");
            PluginRetentionResult result = plugin.selectForDeletion(request);
            PluginDebugLog.info(TAG, "← Result from '" + descriptor.getId() + "': " + describeResult(result));
            return toFeedItems(result, downloadedItems);
        } catch (Exception e) {
            Log.e(TAG, "Plugin invocation failed for " + descriptor.getId(), e);
            PluginDebugLog.error(TAG, "Plugin invocation failed for '" + descriptor.getId() + "'", e);
            return Collections.emptyList();
        } finally {
            if (bound) {
                PluginDebugLog.debug(TAG, "Unbinding from '" + descriptor.getId() + "'");
                appContext.unbindService(connection);
            }
        }
    }

    private static String describeResult(PluginRetentionResult result) {
        if (result == null) {
            return "null (no response)";
        }
        if (!result.isSuccess()) {
            return "success=false";
        }
        return "delete count=" + result.getEpisodeIdsToDelete().length;
    }

    private static PluginRetentionRequest buildRequest(Feed feed, List<FeedItem> downloadedItems) {
        PluginRetentionRequest request = new PluginRetentionRequest();
        request.setFeedId(feed.getId());
        request.setFeedTitle(feed.getTitle());
        request.setFeedUrl(feed.getDownloadUrl());
        List<PluginEpisodeInfo> episodes = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            PluginEpisodeInfo info = new PluginEpisodeInfo();
            info.setId(item.getId());
            info.setTitle(item.getTitle());
            info.setPublishedMs(item.getPubDate() == null ? 0 : item.getPubDate().getTime());
            info.setPlayed(item.isPlayed());
            info.setDeletable(!item.isTagged(FeedItem.TAG_QUEUE) && !item.isTagged(FeedItem.TAG_FAVORITE));
            FeedMedia media = item.getMedia();
            if (media != null) {
                info.setDownloadedMs(media.getDownloadDate());
                info.setSizeBytes(media.getSize());
            }
            episodes.add(info);
        }
        request.setEpisodes(episodes);
        return request;
    }

    private List<FeedItem> toFeedItems(PluginRetentionResult result, List<FeedItem> downloadedItems) {
        if (result == null || !result.isSuccess()) {
            return Collections.emptyList();
        }
        Map<Long, FeedItem> byId = new HashMap<>();
        for (FeedItem item : downloadedItems) {
            byId.put(item.getId(), item);
        }
        List<FeedItem> selected = new ArrayList<>();
        for (long id : result.getEpisodeIdsToDelete()) {
            FeedItem item = byId.get(id);
            if (item == null) {
                PluginDebugLog.warn(TAG, "Plugin '" + descriptor.getId() + "' returned unknown episode id "
                        + id + ". Ignoring it.");
                continue;
            }
            selected.add(item);
        }
        return selected;
    }
}
