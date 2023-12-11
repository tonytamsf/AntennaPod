package de.danoeh.antennapod.model.feed;

import java.util.Map;
import java.util.TreeMap;

public class Transcript {

    private TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();
    private String rawString;

    public void addSegment(TranscriptSegment segment) {
        segmentsMap.put(segment.getStartTime(), segment);
    }

    public void replace(Long k1, Long k2) {
        Map.Entry<Long, TranscriptSegment> entry1 = (Map.Entry<Long, TranscriptSegment>) segmentsMap.floorEntry(k1);
        if (entry1 != null) {
            segmentsMap.remove(entry1.getKey());
            segmentsMap.put(k2, entry1.getValue());
        }
    }

    public TranscriptSegment getSegmentAtTime(long time) {
        if (segmentsMap.floorEntry(time) == null) {
            return null;
        }
        return segmentsMap.floorEntry(time).getValue();
    }

    public int getSegmentCount() {
        if (segmentsMap == null) {
           return 0;
        }
        return segmentsMap.size();
    }

    public Map.Entry<Long, TranscriptSegment> getEntryAfterTime(long time) {
        if (segmentsMap.ceilingEntry(time) == null) {
            return null;
        }
        return segmentsMap.ceilingEntry(time);
    }

    public TranscriptSegment remove(Map.Entry<Long, TranscriptSegment> entry) {
        if (entry == null) {
            return null;
        }
        return segmentsMap.remove(entry.getKey());
    }

    public void setRawString(java.lang.String rawString) {
        rawString.trim().replaceAll(" +", " ");
        this.rawString = rawString;
    }

    public String toString() {
        return rawString;
    }
}