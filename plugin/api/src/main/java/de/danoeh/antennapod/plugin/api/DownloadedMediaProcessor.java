package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedMedia;

public interface DownloadedMediaProcessor {

    @NonNull
    String getId();

    boolean shouldProcess(@NonNull FeedMedia media);

    void process(@NonNull Context context, @NonNull FeedMedia media);
}
