package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PluginRetentionResult implements Parcelable {
    private boolean success;
    @Nullable private String message;
    @NonNull private long[] episodeIdsToDelete = new long[0];

    public PluginRetentionResult() {
    }

    protected PluginRetentionResult(Parcel in) {
        success = in.readInt() != 0;
        message = in.readString();
        long[] ids = in.createLongArray();
        episodeIdsToDelete = ids == null ? new long[0] : ids;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    /**
     * Ids of the episodes that may be deleted. Ids that were not part of the request are ignored.
     */
    @NonNull
    public long[] getEpisodeIdsToDelete() {
        return episodeIdsToDelete;
    }

    public void setEpisodeIdsToDelete(@Nullable long[] episodeIdsToDelete) {
        this.episodeIdsToDelete = episodeIdsToDelete == null ? new long[0] : episodeIdsToDelete;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(success ? 1 : 0);
        dest.writeString(message);
        dest.writeLongArray(episodeIdsToDelete);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginRetentionResult> CREATOR = new Creator<PluginRetentionResult>() {
        @Override
        public PluginRetentionResult createFromParcel(Parcel in) {
            return new PluginRetentionResult(in);
        }

        @Override
        public PluginRetentionResult[] newArray(int size) {
            return new PluginRetentionResult[size];
        }
    };
}
