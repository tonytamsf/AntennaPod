package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;

public class RetentionEpisode implements Parcelable {
    private long id;
    private long pubDateMs;
    private boolean played;
    private boolean favorite;
    private boolean queued;

    public RetentionEpisode() {
    }

    public RetentionEpisode(long id, long pubDateMs, boolean played, boolean favorite, boolean queued) {
        this.id = id;
        this.pubDateMs = pubDateMs;
        this.played = played;
        this.favorite = favorite;
        this.queued = queued;
    }

    protected RetentionEpisode(Parcel in) {
        id = in.readLong();
        pubDateMs = in.readLong();
        played = in.readInt() != 0;
        favorite = in.readInt() != 0;
        queued = in.readInt() != 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPubDateMs() {
        return pubDateMs;
    }

    public void setPubDateMs(long pubDateMs) {
        this.pubDateMs = pubDateMs;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isQueued() {
        return queued;
    }

    public void setQueued(boolean queued) {
        this.queued = queued;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(pubDateMs);
        dest.writeInt(played ? 1 : 0);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeInt(queued ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RetentionEpisode> CREATOR = new Creator<RetentionEpisode>() {
        @Override
        public RetentionEpisode createFromParcel(Parcel in) {
            return new RetentionEpisode(in);
        }

        @Override
        public RetentionEpisode[] newArray(int size) {
            return new RetentionEpisode[size];
        }
    };
}
