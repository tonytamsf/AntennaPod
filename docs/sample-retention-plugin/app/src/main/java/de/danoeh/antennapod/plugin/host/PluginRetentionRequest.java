package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PluginRetentionRequest implements Parcelable {
    private long feedId;
    @Nullable private String feedTitle;
    @Nullable private String feedUrl;
    private final List<PluginEpisodeInfo> episodes = new ArrayList<>();

    public PluginRetentionRequest() {
    }

    protected PluginRetentionRequest(Parcel in) {
        feedId = in.readLong();
        feedTitle = in.readString();
        feedUrl = in.readString();
        in.readTypedList(episodes, PluginEpisodeInfo.CREATOR);
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    @Nullable
    public String getFeedTitle() {
        return feedTitle;
    }

    public void setFeedTitle(@Nullable String feedTitle) {
        this.feedTitle = feedTitle;
    }

    @Nullable
    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(@Nullable String feedUrl) {
        this.feedUrl = feedUrl;
    }

    /**
     * All downloaded episodes of this feed, newest first. Episodes that AntennaPod protects from
     * automatic deletion are included but marked via {@link PluginEpisodeInfo#isDeletable()}.
     */
    public List<PluginEpisodeInfo> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(@Nullable List<PluginEpisodeInfo> newEpisodes) {
        episodes.clear();
        if (newEpisodes != null) {
            episodes.addAll(newEpisodes);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(feedId);
        dest.writeString(feedTitle);
        dest.writeString(feedUrl);
        dest.writeTypedList(episodes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginRetentionRequest> CREATOR = new Creator<PluginRetentionRequest>() {
        @Override
        public PluginRetentionRequest createFromParcel(Parcel in) {
            return new PluginRetentionRequest(in);
        }

        @Override
        public PluginRetentionRequest[] newArray(int size) {
            return new PluginRetentionRequest[size];
        }
    };
}
