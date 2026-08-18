package de.danoeh.antennapod.plugin.api;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class EpisodeRetentionRegistryTest {

    @After
    public void tearDown() {
        EpisodeRetentionRegistry.unregister("a");
        EpisodeRetentionRegistry.unregister("b");
    }

    private static class FixedPolicy implements EpisodeRetentionPolicy {
        final String id;
        final boolean applies;
        final List<FeedItem> selection;

        FixedPolicy(String id, boolean applies, List<FeedItem> selection) {
            this.id = id;
            this.applies = applies;
            this.selection = selection;
        }

        @NonNull
        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean shouldApply(@NonNull Feed feed) {
            return applies;
        }

        @NonNull
        @Override
        public List<FeedItem> selectForDeletion(@NonNull Context context, @NonNull Feed feed,
                                                @NonNull List<FeedItem> downloadedItems) {
            return selection;
        }
    }

    private static Feed feed() {
        return new Feed("http://example.com/feed.xml", null, "Feed");
    }

    private static FeedItem item(long id) {
        FeedItem item = new FeedItem();
        item.setId(id);
        return item;
    }

    @Test
    public void mergesSelectionsWithoutDuplicates() {
        FeedItem first = item(1);
        FeedItem second = item(2);
        List<FeedItem> candidates = Arrays.asList(first, second);
        EpisodeRetentionRegistry.register(new FixedPolicy("a", true, Collections.singletonList(first)));
        EpisodeRetentionRegistry.register(new FixedPolicy("b", true, new ArrayList<>(candidates)));

        List<FeedItem> selected = EpisodeRetentionRegistry.selectForDeletion(
                ApplicationProvider.getApplicationContext(), feed(), candidates);

        assertEquals(2, selected.size());
        assertTrue(selected.contains(first));
        assertTrue(selected.contains(second));
    }

    @Test
    public void ignoresPoliciesThatDoNotApply() {
        FeedItem first = item(1);
        EpisodeRetentionRegistry.register(new FixedPolicy("a", false, Collections.singletonList(first)));

        List<FeedItem> selected = EpisodeRetentionRegistry.selectForDeletion(
                ApplicationProvider.getApplicationContext(), feed(), Collections.singletonList(first));

        assertTrue(selected.isEmpty());
    }

    @Test
    public void ignoresEpisodesThatWereNotOffered() {
        FeedItem candidate = item(1);
        FeedItem foreign = item(2);
        EpisodeRetentionRegistry.register(new FixedPolicy("a", true, Collections.singletonList(foreign)));

        List<FeedItem> selected = EpisodeRetentionRegistry.selectForDeletion(
                ApplicationProvider.getApplicationContext(), feed(), Collections.singletonList(candidate));

        assertTrue(selected.isEmpty());
    }

    @Test
    public void unregisterRemovesPolicy() {
        EpisodeRetentionRegistry.register(new FixedPolicy("a", true, Collections.emptyList()));
        EpisodeRetentionRegistry.unregister("a");

        assertFalse(EpisodeRetentionRegistry.getPolicies().stream()
                .anyMatch(p -> p.getId().equals("a")));
    }
}
