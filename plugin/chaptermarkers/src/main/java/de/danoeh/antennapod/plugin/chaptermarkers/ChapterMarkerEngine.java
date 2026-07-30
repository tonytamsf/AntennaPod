package de.danoeh.antennapod.plugin.chaptermarkers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Chapter;

import java.io.File;
import java.util.List;

public interface ChapterMarkerEngine {

    boolean isAvailable();

    @Nullable
    List<Chapter> generateChapters(@NonNull Context context, @NonNull File mediaFile);
}
