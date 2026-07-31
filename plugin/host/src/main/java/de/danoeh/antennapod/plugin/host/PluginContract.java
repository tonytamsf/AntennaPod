package de.danoeh.antennapod.plugin.host;

public final class PluginContract {
    public static final String ACTION_MEDIA_PROCESSOR = "de.danoeh.antennapod.plugin.action.MEDIA_PROCESSOR";
    public static final String ACTION_EPISODE_RETENTION = "de.danoeh.antennapod.plugin.action.EPISODE_RETENTION";
    public static final String PERMISSION = "de.danoeh.antennapod.permission.PLUGIN";
    public static final String META_DATA_PLUGIN_ID = "de.danoeh.antennapod.plugin.ID";
    public static final String META_DATA_CAPABILITIES = "de.danoeh.antennapod.plugin.CAPABILITIES";

    public static final int CAPABILITY_TRANSCRIPTION = 1;
    public static final int CAPABILITY_CHAPTERS = 1 << 1;
    public static final int CAPABILITY_EPISODE_RETENTION = 1 << 2;

    public static final int RESULT_TYPE_NONE = 0;
    public static final int RESULT_TYPE_TRANSCRIPT = 1;
    public static final int RESULT_TYPE_CHAPTERS = 2;

    private PluginContract() {
    }
}
