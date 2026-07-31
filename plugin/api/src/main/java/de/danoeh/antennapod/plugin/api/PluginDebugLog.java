package de.danoeh.antennapod.plugin.api;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, live-observable log of all interactions between the app and media processor plugins.
 * Events are also forwarded to logcat so they remain visible via {@code adb logcat}.
 * Used to power the on-device plugin debug overlay.
 */
public final class PluginDebugLog {
    public static final int LEVEL_DEBUG = Log.DEBUG;
    public static final int LEVEL_INFO = Log.INFO;
    public static final int LEVEL_WARN = Log.WARN;
    public static final int LEVEL_ERROR = Log.ERROR;

    private static final int MAX_EVENTS = 500;
    private static final Deque<Event> events = new ArrayDeque<>();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private PluginDebugLog() {
    }

    public static final class Event {
        private final long timestamp;
        private final int level;
        private final String tag;
        private final String message;

        Event(long timestamp, int level, String tag, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getLevel() {
            return level;
        }

        @NonNull
        public String getTag() {
            return tag;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    public interface Listener {
        void onPluginDebugEvent(@NonNull Event event);
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        log(LEVEL_DEBUG, tag, message, null);
    }

    public static void i(@NonNull String tag, @NonNull String message) {
        log(LEVEL_INFO, tag, message, null);
    }

    public static void w(@NonNull String tag, @NonNull String message) {
        log(LEVEL_WARN, tag, message, null);
    }

    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        String text = throwable == null ? message : message + ": " + throwable;
        log(LEVEL_ERROR, tag, text, throwable);
    }

    private static void log(int level, String tag, String message, @Nullable Throwable throwable) {
        Log.println(level, tag, throwable == null ? message : message + '\n' + Log.getStackTraceString(throwable));
        Event event = new Event(System.currentTimeMillis(), level, tag, message);
        synchronized (PluginDebugLog.class) {
            events.addLast(event);
            while (events.size() > MAX_EVENTS) {
                events.removeFirst();
            }
        }
        for (Listener listener : listeners) {
            listener.onPluginDebugEvent(event);
        }
    }

    @NonNull
    public static synchronized List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public static synchronized void clear() {
        events.clear();
    }

    public static void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }
}
