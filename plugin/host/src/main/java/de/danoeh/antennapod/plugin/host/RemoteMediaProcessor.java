package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.plugin.api.DownloadedMediaProcessor;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RemoteMediaProcessor implements DownloadedMediaProcessor {
    private static final String TAG = "RemoteMediaProcessor";
    private static final long BIND_TIMEOUT_MS = 15000;

    private final Context appContext;
    private final PluginDescriptor descriptor;

    public RemoteMediaProcessor(@NonNull Context appContext, @NonNull PluginDescriptor descriptor) {
        this.appContext = appContext;
        this.descriptor = descriptor;
    }

    @NonNull
    @Override
    public String getId() {
        return "remote:" + descriptor.getId();
    }

    @Override
    public boolean shouldProcess(@NonNull FeedMedia media) {
        if (!PluginPreferences.isEnabled(descriptor.getId())) {
            return false;
        }
        if (media.getMediaType() != MediaType.AUDIO || media.getLocalFileUrl() == null) {
            return false;
        }
        return !applicableCapabilities(media).isEmpty();
    }

    @Override
    public void process(@NonNull Context context, @NonNull FeedMedia media) {
        List<Integer> capabilities = applicableCapabilities(media);
        if (capabilities.isEmpty()) {
            return;
        }
        Intent intent = new Intent(PluginContract.ACTION_MEDIA_PROCESSOR);
        intent.setComponent(new ComponentName(descriptor.getPackageName(), descriptor.getServiceName()));
        BlockingServiceConnection connection = new BlockingServiceConnection();
        boolean bound = false;
        try {
            bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                Log.e(TAG, "Failed to bind plugin service " + descriptor.getId());
                return;
            }
            IBinder binder = connection.awaitBinder(BIND_TIMEOUT_MS);
            if (binder == null) {
                Log.e(TAG, "Timed out binding plugin service " + descriptor.getId());
                return;
            }
            IMediaProcessorPlugin plugin = IMediaProcessorPlugin.Stub.asInterface(binder);
            for (int capability : capabilities) {
                applyResult(media, invoke(plugin, media, capability));
            }
        } catch (Exception e) {
            Log.e(TAG, "Plugin invocation failed for " + descriptor.getId(), e);
        } finally {
            if (bound) {
                appContext.unbindService(connection);
            }
        }
    }

    private List<Integer> applicableCapabilities(FeedMedia media) {
        List<Integer> capabilities = new ArrayList<>();
        FeedItem item = media.getItem();
        if (item == null) {
            return capabilities;
        }
        if (descriptor.hasCapability(PluginContract.CAPABILITY_TRANSCRIPTION) && !item.hasTranscript()) {
            capabilities.add(PluginContract.CAPABILITY_TRANSCRIPTION);
        }
        if (descriptor.hasCapability(PluginContract.CAPABILITY_CHAPTERS)
                && !item.hasChapters()
                && (item.getChapters() == null || item.getChapters().isEmpty())
                && item.getPodcastIndexChapterUrl() == null) {
            capabilities.add(PluginContract.CAPABILITY_CHAPTERS);
        }
        return capabilities;
    }

    @Nullable
    private PluginMediaResult invoke(IMediaProcessorPlugin plugin, FeedMedia media, int capability) throws Exception {
        ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                new File(media.getLocalFileUrl()), ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            PluginMediaRequest request = new PluginMediaRequest();
            request.setCapability(capability);
            request.setMediaFd(fileDescriptor);
            request.setMimeType(media.getMimeType());
            request.setEpisodeTitle(media.getEpisodeTitle());
            request.setDurationMs(media.getDuration());
            return plugin.process(request);
        } finally {
            IOUtils.closeQuietly(fileDescriptor);
        }
    }

    private void applyResult(FeedMedia media, @Nullable PluginMediaResult result) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        if (result.getResultType() == PluginContract.RESULT_TYPE_TRANSCRIPT) {
            applyTranscript(media, result);
        } else if (result.getResultType() == PluginContract.RESULT_TYPE_CHAPTERS) {
            applyChapters(media, result);
        }
    }

    private void applyTranscript(FeedMedia media, PluginMediaResult result) {
        FeedItem item = media.getItem();
        if (item == null || StringUtils.isEmpty(result.getContent()) || result.getContentMimeType() == null) {
            return;
        }
        item.setTranscriptUrl(result.getContentMimeType(), media.getTranscriptFileUrl());
        if (!item.hasTranscript()) {
            Log.w(TAG, "Plugin returned unsupported transcript format: " + result.getContentMimeType());
            return;
        }
        TranscriptUtils.storeTranscript(media, result.getContent());
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Applied transcript from plugin " + descriptor.getId());
    }

    private void applyChapters(FeedMedia media, PluginMediaResult result) {
        FeedItem item = media.getItem();
        if (item == null || result.getChapters().isEmpty()) {
            return;
        }
        List<Chapter> chapters = new ArrayList<>();
        for (PluginChapter chapter : result.getChapters()) {
            chapters.add(new Chapter(chapter.getStartMs(), chapter.getTitle(), null, null));
        }
        item.setChapters(chapters);
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Applied " + chapters.size() + " chapters from plugin " + descriptor.getId());
    }

    private static class BlockingServiceConnection implements ServiceConnection {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile IBinder binder;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            latch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }

        @Nullable
        IBinder awaitBinder(long timeoutMs) throws InterruptedException {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return binder;
        }
    }
}
