package de.danoeh.antennapod.plugin.host;

import de.danoeh.antennapod.plugin.host.PluginFeedContentRequest;
import de.danoeh.antennapod.plugin.host.PluginFeedContentResult;

interface IFeedContentPlugin {
    String getPluginId();

    int getCapabilities();

    PluginFeedContentResult processFeed(in PluginFeedContentRequest request);
}
