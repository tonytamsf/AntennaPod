package de.danoeh.antennapod.plugin.chaptermarkers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.plugin.api.DownloadedMediaProcessor;
import de.danoeh.antennapod.storage.database.DBWriter;

import java.io.File;
import java.util.List;

public class ChapterMarkerMediaProcessor implements DownloadedMediaProcessor {
    private static final String TAG = "ChapterMarkerProcessor";
    public static final String ID = "chapter-markers";

    private final ChapterMarkerEngine engine;

    public ChapterMarkerMediaProcessor(@NonNull ChapterMarkerEngine engine) {
        this.engine = engine;
    }

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldProcess(@NonNull FeedMedia media) {
        FeedItem item = media.getItem();
        return engine.isAvailable()
                && media.getMediaType() == MediaType.AUDIO
                && media.getLocalFileUrl() != null
                && item != null
                && !item.hasChapters()
                && (item.getChapters() == null || item.getChapters().isEmpty())
                && item.getPodcastIndexChapterUrl() == null;
    }

    @Override
    public void process(@NonNull Context context, @NonNull FeedMedia media) {
        List<Chapter> chapters = engine.generateChapters(context, new File(media.getLocalFileUrl()));
        if (chapters == null || chapters.isEmpty()) {
            return;
        }
        FeedItem item = media.getItem();
        if (item == null) {
            return;
        }
        item.setChapters(chapters);
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Stored " + chapters.size() + " generated chapter markers for " + media.getEpisodeTitle());
    }
}
