package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginChapter implements Parcelable {
    private long startMs;
    @Nullable private String title;

    public PluginChapter() {
    }

    public PluginChapter(long startMs, @Nullable String title) {
        this.startMs = startMs;
        this.title = title;
    }

    protected PluginChapter(Parcel in) {
        startMs = in.readLong();
        title = in.readString();
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startMs);
        dest.writeString(title);
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
