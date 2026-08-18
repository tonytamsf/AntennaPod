package de.danoeh.antennapod.debug;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.plugin.api.PluginDebugLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A floating, draggable overlay that shows every interaction between the app and media processor
 * plugins in real time, backed by {@link PluginDebugLog}. Intended for on-device debugging only.
 */
public final class PluginDebugOverlay implements PluginDebugLog.Listener {
    @SuppressLint("StaticFieldLeak")
    private static final PluginDebugOverlay INSTANCE = new PluginDebugOverlay();
    private static final int MAX_VISIBLE_CHARS = 40000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View rootView;
    private LinearLayout bodyContainer;
    private TextView logView;
    private ScrollView scrollView;
    private Button collapseButton;
    private boolean collapsed = false;

    private PluginDebugOverlay() {
    }

    public static PluginDebugOverlay getInstance() {
        return INSTANCE;
    }

    public boolean isShowing() {
        return rootView != null;
    }

    /**
     * Ensures the overlay permission is granted, launching the system settings screen if not.
     *
     * @return true if the overlay can be shown right now, false if a permission request was started.
     */
    public boolean ensurePermission(@NonNull Activity activity) {
        if (Settings.canDrawOverlays(activity)) {
            return true;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
        return false;
    }

    public void toggle(@NonNull Context context) {
        if (isShowing()) {
            hide();
        } else {
            show(context);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void show(@NonNull Context context) {
        if (isShowing()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams = new WindowManager.LayoutParams(
                dp(appContext, 300), dp(appContext, 260), overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = dp(appContext, 8);
        layoutParams.y = dp(appContext, 80);

        rootView = buildView(appContext);
        windowManager.addView(rootView, layoutParams);
        renderAll();
        PluginDebugLog.addListener(this);
        PluginDebugLog.info("PluginDebugOverlay", "Debug overlay attached. Waiting for plugin activity"
                + " (plugins run when an episode finishes downloading).");
    }

    public void hide() {
        PluginDebugLog.removeListener(this);
        if (windowManager != null && rootView != null) {
            windowManager.removeView(rootView);
        }
        rootView = null;
        bodyContainer = null;
        logView = null;
        scrollView = null;
        collapseButton = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private View buildView(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(0xEE000000);
        container.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        LinearLayout titleBar = new LinearLayout(context);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Plugin debug");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setPadding(dp(context, 6), 0, 0, 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleBar.addView(title, titleParams);

        Button clearButton = smallButton(context, "Clear");
        clearButton.setOnClickListener(v -> {
            PluginDebugLog.clear();
            renderAll();
        });
        titleBar.addView(clearButton);

        collapseButton = smallButton(context, "–");
        collapseButton.setOnClickListener(v -> setCollapsed(!collapsed));
        titleBar.addView(collapseButton);

        Button closeButton = smallButton(context, "✕");
        closeButton.setOnClickListener(v -> hide());
        titleBar.addView(closeButton);

        titleBar.setOnTouchListener(new DragListener());
        container.addView(titleBar);

        scrollView = new ScrollView(context);
        logView = new TextView(context);
        logView.setTextColor(Color.WHITE);
        logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(dp(context, 6), dp(context, 4), dp(context, 6), dp(context, 4));
        logView.setTextIsSelectable(true);
        scrollView.addView(logView);

        bodyContainer = new LinearLayout(context);
        bodyContainer.setOrientation(LinearLayout.VERTICAL);
        bodyContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(bodyContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return container;
    }

    private void setCollapsed(boolean value) {
        collapsed = value;
        if (bodyContainer != null) {
            bodyContainer.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
        if (collapseButton != null) {
            collapseButton.setText(collapsed ? "+" : "–");
        }
        if (layoutParams != null && windowManager != null && rootView != null) {
            layoutParams.height = collapsed
                    ? WindowManager.LayoutParams.WRAP_CONTENT : dp(rootView.getContext(), 260);
            windowManager.updateViewLayout(rootView, layoutParams);
        }
    }

    private Button smallButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(0x33FFFFFF);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(context, 4);
        button.setLayoutParams(params);
        return button;
    }

    private void renderAll() {
        if (logView == null) {
            return;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (PluginDebugLog.Event event : PluginDebugLog.getEvents()) {
            appendEvent(builder, event);
        }
        logView.setText(builder);
        autoScroll();
    }

    private void appendEvent(SpannableStringBuilder builder, PluginDebugLog.Event event) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        int start = builder.length();
        builder.append(timeFormat.format(new Date(event.getTimestamp())))
                .append(' ')
                .append(shortTag(event.getTag()))
                .append("  ")
                .append(event.getMessage());
        builder.setSpan(new ForegroundColorSpan(colorForLevel(event.getLevel())),
                start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String shortTag(String tag) {
        switch (tag) {
            case "PluginManager": return "[disc]";
            case "MediaProcessorRegistry": return "[reg ]";
            case "RemoteMediaProcessor": return "[proc]";
            case "PluginDebugOverlay": return "[over]";
            default: return "[" + tag + "]";
        }
    }

    private int colorForLevel(int level) {
        switch (level) {
            case PluginDebugLog.LEVEL_ERROR: return 0xFFFF6E6E;
            case PluginDebugLog.LEVEL_WARN: return 0xFFFFD166;
            case PluginDebugLog.LEVEL_INFO: return 0xFF8BE9FD;
            default: return 0xFFB0B0B0;
        }
    }

    private void autoScroll() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    @Override
    public void onPluginDebugEvent(@NonNull PluginDebugLog.Event event) {
        mainHandler.post(() -> {
            if (logView == null) {
                return;
            }
            if (logView.getText().length() > MAX_VISIBLE_CHARS) {
                renderAll();
                return;
            }
            SpannableStringBuilder builder = new SpannableStringBuilder(logView.getText());
            appendEvent(builder, event);
            logView.setText(builder);
            autoScroll();
        });
    }

    private final class DragListener implements View.OnTouchListener {
        private int initialX;
        private int initialY;
        private float touchX;
        private float touchY;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = layoutParams.x;
                    initialY = layoutParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    layoutParams.x = initialX + (int) (event.getRawX() - touchX);
                    layoutParams.y = initialY + (int) (event.getRawY() - touchY);
                    if (windowManager != null && rootView != null) {
                        windowManager.updateViewLayout(rootView, layoutParams);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
