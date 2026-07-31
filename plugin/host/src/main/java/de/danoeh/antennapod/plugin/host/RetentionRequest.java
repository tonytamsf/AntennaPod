package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RetentionRequest implements Parcelable {
    private long feedId;
    @Nullable private String feedTitle;
    private final List<RetentionEpisode> episodes = new ArrayList<>();

    public RetentionRequest() {
    }

    protected RetentionRequest(Parcel in) {
        feedId = in.readLong();
        feedTitle = in.readString();
        in.readTypedList(episodes, RetentionEpisode.CREATOR);
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

    public List<RetentionEpisode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(@Nullable List<RetentionEpisode> newEpisodes) {
        episodes.clear();
        if (newEpisodes != null) {
            episodes.addAll(newEpisodes);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(feedId);
        dest.writeString(feedTitle);
        dest.writeTypedList(episodes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RetentionRequest> CREATOR = new Creator<RetentionRequest>() {
        @Override
        public RetentionRequest createFromParcel(Parcel in) {
            return new RetentionRequest(in);
        }

        @Override
        public RetentionRequest[] newArray(int size) {
            return new RetentionRequest[size];
        }
    };
}
