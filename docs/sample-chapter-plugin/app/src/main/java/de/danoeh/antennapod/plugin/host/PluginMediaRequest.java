package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginMediaRequest implements Parcelable {
    private int capability;
    @Nullable private ParcelFileDescriptor mediaFd;
    @Nullable private String mimeType;
    @Nullable private String episodeTitle;
    private long durationMs;

    public PluginMediaRequest() {
    }

    protected PluginMediaRequest(Parcel in) {
        capability = in.readInt();
        mediaFd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        mimeType = in.readString();
        episodeTitle = in.readString();
        durationMs = in.readLong();
    }

    public int getCapability() {
        return capability;
    }

    public void setCapability(int capability) {
        this.capability = capability;
    }

    @Nullable
    public ParcelFileDescriptor getMediaFd() {
        return mediaFd;
    }

    public void setMediaFd(@Nullable ParcelFileDescriptor mediaFd) {
        this.mediaFd = mediaFd;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(@Nullable String mimeType) {
        this.mimeType = mimeType;
    }

    @Nullable
    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(@Nullable String episodeTitle) {
        this.episodeTitle = episodeTitle;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(capability);
        dest.writeParcelable(mediaFd, flags);
        dest.writeString(mimeType);
        dest.writeString(episodeTitle);
        dest.writeLong(durationMs);
    }

    @Override
    public int describeContents() {
        return mediaFd != null ? CONTENTS_FILE_DESCRIPTOR : 0;
    }

    public static final Creator<PluginMediaRequest> CREATOR = new Creator<PluginMediaRequest>() {
        @Override
        public PluginMediaRequest createFromParcel(Parcel in) {
            return new PluginMediaRequest(in);
        }

        @Override
        public PluginMediaRequest[] newArray(int size) {
            return new PluginMediaRequest[size];
        }
    };
}
