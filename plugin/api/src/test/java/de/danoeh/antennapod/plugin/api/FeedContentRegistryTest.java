package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class FeedContentRegistryTest {
    private static final String FEED = "<rss><channel><item/></channel></rss>";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @After
    public void tearDown() {
        FeedContentRegistry.unregister("a");
        FeedContentRegistry.unregister("b");
    }

    private static class RecordingProcessor implements FeedContentProcessor {
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
        public boolean shouldProcess(long feedId, @NonNull String feedUrl) {
            return handle;
        }

        @Override
        public void process(@NonNull Context context, long feedId, @NonNull String feedUrl,
                            @NonNull File feedFile) {
            processCalls++;
        }
    }

    private File feedFile() throws IOException {
        File file = folder.newFile("feed.xml");
        Files.write(file.toPath(), FEED.getBytes(Charset.forName("UTF-8")));
        return file;
    }

    @Test
    public void runsOnlyMatchingProcessors() throws IOException {
        RecordingProcessor matching = new RecordingProcessor("a", true);
        RecordingProcessor notMatching = new RecordingProcessor("b", false);
        FeedContentRegistry.register(matching);
        FeedContentRegistry.register(notMatching);

        Context context = ApplicationProvider.getApplicationContext();
        FeedContentRegistry.runAll(context, 1, "http://example.com/feed.xml", feedFile());

        assertEquals(1, matching.processCalls);
        assertEquals(0, notMatching.processCalls);
    }

    @Test
    public void skipsMissingFile() throws IOException {
        RecordingProcessor processor = new RecordingProcessor("a", true);
        FeedContentRegistry.register(processor);

        FeedContentRegistry.runAll(ApplicationProvider.getApplicationContext(), 1,
                "http://example.com/feed.xml", new File(folder.getRoot(), "missing.xml"));

        assertEquals(0, processor.processCalls);
    }

    @Test
    public void unregisterRemovesProcessor() {
        FeedContentRegistry.register(new RecordingProcessor("a", true));
        FeedContentRegistry.unregister("a");

        assertFalse(FeedContentRegistry.getProcessors().stream()
                .anyMatch(p -> p.getId().equals("a")));
    }
}
