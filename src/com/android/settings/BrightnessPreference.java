/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import java.io.FileWriter;
import java.io.IOException;
import android.util.Log;

public class BrightnessPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {

    private SeekBar mSeekBar;
    private CheckBox mCheckBox;
    private CheckBox mCheckBoxUltra;

    private int mOldBrightness;
    private int mOldAutomatic;
    private int mOldUltraBrightness;
    private static final String TAG = "BrightnessPreference";

    private boolean mAutomaticAvailable;

    private boolean mRestoredOldState;

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private int mScreenBrightnessDim =
	    getContext().getResources().getInteger(com.android.internal.R.integer.config_screenBrightnessDim);
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessChanged();
        }
    };

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };

    private ContentObserver mBrightnessModeUltraObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessUltraModeChanged();
        }
    };

    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);

        setDialogLayoutResource(R.layout.preference_dialog_brightness);
        setDialogIcon(R.drawable.ic_settings_display);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mBrightnessModeObserver);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_ULTRA_MODE), true,
                mBrightnessModeUltraObserver);

        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(MAXIMUM_BACKLIGHT - mScreenBrightnessDim);
        mOldBrightness = getBrightness(0);
        mSeekBar.setProgress(mOldBrightness - mScreenBrightnessDim);

        mCheckBox = (CheckBox)view.findViewById(R.id.automatic_mode);
        if (mAutomaticAvailable) {
            mCheckBox.setOnCheckedChangeListener(this);
            mOldAutomatic = getBrightnessMode(0);
            mCheckBox.setChecked(mOldAutomatic != 0);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }
        mSeekBar.setOnSeekBarChangeListener(this);
        mOldUltraBrightness = getUltraBrightnessMode(0);
        Log.d(TAG,"mOldUltraBrighess was found as " + mOldUltraBrightness);
        mCheckBoxUltra = (CheckBox)view.findViewById(R.id.ultra_mode);
        mCheckBoxUltra.setOnCheckedChangeListener(this);
        mCheckBoxUltra.setChecked(mOldUltraBrightness != 0);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setBrightness(progress + mScreenBrightnessDim);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mCheckBoxUltra) {
            Log.d(TAG,"ultrabrightess set to "+isChecked);
            if (isChecked)
                writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm");
            else
                writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm_als");
            setUltraMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_ULTRA_ENABLED
                : Settings.System.SCREEN_BRIGHTNESS_MODE_ULTRA_DISABLED);
            return;
        }
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (!isChecked) {
            setBrightness(mSeekBar.getProgress() + mScreenBrightnessDim);
        }
    }

    private int getBrightness(int defaultValue) {
        int brightness = defaultValue;
        try {
            brightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException snfe) {
        }
        return brightness;
    }

    private int getBrightnessMode(int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private int getUltraBrightnessMode(int defaultValue) {
        int ultraMode = defaultValue;
        try {
            ultraMode = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_ULTRA_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return ultraMode;
    }

    private void onBrightnessChanged() {
        int brightness = getBrightness(MAXIMUM_BACKLIGHT);
        mSeekBar.setProgress(brightness - mScreenBrightnessDim);
    }

    private void onBrightnessModeChanged() {
        boolean checked = getBrightnessMode(0) != 0;
        mCheckBox.setChecked(checked);
    }

    private void onBrightnessUltraModeChanged() {
        boolean checked = getUltraBrightnessMode(0) != 0;
        mCheckBoxUltra.setChecked(checked);
        Log.d(TAG,"onBrightnessUltraModeChanged! " + checked);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = getContext().getContentResolver();

        if (positiveResult) {
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    mSeekBar.getProgress() + mScreenBrightnessDim);
        } else {
            restoreOldState();
        }

        resolver.unregisterContentObserver(mBrightnessObserver);
        resolver.unregisterContentObserver(mBrightnessModeObserver);
        resolver.unregisterContentObserver(mBrightnessModeUltraObserver);
    }

    private void restoreOldState() {
        if (mRestoredOldState) return;

        if (mAutomaticAvailable) {
            setMode(mOldAutomatic);
        }
        if (!mAutomaticAvailable || mOldAutomatic == 0) {
            setBrightness(mOldBrightness);
        }
        setUltraMode(mOldUltraBrightness);
        mRestoredOldState = true;
    }

    private void setBrightness(int brightness) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
            }
        } catch (RemoteException doe) {
            
        }
    }

    private void setMode(int mode) {
        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            mSeekBar.setVisibility(View.GONE);
        } else {
            mSeekBar.setVisibility(View.VISIBLE);
        }
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    private void setUltraMode(int mode) {
        Log.d(TAG,"Setting ultramode to " + mode);
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_ULTRA_MODE, mode);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.automatic = mCheckBox.isChecked();
        myState.ultra = mCheckBoxUltra.isChecked();
        Log.d(TAG,"Saved ultra: " + myState.ultra);
        myState.progress = mSeekBar.getProgress();
        myState.oldAutomatic = mOldAutomatic == 1;
        myState.oldUltra = mOldUltraBrightness == 1;
        myState.oldProgress = mOldBrightness;

        // Restore the old state when the activity or dialog is being paused
        restoreOldState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOldBrightness = myState.oldProgress;
        mOldAutomatic = myState.oldAutomatic ? 1 : 0;
        mOldUltraBrightness = myState.oldUltra ? 1 : 0;
        Log.d(TAG,"mOldUltraBrightness restored to " + mOldUltraBrightness);
        setMode(myState.automatic ? 1 : 0);
        setUltraMode(myState.ultra ? 1 : 0);
        setBrightness(myState.progress + mScreenBrightnessDim);
    }

    private static class SavedState extends BaseSavedState {

        boolean automatic;
        boolean oldAutomatic;
        boolean ultra;
        boolean oldUltra;
        int progress;
        int oldProgress;

        public SavedState(Parcel source) {
            super(source);
            automatic = source.readInt() == 1;
            ultra = source.readInt() == 1;
            progress = source.readInt();
            oldAutomatic = source.readInt() == 1;
            oldUltra = source.readInt() == 1;
            oldProgress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(automatic ? 1 : 0);
            dest.writeInt(ultra ? 1 : 0);
            dest.writeInt(progress);
            dest.writeInt(oldAutomatic ? 1 : 0);
            dest.writeInt(oldUltra ? 1 : 0);
            dest.writeInt(oldProgress);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static boolean writeOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }

}

