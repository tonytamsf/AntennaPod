package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginChapter implements Parcelable {
    private long startMs;
    @Nullable private String title;
    @Nullable private String url;
    @Nullable private String imageUrl;

    public PluginChapter() {
    }

    public PluginChapter(long startMs, @Nullable String title) {
        this(startMs, title, null, null);
    }

    public PluginChapter(long startMs, @Nullable String title,
                         @Nullable String url, @Nullable String imageUrl) {
        this.startMs = startMs;
        this.title = title;
        this.url = url;
        this.imageUrl = imageUrl;
    }

    protected PluginChapter(Parcel in) {
        startMs = in.readLong();
        title = in.readString();
        url = in.readString();
        imageUrl = in.readString();
    }

    public long getStartMs() {
        return startMs;
    }

    public void setStartMs(long startMs) {
        this.startMs = startMs;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startMs);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(imageUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginChapter> CREATOR = new Creator<PluginChapter>() {
        @Override
        public PluginChapter createFromParcel(Parcel in) {
            return new PluginChapter(in);
        }

        @Override
        public PluginChapter[] newArray(int size) {
            return new PluginChapter[size];
        }
    };
}
