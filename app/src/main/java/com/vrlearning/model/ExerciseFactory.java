package com.vrlearning.model;

/**
 * Created by juraj on 02/06/16.
 */
public class ExerciseFactory {

    public static Exercise sunsetExercise() {
        return Exercise.builder()
                .videoName("video_sunset.mp4")
                .audioName("focus.mp3")
                .bckgAudioName("yodel.wav")
                .bckgAudioVolume(5)
                .build();
    }
}
