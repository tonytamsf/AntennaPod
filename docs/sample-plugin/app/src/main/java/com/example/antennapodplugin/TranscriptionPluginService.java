package com.example.antennapodplugin;

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

public class TranscriptionPluginService extends Service {
    private static final String TAG = "SamplePlugin";
    private static final String PLUGIN_ID = "sample-transcription";

    private final IMediaProcessorPlugin.Stub binder = new IMediaProcessorPlugin.Stub() {
        @Override
        public String getPluginId() {
            return PLUGIN_ID;
        }

        @Override
        public int getCapabilities() {
            return PluginContract.CAPABILITY_TRANSCRIPTION | PluginContract.CAPABILITY_CHAPTERS;
        }

        @Override
        public PluginMediaResult process(PluginMediaRequest request) {
            PluginMediaResult result = new PluginMediaResult();
            try {
                if (request.getCapability() == PluginContract.CAPABILITY_CHAPTERS) {
                    result.setResultType(PluginContract.RESULT_TYPE_CHAPTERS);
                    result.setChapters(generateChapters(request));
                } else {
                    result.setResultType(PluginContract.RESULT_TYPE_TRANSCRIPT);
                    result.setContentMimeType("text/vtt");
                    result.setContent(transcribe(request));
                }
                result.setSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Processing failed", e);
                result.setSuccess(false);
                result.setMessage(e.getMessage());
            }
            return result;
        }
    };

    private String transcribe(PluginMediaRequest request) {
        long sizeBytes = statSize(request.getMediaFd());
        Log.d(TAG, "Transcribing '" + request.getEpisodeTitle() + "' (" + sizeBytes + " bytes)");
        // Replace this placeholder with a real speech-to-text engine that reads
        // the audio from request.getMediaFd() and returns WebVTT/SRT/JSON text.
        return "WEBVTT\n\n"
                + "00:00:00.000 --> 00:00:10.000\n"
                + "Sample plugin transcript for: " + request.getEpisodeTitle() + "\n\n"
                + "00:00:10.000 --> 00:00:20.000\n"
                + "Audio size received over the binder: " + sizeBytes + " bytes\n";
    }

    private List<PluginChapter> generateChapters(PluginMediaRequest request) {
        long durationMs = Math.max(request.getDurationMs(), 0);
        List<PluginChapter> chapters = new ArrayList<>();
        chapters.add(new PluginChapter(0, "Sample chapter: Intro"));
        chapters.add(new PluginChapter(durationMs / 2, "Sample chapter: Middle"));
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
