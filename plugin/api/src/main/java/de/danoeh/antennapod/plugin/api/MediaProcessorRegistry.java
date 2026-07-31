package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class MediaProcessorRegistry {
    private static final String TAG = "MediaProcessorRegistry";
    private static final List<DownloadedMediaProcessor> processors = new ArrayList<>();

    private MediaProcessorRegistry() {
    }

    public static synchronized void register(@NonNull DownloadedMediaProcessor processor) {
        for (DownloadedMediaProcessor existing : processors) {
            if (existing.getId().equals(processor.getId())) {
                return;
            }
        }
        processors.add(processor);
    }

    public static synchronized void unregister(@NonNull String id) {
        Iterator<DownloadedMediaProcessor> iterator = processors.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @NonNull
    public static synchronized List<DownloadedMediaProcessor> getProcessors() {
        return Collections.unmodifiableList(new ArrayList<>(processors));
    }

    public static void runAll(@NonNull Context context, @NonNull FeedMedia media) {
        for (DownloadedMediaProcessor processor : getProcessors()) {
            try {
                if (processor.shouldProcess(media)) {
                    Log.d(TAG, "Running processor '" + processor.getId() + "' for " + media.getEpisodeTitle());
                    processor.process(context, media);
                }
            } catch (Exception e) {
                Log.e(TAG, "Processor '" + processor.getId() + "' failed", e);
            }
        }
    }
}
