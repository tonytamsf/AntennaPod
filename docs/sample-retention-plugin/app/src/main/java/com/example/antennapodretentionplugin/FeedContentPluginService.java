package com.example.antennapodretentionplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.plugin.host.IFeedContentPlugin;
import de.danoeh.antennapod.plugin.host.PluginContract;
import de.danoeh.antennapod.plugin.host.PluginFeedContentRequest;
import de.danoeh.antennapod.plugin.host.PluginFeedContentResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Rewrites a podcast feed before AntennaPod parses it, keeping only the newest N items. Episodes
 * beyond N never reach the app at all: they are not listed, not downloaded, and not counted.
 *
 * <p>N is the same per-podcast value that {@link RetentionPluginService} uses, configured in
 * {@link RetentionSettingsActivity}.</p>
 */
public class FeedContentPluginService extends Service {
    private static final String TAG = "RetentionFeedPlugin";
    private static final String PLUGIN_ID = "keep-newest-episodes";
    private static final String[] ITEM_TAGS = {"item", "entry"};
    private static final String[] DATE_TAGS = {"pubDate", "published", "updated", "date"};
    private static final String[] DATE_FORMATS = {
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    };

    private RetentionPreferences preferences;

    private final IFeedContentPlugin.Stub binder = new IFeedContentPlugin.Stub() {
        @Override
        public String getPluginId() {
            return PLUGIN_ID;
        }

        @Override
        public int getCapabilities() {
            return PluginContract.CAPABILITY_FEED_CONTENT;
        }

        @Override
        public PluginFeedContentResult processFeed(PluginFeedContentRequest request) {
            PluginFeedContentResult result = new PluginFeedContentResult();
            try {
                truncateFeed(request, result);
                result.setSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Feed rewrite failed", e);
                result.setSuccess(false);
                result.setModified(false);
                result.setMessage(e.getMessage());
            }
            return result;
        }
    };

    private void truncateFeed(PluginFeedContentRequest request, PluginFeedContentResult result)
            throws Exception {
        int keep = preferences.getKeepCount(request.getFeedId());
        if (keep == RetentionPreferences.KEEP_ALL) {
            Log.d(TAG, "Keeping all items of " + request.getFeedUrl());
            return;
        }
        Document document = parse(request.getInputFd());
        List<Element> items = findItems(document);
        Log.d(TAG, request.getFeedUrl() + " has " + items.size() + " items, keeping newest " + keep);
        if (items.size() <= keep) {
            return;
        }
        List<Element> surplus = selectSurplus(items, keep);
        for (Element item : surplus) {
            item.getParentNode().removeChild(item);
        }
        write(document, request.getOutputFd());
        result.setModified(true);
        result.setItemsRemoved(surplus.size());
    }

    private static Document parse(@Nullable ParcelFileDescriptor fd) throws Exception {
        if (fd == null) {
            throw new IllegalArgumentException("No feed to read");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream stream = new FileInputStream(fd.getFileDescriptor())) {
            return builder.parse(stream);
        }
    }

    private static void write(Document document, @Nullable ParcelFileDescriptor fd) throws Exception {
        if (fd == null) {
            throw new IllegalArgumentException("Nowhere to write the rewritten feed");
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        try (OutputStream stream = new FileOutputStream(fd.getFileDescriptor())) {
            transformer.transform(new DOMSource(document), new StreamResult(stream));
        }
    }

    private static List<Element> findItems(Document document) {
        List<Element> items = new ArrayList<>();
        for (String tag : ITEM_TAGS) {
            NodeList nodes = document.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element) {
                    items.add((Element) node);
                }
            }
            if (!items.isEmpty()) {
                return items;
            }
        }
        return items;
    }

    /**
     * Returns the items to drop. Feeds are conventionally newest-first, so document order decides
     * unless the items carry parseable dates, in which case the newest ones are kept.
     */
    private static List<Element> selectSurplus(List<Element> items, int keep) {
        List<Element> ordered = new ArrayList<>(items);
        if (allHaveDates(items)) {
            Collections.sort(ordered, new Comparator<Element>() {
                @Override
                public int compare(Element lhs, Element rhs) {
                    return Long.compare(dateOf(rhs), dateOf(lhs));
                }
            });
        }
        return new ArrayList<>(ordered.subList(keep, ordered.size()));
    }

    private static boolean allHaveDates(List<Element> items) {
        for (Element item : items) {
            if (dateOf(item) == 0) {
                return false;
            }
        }
        return true;
    }

    private static long dateOf(Element item) {
        for (String tag : DATE_TAGS) {
            NodeList nodes = item.getElementsByTagName(tag);
            if (nodes.getLength() == 0) {
                continue;
            }
            String text = nodes.item(0).getTextContent();
            if (text == null) {
                continue;
            }
            long parsed = parseDate(text.trim());
            if (parsed != 0) {
                return parsed;
            }
        }
        return 0;
    }

    private static long parseDate(String text) {
        String normalized = text.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
        for (String format : DATE_FORMATS) {
            try {
                Date date = new SimpleDateFormat(format, Locale.US).parse(normalized);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException e) {
                // Try the next format
            }
        }
        return 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new RetentionPreferences(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
