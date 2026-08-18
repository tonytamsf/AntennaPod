package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.plugin.api.FeedContentProcessor;
import de.danoeh.antennapod.plugin.api.PluginDebugLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class RemoteFeedContentProcessor implements FeedContentProcessor {
    private static final String TAG = "RemoteFeedContent";
    private static final long BIND_TIMEOUT_MS = 15000;

    private final Context appContext;
    private final PluginDescriptor descriptor;

    public RemoteFeedContentProcessor(@NonNull Context appContext, @NonNull PluginDescriptor descriptor) {
        this.appContext = appContext;
        this.descriptor = descriptor;
    }

    @NonNull
    @Override
    public String getId() {
        return "remote:" + descriptor.getId();
    }

    @Override
    public boolean shouldProcess(long feedId, @NonNull String feedUrl) {
        if (!PluginPreferences.isEnabled(descriptor.getId())) {
            PluginDebugLog.debug(TAG, "Skipping '" + descriptor.getId() + "': disabled in settings");
            return false;
        }
        return descriptor.hasCapability(PluginContract.CAPABILITY_FEED_CONTENT);
    }

    @Override
    public void process(@NonNull Context context, long feedId, @NonNull String feedUrl,
                        @NonNull File feedFile) {
        Intent intent = new Intent(PluginContract.ACTION_FEED_CONTENT);
        intent.setComponent(new ComponentName(descriptor.getPackageName(), descriptor.getServiceName()));
        BlockingServiceConnection connection = new BlockingServiceConnection();
        File rewritten = new File(feedFile.getAbsolutePath() + ".plugin");
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
            IFeedContentPlugin plugin = IFeedContentPlugin.Stub.asInterface(binder);
            PluginFeedContentResult result = invoke(plugin, feedId, feedUrl, feedFile, rewritten);
            PluginDebugLog.info(TAG, "← Result from '" + descriptor.getId() + "': " + describeResult(result));
            applyResult(result, feedFile, rewritten);
        } catch (Exception e) {
            Log.e(TAG, "Plugin invocation failed for " + descriptor.getId(), e);
            PluginDebugLog.error(TAG, "Plugin invocation failed for '" + descriptor.getId() + "'", e);
        } finally {
            FileUtils.deleteQuietly(rewritten);
            if (bound) {
                PluginDebugLog.debug(TAG, "Unbinding from '" + descriptor.getId() + "'");
                appContext.unbindService(connection);
            }
        }
    }

    private PluginFeedContentResult invoke(IFeedContentPlugin plugin, long feedId, String feedUrl,
                                           File feedFile, File rewritten) throws Exception {
        ParcelFileDescriptor inputFd = ParcelFileDescriptor.open(
                feedFile, ParcelFileDescriptor.MODE_READ_ONLY);
        ParcelFileDescriptor outputFd = ParcelFileDescriptor.open(rewritten,
                ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE
                        | ParcelFileDescriptor.MODE_WRITE_ONLY);
        try {
            PluginFeedContentRequest request = new PluginFeedContentRequest();
            request.setFeedId(feedId);
            request.setFeedUrl(feedUrl);
            request.setInputFd(inputFd);
            request.setOutputFd(outputFd);
            PluginDebugLog.info(TAG, "→ Sending feed to '" + descriptor.getId() + "' feedId=" + feedId
                    + " url=" + feedUrl + " bytes=" + feedFile.length());
            return plugin.processFeed(request);
        } finally {
            IOUtils.closeQuietly(inputFd);
            IOUtils.closeQuietly(outputFd);
        }
    }

    private void applyResult(PluginFeedContentResult result, File feedFile, File rewritten)
            throws Exception {
        if (result == null || !result.isSuccess() || !result.isModified()) {
            return;
        }
        if (!isUsable(rewritten)) {
            Log.w(TAG, "Plugin " + descriptor.getId() + " produced an unusable feed; keeping the original");
            PluginDebugLog.warn(TAG, "Plugin '" + descriptor.getId() + "' produced an empty or non-XML "
                    + "document (" + rewritten.length() + " bytes); keeping the downloaded feed");
            return;
        }
        FileUtils.copyFile(rewritten, feedFile);
        Log.d(TAG, "Applied feed rewritten by plugin " + descriptor.getId());
        PluginDebugLog.info(TAG, "Applied feed rewritten by '" + descriptor.getId() + "': "
                + result.getItemsRemoved() + " item(s) removed, " + feedFile.length() + " bytes");
    }

    private static boolean isUsable(File rewritten) throws Exception {
        if (!rewritten.exists() || rewritten.length() == 0) {
            return false;
        }
        try (InputStream stream = new FileInputStream(rewritten)) {
            int character;
            while ((character = stream.read()) != -1) {
                if (!Character.isWhitespace(character)) {
                    return character == '<';
                }
            }
        }
        return false;
    }

    private static String describeResult(PluginFeedContentResult result) {
        if (result == null) {
            return "null (no response)";
        }
        if (!result.isSuccess()) {
            return "success=false";
        }
        return "modified=" + result.isModified() + " itemsRemoved=" + result.getItemsRemoved();
    }
}
