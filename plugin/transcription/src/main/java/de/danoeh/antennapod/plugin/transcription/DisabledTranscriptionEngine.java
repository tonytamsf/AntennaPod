package de.danoeh.antennapod.plugin.transcription;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class DisabledTranscriptionEngine implements TranscriptionEngine {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Nullable
    @Override
    public TranscriptionResult transcribe(@NonNull Context context, @NonNull File mediaFile) {
        return null;
    }
}
