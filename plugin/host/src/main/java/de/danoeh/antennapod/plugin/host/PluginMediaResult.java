package de.danoeh.antennapod.plugin.host;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class PluginMediaResult implements Parcelable {
    private boolean success;
    private int resultType;
    @Nullable private String contentMimeType;
    @Nullable private String content;
    @Nullable private String message;

    public PluginMediaResult() {
    }

    protected PluginMediaResult(Parcel in) {
        success = in.readInt() != 0;
        resultType = in.readInt();
        contentMimeType = in.readString();
        content = in.readString();
        message = in.readString();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    @Nullable
    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(@Nullable String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    @Nullable
    public String getContent() {
        return content;
    }

    public void setContent(@Nullable String content) {
        this.content = content;
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
        dest.writeInt(resultType);
        dest.writeString(contentMimeType);
        dest.writeString(content);
        dest.writeString(message);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginMediaResult> CREATOR = new Creator<PluginMediaResult>() {
        @Override
        public PluginMediaResult createFromParcel(Parcel in) {
            return new PluginMediaResult(in);
        }

        @Override
        public PluginMediaResult[] newArray(int size) {
            return new PluginMediaResult[size];
        }
    };
}
