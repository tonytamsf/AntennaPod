package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;

public interface FeedContentProcessor {

    @NonNull
    String getId();

    boolean shouldProcess(long feedId, @NonNull String feedUrl);

    /**
     * Rewrites the downloaded feed file in place, before AntennaPod parses it. A processor may drop
     * items so that the app never sees them, for example to keep only the newest N episodes.
     *
     * @param feedFile the downloaded feed document. Implementations must leave a parseable document
     *                 behind; if they cannot, they must leave the file untouched.
     */
    void process(@NonNull Context context, long feedId, @NonNull String feedUrl, @NonNull File feedFile);
}
