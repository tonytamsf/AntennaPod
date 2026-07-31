package de.danoeh.antennapod.plugin.host;

import de.danoeh.antennapod.plugin.host.PluginMediaRequest;
import de.danoeh.antennapod.plugin.host.PluginMediaResult;

interface IMediaProcessorPlugin {
    String getPluginId();

    int getCapabilities();

    PluginMediaResult process(in PluginMediaRequest request);
}
