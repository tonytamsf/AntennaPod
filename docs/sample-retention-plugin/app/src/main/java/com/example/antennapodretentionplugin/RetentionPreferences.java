package com.example.antennapodretentionplugin;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stores how many episodes to keep, globally and per podcast. Feeds are remembered as AntennaPod
 * asks about them, so the settings screen can offer a per-podcast value for each of them.
 */
public class RetentionPreferences {
    public static final int KEEP_ALL = 0;
    private static final String PREF_NAME = "retention";
    private static final String KEY_DEFAULT_KEEP = "defaultKeep";
    private static final String KEY_KEEP_PREFIX = "keep_";
    private static final String KEY_TITLE_PREFIX = "title_";
    private static final int DEFAULT_KEEP = 5;

    private final SharedPreferences prefs;

    public RetentionPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getDefaultKeepCount() {
        return prefs.getInt(KEY_DEFAULT_KEEP, DEFAULT_KEEP);
    }

    public void setDefaultKeepCount(int count) {
        prefs.edit().putInt(KEY_DEFAULT_KEEP, Math.max(count, KEEP_ALL)).apply();
    }

    public boolean hasKeepCount(long feedId) {
        return prefs.contains(KEY_KEEP_PREFIX + feedId);
    }

    public int getKeepCount(long feedId) {
        return prefs.getInt(KEY_KEEP_PREFIX + feedId, getDefaultKeepCount());
    }

    public void setKeepCount(long feedId, int count) {
        prefs.edit().putInt(KEY_KEEP_PREFIX + feedId, Math.max(count, KEEP_ALL)).apply();
    }

    public void clearKeepCount(long feedId) {
        prefs.edit().remove(KEY_KEEP_PREFIX + feedId).apply();
    }

    public void rememberFeed(long feedId, String title) {
        if (title == null) {
            return;
        }
        prefs.edit().putString(KEY_TITLE_PREFIX + feedId, title).apply();
    }

    public List<KnownFeed> getKnownFeeds() {
        List<KnownFeed> feeds = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(KEY_TITLE_PREFIX)) {
                continue;
            }
            long feedId = Long.parseLong(entry.getKey().substring(KEY_TITLE_PREFIX.length()));
            feeds.add(new KnownFeed(feedId, String.valueOf(entry.getValue())));
        }
        Collections.sort(feeds, (lhs, rhs) -> lhs.title.compareToIgnoreCase(rhs.title));
        return feeds;
    }

    public static class KnownFeed {
        public final long id;
        public final String title;

        KnownFeed(long id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}
