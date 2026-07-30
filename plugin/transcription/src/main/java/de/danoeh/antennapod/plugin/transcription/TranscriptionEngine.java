package de.danoeh.antennapod.plugin.transcription;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public interface TranscriptionEngine {

    boolean isAvailable();

    @Nullable
    TranscriptionResult transcribe(@NonNull Context context, @NonNull File mediaFile);
}
