package com.example.antennapodretentionplugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Lets the user pick how many episodes to keep, globally and for each podcast AntennaPod has asked
 * about. AntennaPod itself only offers the extension point; the rule and its settings live here.
 */
public class RetentionSettingsActivity extends Activity {
    private RetentionPreferences preferences;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new RetentionPreferences(this);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        container.setPadding(padding, padding, padding, padding);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        container.removeAllViews();
        container.addView(headline("Keep the newest episodes"));
        container.addView(caption("Older episodes are cut out of the feed before AntennaPod reads it, so "
                + "only the newest ones ever appear. Episodes already downloaded are removed during the "
                + "next automatic cleanup; queued and favorite episodes are never deleted."));
        container.addView(row("All podcasts", describe(preferences.getDefaultKeepCount()),
                view -> askForCount("All podcasts", preferences.getDefaultKeepCount(), false, count -> {
                    preferences.setDefaultKeepCount(count);
                    render();
                })));

        List<RetentionPreferences.KnownFeed> feeds = preferences.getKnownFeeds();
        if (feeds.isEmpty()) {
            container.addView(caption("No podcasts yet. They appear here once AntennaPod has run its "
                    + "cleanup with this plugin enabled."));
            return;
        }
        container.addView(headline("Per podcast"));
        for (RetentionPreferences.KnownFeed feed : feeds) {
            String value = preferences.hasKeepCount(feed.id)
                    ? describe(preferences.getKeepCount(feed.id))
                    : "Default (" + describe(preferences.getDefaultKeepCount()) + ")";
            container.addView(row(feed.title, value,
                    view -> askForCount(feed.title, preferences.getKeepCount(feed.id), true, count -> {
                        if (count < 0) {
                            preferences.clearKeepCount(feed.id);
                        } else {
                            preferences.setKeepCount(feed.id, count);
                        }
                        render();
                    })));
        }
    }

    private interface CountListener {
        void onCount(int count);
    }

    private void askForCount(String title, int current, boolean allowDefault, CountListener listener) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        int padding = dp(16);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Number of episodes to keep (0 keeps all episodes)")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        listener.onCount(Math.max(Integer.parseInt(input.getText().toString()), 0));
                    } catch (NumberFormatException e) {
                        // Keep the previous value
                    }
                })
                .setNegativeButton("Cancel", null);
        if (allowDefault) {
            builder.setNeutralButton("Use default", (dialog, which) -> listener.onCount(-1));
        }
        builder.show();
    }

    private static String describe(int count) {
        return count == RetentionPreferences.KEEP_ALL ? "All" : String.valueOf(count);
    }

    private View row(String title, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label);

        Button button = new Button(this);
        button.setText(value);
        button.setOnClickListener(listener);
        row.addView(button);
        return row;
    }

    private TextView headline(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        view.setPadding(0, dp(16), 0, dp(8));
        return view;
    }

    private TextView caption(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
