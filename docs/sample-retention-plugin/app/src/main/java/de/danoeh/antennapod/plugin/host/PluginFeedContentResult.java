package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginFeedContentResult implements Parcelable {
    private boolean success;
    private boolean modified;
    private int itemsRemoved;
    @Nullable private String message;

    public PluginFeedContentResult() {
    }

    protected PluginFeedContentResult(Parcel in) {
        success = in.readInt() != 0;
        modified = in.readInt() != 0;
        itemsRemoved = in.readInt();
        message = in.readString();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Whether the plugin wrote a rewritten document to the request's output descriptor. When false,
     * AntennaPod parses the feed exactly as it was downloaded.
     */
    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * How many items the plugin dropped. Informational only.
     */
    public int getItemsRemoved() {
        return itemsRemoved;
    }

    public void setItemsRemoved(int itemsRemoved) {
        this.itemsRemoved = itemsRemoved;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(success ? 1 : 0);
        dest.writeInt(modified ? 1 : 0);
        dest.writeInt(itemsRemoved);
        dest.writeString(message);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginFeedContentResult> CREATOR = new Creator<PluginFeedContentResult>() {
        @Override
        public PluginFeedContentResult createFromParcel(Parcel in) {
            return new PluginFeedContentResult(in);
        }

        @Override
        public PluginFeedContentResult[] newArray(int size) {
            return new PluginFeedContentResult[size];
        }
    };
}
