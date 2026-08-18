package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class EpisodeRetentionRegistry {
    private static final String TAG = "EpisodeRetentionRegistry";
    private static final List<EpisodeRetentionPolicy> policies = new ArrayList<>();

    private EpisodeRetentionRegistry() {
    }

    public static synchronized void register(@NonNull EpisodeRetentionPolicy policy) {
        for (EpisodeRetentionPolicy existing : policies) {
            if (existing.getId().equals(policy.getId())) {
                return;
            }
        }
        policies.add(policy);
    }

    public static synchronized void unregister(@NonNull String id) {
        Iterator<EpisodeRetentionPolicy> iterator = policies.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @NonNull
    public static synchronized List<EpisodeRetentionPolicy> getPolicies() {
        return Collections.unmodifiableList(new ArrayList<>(policies));
    }

    /**
     * Asks every registered policy which of the given downloaded episodes of a feed are no longer
     * needed. Episodes that were not part of the list are ignored, so a policy can never select
     * episodes of other feeds. It is up to the caller to skip episodes that must not be deleted.
     *
     * @return the union of all selections, without duplicates.
     */
    @NonNull
    public static List<FeedItem> selectForDeletion(@NonNull Context context, @NonNull Feed feed,
                                                   @NonNull List<FeedItem> downloadedItems) {
        List<EpisodeRetentionPolicy> current = getPolicies();
        if (current.isEmpty() || downloadedItems.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> candidateIds = new HashSet<>();
        for (FeedItem item : downloadedItems) {
            candidateIds.add(item.getId());
        }
        PluginDebugLog.info(TAG, "Asking " + current.size() + " retention policy/policies about "
                + downloadedItems.size() + " downloaded episode(s) of '" + feed.getTitle() + "'");
        Set<Long> selectedIds = new HashSet<>();
        List<FeedItem> selected = new ArrayList<>();
        for (EpisodeRetentionPolicy policy : current) {
            try {
                if (!policy.shouldApply(feed)) {
                    PluginDebugLog.debug(TAG, "Policy '" + policy.getId() + "' does not apply to '"
                            + feed.getTitle() + "'");
                    continue;
                }
                List<FeedItem> result = policy.selectForDeletion(context, feed, downloadedItems);
                PluginDebugLog.info(TAG, "Policy '" + policy.getId() + "' selected " + result.size()
                        + " episode(s) of '" + feed.getTitle() + "' for deletion");
                for (FeedItem item : result) {
                    if (!candidateIds.contains(item.getId())) {
                        PluginDebugLog.warn(TAG, "Policy '" + policy.getId() + "' selected episode "
                                + item.getId() + " that was not offered. Ignoring it.");
                        continue;
                    }
                    if (selectedIds.add(item.getId())) {
                        selected.add(item);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Retention policy '" + policy.getId() + "' failed", e);
                PluginDebugLog.error(TAG, "Retention policy '" + policy.getId() + "' failed", e);
            }
        }
        return selected;
    }
}
