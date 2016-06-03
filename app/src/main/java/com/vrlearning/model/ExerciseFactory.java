package com.vrlearning.model;

import com.vrlearning.R;

/**
 * Created by juraj on 02/06/16.
 */
public class ExerciseFactory {

    public static Exercise sunsetExercise() {
        return Exercise.builder()
                .exerciseTitle("Sunset focus exercise")
                .videoName("video_sunset.mp4")
                .audioName("focus.mp3")
                .bckgAudioName("yodel.wav")
                .bckgAudioVolume(5)
                .imgResId(R.drawable.ic_sunset_focus_exercise)
                .build();
    }
}
