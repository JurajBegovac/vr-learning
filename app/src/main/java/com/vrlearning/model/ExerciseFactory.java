package com.vrlearning.model;

import com.vrlearning.R;

/**
 * Created by juraj on 02/06/16.
 */
public class ExerciseFactory {

    public static Exercise sunsetFocus() {
        return Exercise.builder()
                .exerciseTitle("Sunset focus exercise")
                .videoName("video_sunset.mp4")
                .audioName("focus.mp3")
                .bckgAudioName("yodel.wav")
                .bckgAudioVolume(5)
                .imgResId(R.drawable.ic_sunset_focus_exercise)
                .build();
    }

    public static Exercise bavarianAlpsInnerPeace() {
        return Exercise.builder()
                .exerciseTitle("Bavarian Alps Inner peace")
                .videoName("video_sunset.mp4")
                .audioName("inner_peace.mp3")
                .bckgAudioName("yodel.wav")
                .bckgAudioVolume(5)
                .imgResId(R.drawable.ic_bavarian_alps_inner_peace)
                .build();
    }

    public static Exercise dreamBeachConfidence() {
        return Exercise.builder()
                .exerciseTitle("Dream Beach Confidence")
                .videoName("video_more.mp4")
                .audioName("confidence.mp3")
                .bckgAudioName("waves.wav")
                .bckgAudioVolume(2)
                .imgResId(R.drawable.ic_dream_beach_confidence)
                .build();
    }
}
