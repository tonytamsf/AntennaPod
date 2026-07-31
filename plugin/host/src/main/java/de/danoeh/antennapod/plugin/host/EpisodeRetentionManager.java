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
            result.add(new PluginDescriptor(id, label, service.packageName, service.name, 0));
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
        RetentionRequest request = new RetentionRequest();
        request.setFeedId(items.get(0).getFeedId());
        if (items.get(0).getFeed() != null) {
            request.setFeedTitle(items.get(0).getFeed().getTitle());
        }
        List<RetentionEpisode> episodes = new ArrayList<>();
        for (FeedItem item : items) {
            long pubDateMs = item.getPubDate() != null ? item.getPubDate().getTime() : 0;
            episodes.add(new RetentionEpisode(item.getId(), pubDateMs, item.isPlayed(),
                    item.isTagged(FeedItem.TAG_FAVORITE), item.isTagged(FeedItem.TAG_QUEUE)));
        }
        request.setEpisodes(episodes);

        long[] toDelete = plugin.selectEpisodesToDelete(request);
        if (toDelete == null || toDelete.length == 0) {
            return;
        }
        Set<Long> deleteIds = new HashSet<>();
        for (long id : toDelete) {
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
