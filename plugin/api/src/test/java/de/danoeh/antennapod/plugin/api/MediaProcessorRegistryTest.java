package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class MediaProcessorRegistryTest {

    @After
    public void tearDown() {
        MediaProcessorRegistry.unregister("a");
        MediaProcessorRegistry.unregister("b");
    }

    private static class RecordingProcessor implements DownloadedMediaProcessor {
        final String id;
        final boolean handle;
        int processCalls = 0;

        RecordingProcessor(String id, boolean handle) {
            this.id = id;
            this.handle = handle;
        }

        @NonNull
        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean shouldProcess(@NonNull FeedMedia media) {
            return handle;
        }

        @Override
        public void process(@NonNull Context context, @NonNull FeedMedia media) {
            processCalls++;
        }
    }

    @Test
    public void runsOnlyMatchingProcessors() {
        RecordingProcessor matching = new RecordingProcessor("a", true);
        RecordingProcessor notMatching = new RecordingProcessor("b", false);
        MediaProcessorRegistry.register(matching);
        MediaProcessorRegistry.register(notMatching);

        Context context = ApplicationProvider.getApplicationContext();
        MediaProcessorRegistry.runAll(context, mock(FeedMedia.class));

        assertEquals(1, matching.processCalls);
        assertEquals(0, notMatching.processCalls);
    }

    @Test
    public void registerIsIdempotentById() {
        MediaProcessorRegistry.register(new RecordingProcessor("a", true));
        MediaProcessorRegistry.register(new RecordingProcessor("a", true));

        long count = MediaProcessorRegistry.getProcessors().stream()
                .filter(p -> p.getId().equals("a")).count();
        assertEquals(1, count);
    }

    @Test
    public void unregisterRemovesProcessor() {
        MediaProcessorRegistry.register(new RecordingProcessor("a", true));
        MediaProcessorRegistry.unregister("a");

        assertFalse(MediaProcessorRegistry.getProcessors().stream()
                .anyMatch(p -> p.getId().equals("a")));
    }
}
