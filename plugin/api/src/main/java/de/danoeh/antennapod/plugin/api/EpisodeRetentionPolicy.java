package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;

import java.util.List;

public interface EpisodeRetentionPolicy {

    @NonNull
    String getId();

    boolean shouldApply(@NonNull Feed feed);

    /**
     * @param downloadedItems all downloaded episodes of the feed, newest first
     * @return the episodes that are no longer needed. Only episodes of {@code downloadedItems} are
     *         taken into account, and the caller still skips episodes that must not be deleted.
     */
    @NonNull
    List<FeedItem> selectForDeletion(@NonNull Context context, @NonNull Feed feed,
                                     @NonNull List<FeedItem> downloadedItems);
}
