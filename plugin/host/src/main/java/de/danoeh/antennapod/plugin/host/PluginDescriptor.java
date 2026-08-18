package de.danoeh.antennapod.plugin.host;

import androidx.annotation.NonNull;

public class PluginDescriptor {
    private final String id;
    private final String label;
    private final String packageName;
    private final String serviceName;
    private final int capabilities;

    public PluginDescriptor(@NonNull String id, @NonNull String label, @NonNull String packageName,
                            @NonNull String serviceName, int capabilities) {
        this.id = id;
        this.label = label;
        this.packageName = packageName;
        this.serviceName = serviceName;
        this.capabilities = capabilities;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    public String getServiceName() {
        return serviceName;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public boolean hasCapability(int capability) {
        return (capabilities & capability) != 0;
    }
}
