package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;
import android.util.Log;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.plugin.api.EpisodeRetentionRegistry;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Lets registered retention policies decide, per subscription, which downloaded episodes are no
 * longer needed. Policies see all downloaded episodes of a feed, but episodes that AntennaPod
 * protects from automatic deletion (queued and favorite episodes) are never deleted.
 */
public class PluginRetentionCleanup {
    private static final String TAG = "PluginRetentionCleanup";

    private PluginRetentionCleanup() {
    }

    public static int performCleanup(Context context) {
        if (EpisodeRetentionRegistry.getPolicies().isEmpty()) {
            return 0;
        }
        Map<Long, List<FeedItem>> itemsByFeed = new LinkedHashMap<>();
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        for (FeedItem item : downloadedItems) {
            if (!item.hasMedia() || !item.getMedia().isDownloaded() || item.getFeed() == null) {
                continue;
            }
            List<FeedItem> items = itemsByFeed.get(item.getFeedId());
            if (items == null) {
                items = new ArrayList<>();
                itemsByFeed.put(item.getFeedId(), items);
            }
            items.add(item);
        }

        int deleted = 0;
        for (List<FeedItem> items : itemsByFeed.values()) {
            Feed feed = items.get(0).getFeed();
            for (FeedItem item : EpisodeRetentionRegistry.selectForDeletion(context, feed, items)) {
                if (!mayDelete(item)) {
                    Log.d(TAG, "Not deleting protected episode " + item.getId());
                    continue;
                }
                try {
                    DBWriter.deleteFeedMediaOfItem(context, item.getMedia()).get();
                    deleted++;
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Could not delete episode " + item.getId(), e);
                }
            }
        }
        Log.i(TAG, "Retention policies deleted " + deleted + " episodes");
        return deleted;
    }

    private static boolean mayDelete(FeedItem item) {
        return (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal())
                && !item.isTagged(FeedItem.TAG_QUEUE)
                && !item.isTagged(FeedItem.TAG_FAVORITE);
    }
}
