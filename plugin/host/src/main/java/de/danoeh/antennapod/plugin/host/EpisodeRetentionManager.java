package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.plugin.api.PluginDebugLog;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class EpisodeRetentionManager {
    private static final String TAG = "EpisodeRetentionManager";
    private static final long BIND_TIMEOUT_MS = 15000;

    private EpisodeRetentionManager() {
    }

    public static List<PluginDescriptor> discover(Context context) {
        List<PluginDescriptor> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(PluginContract.ACTION_EPISODE_RETENTION);
        List<ResolveInfo> services = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : services) {
            ServiceInfo service = info.serviceInfo;
            if (service == null || service.metaData == null) {
                continue;
            }
            String id = service.metaData.getString(PluginContract.META_DATA_PLUGIN_ID);
            if (id == null) {
                continue;
            }
            String label = String.valueOf(service.loadLabel(packageManager));
            int capabilities = service.metaData.getInt(PluginContract.META_DATA_CAPABILITIES, 0);
            result.add(new PluginDescriptor(id, label, service.packageName, service.name, capabilities));
        }
        return result;
    }

    public static void applyRetention(Context context) {
        Context appContext = context.getApplicationContext();
        List<PluginDescriptor> plugins = discover(appContext);
        for (PluginDescriptor descriptor : plugins) {
            if (!PluginPreferences.isEnabled(descriptor.getId())) {
                continue;
            }
            try {
                applyForPlugin(appContext, descriptor);
            } catch (Exception e) {
                Log.e(TAG, "Retention plugin '" + descriptor.getId() + "' failed", e);
                PluginDebugLog.error(TAG, "Retention plugin '" + descriptor.getId() + "' failed", e);
            }
        }
    }

    private static void applyForPlugin(Context appContext, PluginDescriptor descriptor) {
        Map<Long, List<FeedItem>> byFeed = downloadedEpisodesByFeed();
        if (byFeed.isEmpty()) {
            return;
        }
        Intent intent = new Intent(PluginContract.ACTION_EPISODE_RETENTION);
        intent.setComponent(new ComponentName(descriptor.getPackageName(), descriptor.getServiceName()));
        BlockingServiceConnection connection = new BlockingServiceConnection();
        boolean bound = false;
        try {
            bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                PluginDebugLog.error(TAG, "bindService returned false for retention plugin '"
                        + descriptor.getId() + "'", null);
                return;
            }
            IBinder binder = connection.awaitBinder(BIND_TIMEOUT_MS);
            if (binder == null) {
                PluginDebugLog.error(TAG, "Timed out binding retention plugin '" + descriptor.getId() + "'", null);
                return;
            }
            IEpisodeRetentionPlugin plugin = IEpisodeRetentionPlugin.Stub.asInterface(binder);
            for (Map.Entry<Long, List<FeedItem>> entry : byFeed.entrySet()) {
                deleteForFeed(appContext, descriptor, plugin, entry.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "Retention invocation failed for " + descriptor.getId(), e);
            PluginDebugLog.error(TAG, "Retention invocation failed for '" + descriptor.getId() + "'", e);
        } finally {
            if (bound) {
                appContext.unbindService(connection);
            }
        }
    }

    private static void deleteForFeed(Context appContext, PluginDescriptor descriptor,
                                      IEpisodeRetentionPlugin plugin, List<FeedItem> items) throws Exception {
        FeedItem first = items.get(0);
        PluginRetentionRequest request = new PluginRetentionRequest();
        request.setFeedId(first.getFeedId());
        if (first.getFeed() != null) {
            request.setFeedTitle(first.getFeed().getTitle());
            request.setFeedUrl(first.getFeed().getDownloadUrl());
        }
        List<PluginEpisodeInfo> episodes = new ArrayList<>();
        for (FeedItem item : items) {
            PluginEpisodeInfo info = new PluginEpisodeInfo();
            info.setId(item.getId());
            info.setTitle(item.getTitle());
            info.setPublishedMs(item.getPubDate() != null ? item.getPubDate().getTime() : 0);
            FeedMedia media = item.getMedia();
            info.setDownloadedMs(media != null ? media.getDownloadDate() : 0);
            info.setSizeBytes(media != null ? media.getSize() : 0);
            info.setPlayed(item.isPlayed());
            info.setDeletable(isSafeToDelete(item));
            episodes.add(info);
        }
        request.setEpisodes(episodes);

        PluginRetentionResult result = plugin.selectForDeletion(request);
        if (result == null || !result.isSuccess()) {
            return;
        }
        Set<Long> deleteIds = new HashSet<>();
        for (long id : result.getEpisodeIdsToDelete()) {
            deleteIds.add(id);
        }
        int deleted = 0;
        for (FeedItem item : items) {
            if (deleteIds.contains(item.getId()) && isSafeToDelete(item)) {
                DBWriter.deleteFeedMediaOfItem(appContext, item.getMedia()).get();
                deleted++;
            }
        }
        PluginDebugLog.info(TAG, "Retention '" + descriptor.getId() + "' deleted " + deleted
                + " episode(s) from feed '" + request.getFeedTitle() + "'");
    }

    private static boolean isSafeToDelete(FeedItem item) {
        return item.hasMedia()
                && item.getMedia().isDownloaded()
                && item.getFeed() != null
                && !item.getFeed().isLocalFeed()
                && !item.isTagged(FeedItem.TAG_FAVORITE)
                && !item.isTagged(FeedItem.TAG_QUEUE);
    }

    private static Map<Long, List<FeedItem>> downloadedEpisodesByFeed() {
        List<FeedItem> downloaded = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        Map<Long, List<FeedItem>> byFeed = new LinkedHashMap<>();
        for (FeedItem item : downloaded) {
            if (!item.hasMedia() || item.getFeed() == null) {
                continue;
            }
            List<FeedItem> list = byFeed.get(item.getFeedId());
            if (list == null) {
                list = new ArrayList<>();
                byFeed.put(item.getFeedId(), list);
            }
            list.add(item);
        }
        return byFeed;
    }

    private static class BlockingServiceConnection implements ServiceConnection {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile IBinder binder;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            latch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }

        @Nullable
        IBinder awaitBinder(long timeoutMs) throws InterruptedException {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return binder;
        }
    }
}
