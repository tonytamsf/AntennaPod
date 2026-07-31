package de.danoeh.antennapod.plugin.host;

import de.danoeh.antennapod.plugin.host.RetentionRequest;

interface IEpisodeRetentionPlugin {
    String getPluginId();

    long[] selectEpisodesToDelete(in RetentionRequest request);
}
