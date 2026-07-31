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

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.plugin.api.DownloadedMediaProcessor;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
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
        if (media.getMediaType() != MediaType.AUDIO || media.getLocalFileUrl() == null) {
            return false;
        }
        return requestedCapability(media) != 0;
    }

    @Override
    public void process(@NonNull Context context, @NonNull FeedMedia media) {
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
            applyResult(media, invoke(plugin, media));
        } catch (Exception e) {
            Log.e(TAG, "Plugin invocation failed for " + descriptor.getId(), e);
        } finally {
            if (bound) {
                appContext.unbindService(connection);
            }
        }
    }

    @Nullable
    private PluginMediaResult invoke(IMediaProcessorPlugin plugin, FeedMedia media) throws Exception {
        ParcelFileDescriptor descriptorFd = ParcelFileDescriptor.open(
                new File(media.getLocalFileUrl()), ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            PluginMediaRequest request = new PluginMediaRequest();
            request.setCapability(requestedCapability(media));
            request.setMediaFd(descriptorFd);
            request.setMimeType(media.getMimeType());
            request.setEpisodeTitle(media.getEpisodeTitle());
            request.setDurationMs(media.getDuration());
            return plugin.process(request);
        } finally {
            IOUtils.closeQuietly(descriptorFd);
        }
    }

    private int requestedCapability(FeedMedia media) {
        FeedItem item = media.getItem();
        if (item == null) {
            return 0;
        }
        if (descriptor.hasCapability(PluginContract.CAPABILITY_TRANSCRIPTION) && !item.hasTranscript()) {
            return PluginContract.CAPABILITY_TRANSCRIPTION;
        }
        return 0;
    }

    private void applyResult(FeedMedia media, @Nullable PluginMediaResult result) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        if (result.getResultType() == PluginContract.RESULT_TYPE_TRANSCRIPT) {
            applyTranscript(media, result);
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
