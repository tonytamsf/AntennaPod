package de.danoeh.antennapod.plugin.host;

import de.danoeh.antennapod.plugin.host.PluginRetentionRequest;
import de.danoeh.antennapod.plugin.host.PluginRetentionResult;

interface IEpisodeRetentionPlugin {
    String getPluginId();

    int getCapabilities();

    PluginRetentionResult selectForDeletion(in PluginRetentionRequest request);
}
