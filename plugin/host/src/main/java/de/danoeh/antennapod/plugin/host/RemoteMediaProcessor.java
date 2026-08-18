package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import de.danoeh.antennapod.plugin.api.PluginDebugLog;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            PluginDebugLog.debug(TAG, "Skipping '" + descriptor.getId() + "': disabled in settings");
            return false;
        }
        if (media.getMediaType() != MediaType.AUDIO || media.getLocalFileUrl() == null) {
            PluginDebugLog.debug(TAG, "Skipping '" + descriptor.getId() + "': not local audio (type="
                    + media.getMediaType() + ", localFile=" + media.getLocalFileUrl() + ")");
            return false;
        }
        List<Integer> capabilities = applicableCapabilities(media);
        if (capabilities.isEmpty()) {
            PluginDebugLog.debug(TAG, "Skipping '" + descriptor.getId()
                    + "': episode already has the content this plugin would provide");
            return false;
        }
        PluginDebugLog.debug(TAG, "'" + descriptor.getId() + "' will run for capabilities " + capabilities);
        return true;
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
            PluginDebugLog.info(TAG, "Binding to " + descriptor.getPackageName() + "/"
                    + descriptor.getServiceName() + " for '" + descriptor.getId() + "'");
            bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                Log.e(TAG, "Failed to bind plugin service " + descriptor.getId());
                PluginDebugLog.error(TAG, "bindService returned false for '" + descriptor.getId()
                        + "'. Check the plugin's permission and that the service is exported", null);
                return;
            }
            IBinder binder = connection.awaitBinder(BIND_TIMEOUT_MS);
            if (binder == null) {
                Log.e(TAG, "Timed out binding plugin service " + descriptor.getId());
                PluginDebugLog.error(TAG, "Timed out after " + BIND_TIMEOUT_MS + "ms binding '"
                        + descriptor.getId() + "'", null);
                return;
            }
            PluginDebugLog.info(TAG, "Bound to '" + descriptor.getId() + "'; invoking "
                    + capabilities.size() + " capability call(s)");
            IMediaProcessorPlugin plugin = IMediaProcessorPlugin.Stub.asInterface(binder);
            for (int capability : capabilities) {
                PluginDebugLog.info(TAG, "→ Sending request to '" + descriptor.getId() + "' capability="
                        + capability + " title='" + media.getEpisodeTitle() + "' mime="
                        + media.getMimeType() + " durationMs=" + media.getDuration());
                PluginMediaResult result = invoke(plugin, media, capability);
                PluginDebugLog.info(TAG, "← Result from '" + descriptor.getId() + "': "
                        + describeResult(result));
                applyResult(media, result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Plugin invocation failed for " + descriptor.getId(), e);
            PluginDebugLog.error(TAG, "Plugin invocation failed for '" + descriptor.getId() + "'", e);
        } finally {
            if (bound) {
                PluginDebugLog.debug(TAG, "Unbinding from '" + descriptor.getId() + "'");
                appContext.unbindService(connection);
            }
        }
    }

    private static String describeResult(@Nullable PluginMediaResult result) {
        if (result == null) {
            return "null (no response)";
        }
        if (!result.isSuccess()) {
            return "success=false";
        }
        if (result.getResultType() == PluginContract.RESULT_TYPE_TRANSCRIPT) {
            String content = result.getContent();
            return "transcript mime=" + result.getContentMimeType()
                    + " length=" + (content == null ? 0 : content.length());
        } else if (result.getResultType() == PluginContract.RESULT_TYPE_CHAPTERS) {
            return "chapters count=" + result.getChapters().size();
        }
        return "resultType=" + result.getResultType();
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
            PluginDebugLog.warn(TAG, "Ignoring transcript from '" + descriptor.getId()
                    + "': missing item, empty content, or missing mime type");
            return;
        }
        item.setTranscriptUrl(result.getContentMimeType(), media.getTranscriptFileUrl());
        if (!item.hasTranscript()) {
            Log.w(TAG, "Plugin returned unsupported transcript format: " + result.getContentMimeType());
            PluginDebugLog.warn(TAG, "Plugin '" + descriptor.getId()
                    + "' returned unsupported transcript format: " + result.getContentMimeType());
            return;
        }
        TranscriptUtils.storeTranscript(media, result.getContent());
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Applied transcript from plugin " + descriptor.getId());
        PluginDebugLog.info(TAG, "Applied transcript from plugin '" + descriptor.getId() + "'");
    }

    private void applyChapters(FeedMedia media, PluginMediaResult result) {
        FeedItem item = media.getItem();
        if (item == null || result.getChapters().isEmpty()) {
            PluginDebugLog.warn(TAG, "Ignoring chapters from '" + descriptor.getId()
                    + "': missing item or empty chapter list");
            return;
        }
        List<Chapter> chapters = new ArrayList<>();
        for (PluginChapter chapter : result.getChapters()) {
            chapters.add(new Chapter(chapter.getStartMs(), chapter.getTitle(),
                    chapter.getUrl(), chapter.getImageUrl()));
        }
        item.setChapters(chapters);
        DBWriter.setFeedItem(item, false);
        Log.d(TAG, "Applied " + chapters.size() + " chapters from plugin " + descriptor.getId());
        PluginDebugLog.info(TAG, "Applied " + chapters.size() + " chapters from plugin '"
                + descriptor.getId() + "'");
    }
}
