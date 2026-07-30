package de.danoeh.antennapod.plugin.transcription;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.plugin.api.DownloadedMediaProcessor;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class TranscriptionMediaProcessor implements DownloadedMediaProcessor {
    private static final String TAG = "TranscriptionProcessor";
    public static final String ID = "transcription";

    private final TranscriptionEngine engine;

    public TranscriptionMediaProcessor(@NonNull TranscriptionEngine engine) {
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
                && !item.hasTranscript();
    }

    @Override
    public void process(@NonNull Context context, @NonNull FeedMedia media) {
        TranscriptionResult result = engine.transcribe(context, new File(media.getLocalFileUrl()));
        if (result == null || StringUtils.isEmpty(result.getContent())) {
            return;
        }
        FeedItem item = media.getItem();
        if (item == null) {
            return;
        }
        item.setTranscriptUrl(result.getMimeType(), media.getTranscriptFileUrl());
        if (!item.hasTranscript()) {
            Log.w(TAG, "Engine returned unsupported transcript format: " + result.getMimeType());
            return;
        }
        TranscriptUtils.storeTranscript(media, result.getContent());
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Stored generated transcript for " + media.getEpisodeTitle());
    }
}
