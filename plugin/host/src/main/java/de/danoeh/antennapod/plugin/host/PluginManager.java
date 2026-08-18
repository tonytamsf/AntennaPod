package de.danoeh.antennapod.plugin.host;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;
import de.danoeh.antennapod.plugin.api.DownloadedMediaProcessor;
import de.danoeh.antennapod.plugin.api.EpisodeRetentionPolicy;
import de.danoeh.antennapod.plugin.api.EpisodeRetentionRegistry;
import de.danoeh.antennapod.plugin.api.FeedContentProcessor;
import de.danoeh.antennapod.plugin.api.FeedContentRegistry;
import de.danoeh.antennapod.plugin.api.MediaProcessorRegistry;
import de.danoeh.antennapod.plugin.api.PluginDebugLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PluginManager {
    private static final String TAG = "PluginManager";
    private static final String REMOTE_ID_PREFIX = "remote:";
    private static boolean monitorRegistered = false;

    private PluginManager() {
    }

    public static List<PluginDescriptor> discover(Context context) {
        return discover(context, PluginContract.ACTION_MEDIA_PROCESSOR);
    }

    public static List<PluginDescriptor> discoverRetentionPlugins(Context context) {
        return discover(context, PluginContract.ACTION_EPISODE_RETENTION);
    }

    public static List<PluginDescriptor> discoverFeedContentPlugins(Context context) {
        return discover(context, PluginContract.ACTION_FEED_CONTENT);
    }

    /**
     * All plugins the user can enable, no matter which extension point they implement.
     */
    public static List<PluginDescriptor> discoverAll(Context context) {
        List<PluginDescriptor> result = new ArrayList<>(discover(context));
        Set<String> knownIds = new HashSet<>();
        for (PluginDescriptor descriptor : result) {
            knownIds.add(descriptor.getId());
        }
        for (PluginDescriptor descriptor : discoverRetentionPlugins(context)) {
            if (knownIds.add(descriptor.getId())) {
                result.add(descriptor);
            }
        }
        for (PluginDescriptor descriptor : discoverFeedContentPlugins(context)) {
            if (knownIds.add(descriptor.getId())) {
                result.add(descriptor);
            }
        }
        return result;
    }

    private static List<PluginDescriptor> discover(Context context, String action) {
        List<PluginDescriptor> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(action);
        List<ResolveInfo> services = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
        PluginDebugLog.info(TAG, "Discovery: " + services.size() + " service(s) responded to " + action);
        for (ResolveInfo info : services) {
            ServiceInfo service = info.serviceInfo;
            if (service == null || service.metaData == null) {
                PluginDebugLog.warn(TAG, "Skipping a resolved service with no meta-data "
                        + "(missing <meta-data> plugin ID/capabilities?)");
                continue;
            }
            String id = service.metaData.getString(PluginContract.META_DATA_PLUGIN_ID);
            int capabilities = service.metaData.getInt(PluginContract.META_DATA_CAPABILITIES, 0);
            if (id == null || capabilities == 0) {
                PluginDebugLog.warn(TAG, "Skipping " + service.packageName + "/" + service.name
                        + " (id=" + id + ", capabilities=" + capabilities + ")");
                continue;
            }
            String label = String.valueOf(service.loadLabel(packageManager));
            PluginDebugLog.info(TAG, "Discovered plugin '" + id + "' (" + label + ") in "
                    + service.packageName + " capabilities=" + capabilities);
            result.add(new PluginDescriptor(id, label, service.packageName, service.name, capabilities));
        }
        if (result.isEmpty()) {
            PluginDebugLog.warn(TAG, "No plugins discovered for " + action + ". Confirm the plugin app is "
                    + "installed and exposes a service with that intent filter and plugin meta-data.");
        }
        return result;
    }

    public static void discoverAndRegister(Context context) {
        Context appContext = context.getApplicationContext();
        syncRegistrations(appContext);
        registerPackageMonitor(appContext);
    }

    public static synchronized void syncRegistrations(Context context) {
        Context appContext = context.getApplicationContext();
        Set<String> discoveredIds = new HashSet<>();
        for (PluginDescriptor descriptor : discover(appContext)) {
            discoveredIds.add(REMOTE_ID_PREFIX + descriptor.getId());
            Log.d(TAG, "Registering plugin '" + descriptor.getId() + "' from " + descriptor.getPackageName());
            PluginDebugLog.info(TAG, "Registering plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName()
                    + " (enabled=" + PluginPreferences.isEnabled(descriptor.getId()) + ")");
            MediaProcessorRegistry.register(new RemoteMediaProcessor(appContext, descriptor));
        }
        for (DownloadedMediaProcessor processor : MediaProcessorRegistry.getProcessors()) {
            if (processor.getId().startsWith(REMOTE_ID_PREFIX) && !discoveredIds.contains(processor.getId())) {
                Log.d(TAG, "Unregistering removed plugin " + processor.getId());
                PluginDebugLog.info(TAG, "Unregistering removed plugin " + processor.getId());
                MediaProcessorRegistry.unregister(processor.getId());
            }
        }

        Set<String> discoveredRetentionIds = new HashSet<>();
        for (PluginDescriptor descriptor : discoverRetentionPlugins(appContext)) {
            discoveredRetentionIds.add(REMOTE_ID_PREFIX + descriptor.getId());
            Log.d(TAG, "Registering retention plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName());
            PluginDebugLog.info(TAG, "Registering retention plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName()
                    + " (enabled=" + PluginPreferences.isEnabled(descriptor.getId()) + ")");
            EpisodeRetentionRegistry.register(new RemoteEpisodeRetentionPolicy(appContext, descriptor));
        }
        for (EpisodeRetentionPolicy policy : EpisodeRetentionRegistry.getPolicies()) {
            if (policy.getId().startsWith(REMOTE_ID_PREFIX) && !discoveredRetentionIds.contains(policy.getId())) {
                Log.d(TAG, "Unregistering removed retention plugin " + policy.getId());
                PluginDebugLog.info(TAG, "Unregistering removed retention plugin " + policy.getId());
                EpisodeRetentionRegistry.unregister(policy.getId());
            }
        }
        Set<String> discoveredFeedContentIds = new HashSet<>();
        for (PluginDescriptor descriptor : discoverFeedContentPlugins(appContext)) {
            discoveredFeedContentIds.add(REMOTE_ID_PREFIX + descriptor.getId());
            Log.d(TAG, "Registering feed content plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName());
            PluginDebugLog.info(TAG, "Registering feed content plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName()
                    + " (enabled=" + PluginPreferences.isEnabled(descriptor.getId()) + ")");
            FeedContentRegistry.register(new RemoteFeedContentProcessor(appContext, descriptor));
        }
        for (FeedContentProcessor processor : FeedContentRegistry.getProcessors()) {
            if (processor.getId().startsWith(REMOTE_ID_PREFIX)
                    && !discoveredFeedContentIds.contains(processor.getId())) {
                Log.d(TAG, "Unregistering removed feed content plugin " + processor.getId());
                PluginDebugLog.info(TAG, "Unregistering removed feed content plugin " + processor.getId());
                FeedContentRegistry.unregister(processor.getId());
            }
        }
        PluginDebugLog.debug(TAG, "Registry now holds " + MediaProcessorRegistry.getProcessors().size()
                + " processor(s), " + EpisodeRetentionRegistry.getPolicies().size()
                + " retention policy/policies and " + FeedContentRegistry.getProcessors().size()
                + " feed content processor(s)");
    }

    private static synchronized void registerPackageMonitor(Context appContext) {
        if (monitorRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncRegistrations(appContext);
            }
        }, filter);
        monitorRegistered = true;
    }
}
