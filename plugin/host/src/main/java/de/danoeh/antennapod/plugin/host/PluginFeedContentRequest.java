package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginFeedContentRequest implements Parcelable {
    private long feedId;
    @Nullable private String feedUrl;
    @Nullable private ParcelFileDescriptor inputFd;
    @Nullable private ParcelFileDescriptor outputFd;

    public PluginFeedContentRequest() {
    }

    protected PluginFeedContentRequest(Parcel in) {
        feedId = in.readLong();
        feedUrl = in.readString();
        inputFd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        outputFd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
    }

    /**
     * The id of the subscription, or 0 while the feed is being added for the first time.
     */
    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    @Nullable
    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(@Nullable String feedUrl) {
        this.feedUrl = feedUrl;
    }

    /**
     * The downloaded feed document, read-only. AntennaPod has not parsed it yet.
     */
    @Nullable
    public ParcelFileDescriptor getInputFd() {
        return inputFd;
    }

    public void setInputFd(@Nullable ParcelFileDescriptor inputFd) {
        this.inputFd = inputFd;
    }

    /**
     * Where to write the rewritten document. Only used when the result reports it was modified.
     */
    @Nullable
    public ParcelFileDescriptor getOutputFd() {
        return outputFd;
    }

    public void setOutputFd(@Nullable ParcelFileDescriptor outputFd) {
        this.outputFd = outputFd;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(feedId);
        dest.writeString(feedUrl);
        dest.writeParcelable(inputFd, flags);
        dest.writeParcelable(outputFd, flags);
    }

    @Override
    public int describeContents() {
        return inputFd != null || outputFd != null ? CONTENTS_FILE_DESCRIPTOR : 0;
    }

    public static final Creator<PluginFeedContentRequest> CREATOR = new Creator<PluginFeedContentRequest>() {
        @Override
        public PluginFeedContentRequest createFromParcel(Parcel in) {
            return new PluginFeedContentRequest(in);
        }

        @Override
        public PluginFeedContentRequest[] newArray(int size) {
            return new PluginFeedContentRequest[size];
        }
    };
}
