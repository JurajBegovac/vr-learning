package com.vrlearning.model;

import android.os.Parcelable;
import android.support.annotation.DrawableRes;

import auto.parcel.AutoParcel;

/**
 * Created by juraj on 02/06/16.
 */
@AutoParcel
public abstract class Exercise implements Parcelable {

    abstract public String exerciseTitle();
    abstract public String videoName();
    abstract public String audioName();
    abstract public String bckgAudioName();
    abstract public float bckgAudioVolume();
    @DrawableRes
    abstract public int imgResId();

    public static Builder builder() {
        return new AutoParcel_Exercise.Builder();
    }

    @AutoParcel.Builder
    abstract public static class Builder {
        abstract public Builder exerciseTitle(String title);
        abstract public Builder videoName(String name);
        abstract public Builder audioName(String name);
        abstract public Builder bckgAudioName(String name);
        abstract public Builder bckgAudioVolume(float volume);
        abstract public Builder imgResId(@DrawableRes int resId);
        abstract public Exercise build();
    }
}
