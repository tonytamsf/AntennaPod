package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class FeedContentRegistry {
    private static final String TAG = "FeedContentRegistry";
    private static final List<FeedContentProcessor> processors = new ArrayList<>();

    private FeedContentRegistry() {
    }

    public static synchronized void register(@NonNull FeedContentProcessor processor) {
        for (FeedContentProcessor existing : processors) {
            if (existing.getId().equals(processor.getId())) {
                return;
            }
        }
        processors.add(processor);
    }

    public static synchronized void unregister(@NonNull String id) {
        Iterator<FeedContentProcessor> iterator = processors.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @NonNull
    public static synchronized List<FeedContentProcessor> getProcessors() {
        return Collections.unmodifiableList(new ArrayList<>(processors));
    }

    /**
     * Lets every registered processor rewrite the downloaded feed document before it is parsed.
     * A processor that fails is skipped; the file it was given is then used as it left it.
     */
    public static void runAll(@NonNull Context context, long feedId, @NonNull String feedUrl,
                              @NonNull File feedFile) {
        List<FeedContentProcessor> current = getProcessors();
        if (current.isEmpty() || !feedFile.exists()) {
            return;
        }
        PluginDebugLog.info(TAG, "Feed downloaded from " + feedUrl + " (" + feedFile.length()
                + " bytes); evaluating " + current.size() + " feed content processor(s)");
        for (FeedContentProcessor processor : current) {
            try {
                if (!processor.shouldProcess(feedId, feedUrl)) {
                    PluginDebugLog.debug(TAG, "Processor '" + processor.getId() + "' skips " + feedUrl);
                    continue;
                }
                long sizeBefore = feedFile.length();
                processor.process(context, feedId, feedUrl, feedFile);
                PluginDebugLog.info(TAG, "Processor '" + processor.getId() + "' finished for " + feedUrl
                        + " (" + sizeBefore + " → " + feedFile.length() + " bytes)");
            } catch (Exception e) {
                Log.e(TAG, "Feed content processor '" + processor.getId() + "' failed", e);
                PluginDebugLog.error(TAG, "Feed content processor '" + processor.getId() + "' failed", e);
            }
        }
    }
}
