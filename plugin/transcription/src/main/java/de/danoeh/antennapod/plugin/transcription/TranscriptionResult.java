package de.danoeh.antennapod.plugin.transcription;

import androidx.annotation.NonNull;

public class TranscriptionResult {
    private final String mimeType;
    private final String content;

    public TranscriptionResult(@NonNull String mimeType, @NonNull String content) {
        this.mimeType = mimeType;
        this.content = content;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public String getContent() {
        return content;
    }
}
