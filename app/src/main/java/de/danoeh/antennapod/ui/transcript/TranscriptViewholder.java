package de.danoeh.antennapod.ui.transcript;

import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.databinding.TranscriptItemBinding;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class TranscriptViewholder extends RecyclerView.ViewHolder {
    public final TextView viewTimecode;
    public final TextView viewContent;
    public TranscriptSegment transcriptSegment;

    public TranscriptViewholder(TranscriptItemBinding binding) {
        super(binding.getRoot());
        viewTimecode = binding.speaker;
        viewContent = binding.content;

        viewContent.setOnClickListener(v -> {
            long startTime = transcriptSegment.getStartTime();
            long endTime = transcriptSegment.getEndTime();
            /*
            if (! (controller.getPosition() >= startTime
                    && controller.getPosition() <= endTime)) {
                controller.seekTo((int) startTime);

                if (controller.getStatus() == PlayerStatus.PAUSED
                        || controller.getStatus() == PlayerStatus.STOPPED) {
                    controller.playPause();
                }
            } else {
                controller.playPause();
            }
             */
        });
    }

    @Override
    public String toString() {
        return super.toString() + " '" + viewContent.getText() + "'";
    }
}