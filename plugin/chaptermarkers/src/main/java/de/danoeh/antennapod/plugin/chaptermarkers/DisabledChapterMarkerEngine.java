package de.danoeh.antennapod.plugin.chaptermarkers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Chapter;

import java.io.File;
import java.util.List;

public class DisabledChapterMarkerEngine implements ChapterMarkerEngine {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Nullable
    @Override
    public List<Chapter> generateChapters(@NonNull Context context, @NonNull File mediaFile) {
        return null;
    }
}
