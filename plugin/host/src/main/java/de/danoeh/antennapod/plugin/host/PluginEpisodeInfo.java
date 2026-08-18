package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginEpisodeInfo implements Parcelable {
    private long id;
    @Nullable private String title;
    private long publishedMs;
    private long downloadedMs;
    private long sizeBytes;
    private boolean played;
    private boolean deletable;

    public PluginEpisodeInfo() {
    }

    protected PluginEpisodeInfo(Parcel in) {
        id = in.readLong();
        title = in.readString();
        publishedMs = in.readLong();
        downloadedMs = in.readLong();
        sizeBytes = in.readLong();
        played = in.readInt() != 0;
        deletable = in.readInt() != 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public long getPublishedMs() {
        return publishedMs;
    }

    public void setPublishedMs(long publishedMs) {
        this.publishedMs = publishedMs;
    }

    public long getDownloadedMs() {
        return downloadedMs;
    }

    public void setDownloadedMs(long downloadedMs) {
        this.downloadedMs = downloadedMs;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    /**
     * Whether AntennaPod would delete this episode if a policy selects it. Episodes that are queued
     * or marked as favorite are never deleted, but they are part of the request so that a policy can
     * take them into account.
     */
    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeLong(publishedMs);
        dest.writeLong(downloadedMs);
        dest.writeLong(sizeBytes);
        dest.writeInt(played ? 1 : 0);
        dest.writeInt(deletable ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginEpisodeInfo> CREATOR = new Creator<PluginEpisodeInfo>() {
        @Override
        public PluginEpisodeInfo createFromParcel(Parcel in) {
            return new PluginEpisodeInfo(in);
        }

        @Override
        public PluginEpisodeInfo[] newArray(int size) {
            return new PluginEpisodeInfo[size];
        }
    };
}
