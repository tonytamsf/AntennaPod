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
        List<PluginDescriptor> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(PluginContract.ACTION_MEDIA_PROCESSOR);
        List<ResolveInfo> services = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
        PluginDebugLog.i(TAG, "Discovery: " + services.size() + " service(s) responded to "
                + PluginContract.ACTION_MEDIA_PROCESSOR);
        for (ResolveInfo info : services) {
            ServiceInfo service = info.serviceInfo;
            if (service == null || service.metaData == null) {
                PluginDebugLog.w(TAG, "Skipping a resolved service with no meta-data "
                        + "(missing <meta-data> plugin ID/capabilities?)");
                continue;
            }
            String id = service.metaData.getString(PluginContract.META_DATA_PLUGIN_ID);
            int capabilities = service.metaData.getInt(PluginContract.META_DATA_CAPABILITIES, 0);
            if (id == null || capabilities == 0) {
                PluginDebugLog.w(TAG, "Skipping " + service.packageName + "/" + service.name
                        + " (id=" + id + ", capabilities=" + capabilities + ")");
                continue;
            }
            String label = String.valueOf(service.loadLabel(packageManager));
            PluginDebugLog.i(TAG, "Discovered plugin '" + id + "' (" + label + ") in "
                    + service.packageName + " capabilities=" + capabilities);
            result.add(new PluginDescriptor(id, label, service.packageName, service.name, capabilities));
        }
        if (result.isEmpty()) {
            PluginDebugLog.w(TAG, "No plugins discovered. Confirm the plugin app is installed and exposes a "
                    + "service with the MEDIA_PROCESSOR intent filter and plugin meta-data.");
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
            PluginDebugLog.i(TAG, "Registering plugin '" + descriptor.getId() + "' from "
                    + descriptor.getPackageName()
                    + " (enabled=" + PluginPreferences.isEnabled(descriptor.getId()) + ")");
            MediaProcessorRegistry.register(new RemoteMediaProcessor(appContext, descriptor));
        }
        for (DownloadedMediaProcessor processor : MediaProcessorRegistry.getProcessors()) {
            if (processor.getId().startsWith(REMOTE_ID_PREFIX) && !discoveredIds.contains(processor.getId())) {
                Log.d(TAG, "Unregistering removed plugin " + processor.getId());
                PluginDebugLog.i(TAG, "Unregistering removed plugin " + processor.getId());
                MediaProcessorRegistry.unregister(processor.getId());
            }
        }
        PluginDebugLog.d(TAG, "Registry now holds " + MediaProcessorRegistry.getProcessors().size()
                + " processor(s)");
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
