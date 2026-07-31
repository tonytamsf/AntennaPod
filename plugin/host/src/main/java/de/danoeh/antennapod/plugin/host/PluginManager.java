package de.danoeh.antennapod.plugin.host;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;
import de.danoeh.antennapod.plugin.api.MediaProcessorRegistry;

import java.util.ArrayList;
import java.util.List;

public final class PluginManager {
    private static final String TAG = "PluginManager";

    private PluginManager() {
    }

    public static List<PluginDescriptor> discover(Context context) {
        List<PluginDescriptor> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(PluginContract.ACTION_MEDIA_PROCESSOR);
        List<ResolveInfo> services = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : services) {
            ServiceInfo service = info.serviceInfo;
            if (service == null || service.metaData == null) {
                continue;
            }
            String id = service.metaData.getString(PluginContract.META_DATA_PLUGIN_ID);
            int capabilities = service.metaData.getInt(PluginContract.META_DATA_CAPABILITIES, 0);
            if (id == null || capabilities == 0) {
                continue;
            }
            String label = String.valueOf(service.loadLabel(packageManager));
            result.add(new PluginDescriptor(id, label, service.packageName, service.name, capabilities));
        }
        return result;
    }

    public static void discoverAndRegister(Context context) {
        Context appContext = context.getApplicationContext();
        for (PluginDescriptor descriptor : discover(appContext)) {
            Log.d(TAG, "Registering plugin '" + descriptor.getId() + "' from " + descriptor.getPackageName());
            MediaProcessorRegistry.register(new RemoteMediaProcessor(appContext, descriptor));
        }
    }
}
