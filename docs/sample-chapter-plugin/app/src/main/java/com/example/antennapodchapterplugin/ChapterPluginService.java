package com.example.antennapodchapterplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.plugin.host.IMediaProcessorPlugin;
import de.danoeh.antennapod.plugin.host.PluginChapter;
import de.danoeh.antennapod.plugin.host.PluginContract;
import de.danoeh.antennapod.plugin.host.PluginMediaRequest;
import de.danoeh.antennapod.plugin.host.PluginMediaResult;

import java.util.ArrayList;
import java.util.List;

public class ChapterPluginService extends Service {
    private static final String TAG = "SampleChapterPlugin";
    private static final String PLUGIN_ID = "sample-chapters";

    private final IMediaProcessorPlugin.Stub binder = new IMediaProcessorPlugin.Stub() {
        @Override
        public String getPluginId() {
            return PLUGIN_ID;
        }

        @Override
        public int getCapabilities() {
            return PluginContract.CAPABILITY_CHAPTERS;
        }

        @Override
        public PluginMediaResult process(PluginMediaRequest request) {
            PluginMediaResult result = new PluginMediaResult();
            try {
                result.setResultType(PluginContract.RESULT_TYPE_CHAPTERS);
                result.setChapters(generateChapters(request));
                result.setSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Chapter generation failed", e);
                result.setSuccess(false);
                result.setMessage(e.getMessage());
            }
            return result;
        }
    };

    private List<PluginChapter> generateChapters(PluginMediaRequest request) {
        long sizeBytes = statSize(request.getMediaFd());
        long durationMs = Math.max(request.getDurationMs(), 0);
        Log.d(TAG, "Generating chapters for '" + request.getEpisodeTitle() + "' (" + sizeBytes + " bytes)");
        // Replace this placeholder with real chapter detection (e.g. silence/segment
        // analysis) that reads the audio from request.getMediaFd().
        List<PluginChapter> chapters = new ArrayList<>();
        chapters.add(new PluginChapter(0, "Intro"));
        chapters.add(new PluginChapter(durationMs / 3, "Part 1"));
        chapters.add(new PluginChapter(2 * durationMs / 3, "Part 2"));
        return chapters;
    }

    private long statSize(@Nullable ParcelFileDescriptor fd) {
        return fd != null ? fd.getStatSize() : 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
