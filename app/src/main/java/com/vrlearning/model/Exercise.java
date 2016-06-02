package com.vrlearning.model;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * Created by juraj on 02/06/16.
 */
@AutoParcel
public abstract class Exercise implements Parcelable {

    abstract public String videoName();
    abstract public String audioName();
    abstract public String bckgAudioName();
    abstract public float bckgAudioVolume();

    public static Builder builder() {
        return new AutoParcel_Exercise.Builder();
    }

    @AutoParcel.Builder
    abstract public static class Builder {
        abstract public Builder videoName(String name);
        abstract public Builder audioName(String name);
        abstract public Builder bckgAudioName(String name);
        abstract public Builder bckgAudioVolume(float volume);
        abstract public Exercise build();
    }
}
