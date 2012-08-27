/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.Utils;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    private static final String CPU_SETTINGS_PROP = "sys.cpufreq.restored";
    private static final String IOSCHED_SETTINGS_PROP = "sys.iosched.restored";
    private static final String KSM_SETTINGS_PROP = "sys.ksm.restored";
    private static final String UNDERVOLTING_PROP = "persist.sys.undervolt";
    private static final String SWAP_FILE = "/mnt/sdcard/.MiniCM9.swp";
    private static final String SWAP_FILE_STAGING = "/mnt/secure/staging/.MiniCM9.swp";
    private static final String SWAP_ENABLED_PROP = "persist.sys.swap.enabled";
    private static final String SWAP_SIZE_PROP = "persist.sys.swap.size";
    private static String UV_MODULE;
    private static Context myContext;
    private static Intent mBkService = null;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        myContext = ctx;
        if (SystemProperties.getBoolean(CPU_SETTINGS_PROP, false) == false
                && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SystemProperties.set(CPU_SETTINGS_PROP, "true");
            configureCPU(ctx);
        } else {
            SystemProperties.set(CPU_SETTINGS_PROP, "false");
        }

        if (SystemProperties.getBoolean(IOSCHED_SETTINGS_PROP, false) == false
                && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SystemProperties.set(IOSCHED_SETTINGS_PROP, "true");
            configureIOSched(ctx);
        } else {
            SystemProperties.set(IOSCHED_SETTINGS_PROP, "false");
        }

        if (Utils.fileExists(MemoryManagement.KSM_RUN_FILE)) {
            if (SystemProperties.getBoolean(KSM_SETTINGS_PROP, false) == false
                    && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                SystemProperties.set(KSM_SETTINGS_PROP, "true");
                configureKSM(ctx);
            } else {
                SystemProperties.set(KSM_SETTINGS_PROP, "false");
            }
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (getUltraBrightnessMode(ctx, 0) == 1)
                writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm");
            else
                writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm_als");
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
            if (SystemProperties.get(SWAP_ENABLED_PROP).equals("1")) {
                Log.d(TAG, "SWAP_ENABLED_PROP: " + SWAP_ENABLED_PROP);
                String command = "swapon " + SWAP_FILE;
                mrunShellCommand = new runShellCommand(command, ctx.getResources().getString(com.android.settings.R.string.swap_toast_swap_enabled));
                mrunShellCommand.start();
            }
        }

        else if ( intent.getAction().equals(Intent.ACTION_SHUTDOWN) || intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
            if (SystemProperties.get(SWAP_ENABLED_PROP).equals("1")) {
                Log.d(TAG, "SWAP_ENABLED_PROP: " + SWAP_ENABLED_PROP);
                String command = "swapoff " + SWAP_FILE_STAGING;
                mrunShellCommand = new runShellCommand(command, ctx.getResources().getString(com.android.settings.R.string.swap_toast_swap_disabled));
                mrunShellCommand.start();
            }
        }
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (mBkService == null) {
                Log.d("Display and Keyboard Service", "Started");
                mBkService = new Intent(ctx, com.android.settings.cyanogenmod.BkService.class);
                ctx.startService(mBkService);
            }
        }
    }

    private class runShellCommand extends Thread {
        private String command = "";
        private String toastMessage = "";

        public runShellCommand(String command, String toastMessage) {
            this.command = command;
            this.toastMessage = toastMessage;
        }

        @Override
        public void run() {
        try {
            if (!command.equals("")) {
                Process process = Runtime.getRuntime().exec("su");
                Log.d(TAG, "Executing: " + command);
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                DataInputStream inputStream = new DataInputStream(process.getInputStream());
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                process.waitFor();
            }
            } catch (IOException e) {
                Log.e(TAG, "Thread IOException");
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread InterruptedException");
            }
            Message messageToThread = new Message();
            Bundle messageData = new Bundle();
            messageToThread.what = 0;
            messageData.putString("toastMessage", toastMessage);
            messageToThread.setData(messageData);
            mrunShellCommandHandler.sendMessage(messageToThread);
        }
    };

    private runShellCommand mrunShellCommand;

    private Handler mrunShellCommandHandler = new Handler() {
        public void handleMessage(Message msg) {
            CharSequence text="";
            Bundle messageData = msg.getData();
            text = messageData.getString("toastMessage", "");
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(myContext, text, duration);
            toast.show();
        }
    };

    private void configureCPU(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (prefs.getBoolean(Processor.SOB_PREF, false) == false) {
            SystemProperties.set(UNDERVOLTING_PROP, "0");
            Log.i(TAG, "Restore disabled by user preference.");
            return;
        }

        UV_MODULE = ctx.getResources().getString(com.android.settings.R.string.undervolting_module);
	    if (SystemProperties.getBoolean(UNDERVOLTING_PROP, false) == true) {
            // insmod undervolting module
            insmod(UV_MODULE, true);
        }
        else {
            // remove undervolting module
            //insmod(UV_MODULE, false);
	    }

        String governor = prefs.getString(Processor.GOV_PREF, null);
        String minFrequency = prefs.getString(Processor.FREQ_MIN_PREF, null);
        String maxFrequency = prefs.getString(Processor.FREQ_MAX_PREF, null);
        String availableFrequenciesLine = Utils.fileReadOneLine(Processor.FREQ_LIST_FILE);
        String availableGovernorsLine = Utils.fileReadOneLine(Processor.GOV_LIST_FILE);
        boolean noSettings = ((availableGovernorsLine == null) || (governor == null)) &&
                             ((availableFrequenciesLine == null) || ((minFrequency == null) && (maxFrequency == null)));
        List<String> frequencies = null;
        List<String> governors = null;

        if (noSettings) {
            Log.d(TAG, "No CPU settings saved. Nothing to restore.");
        } else {
            if (availableGovernorsLine != null){
                governors = Arrays.asList(availableGovernorsLine.split(" "));
            }
            if (availableFrequenciesLine != null){
                frequencies = Arrays.asList(availableFrequenciesLine.split(" "));
            }
            if (governor != null && governors != null && governors.contains(governor)) {
                Utils.fileWriteOneLine(Processor.GOV_FILE, governor);
            }
            if (maxFrequency != null && frequencies != null && frequencies.contains(maxFrequency)) {
                Utils.fileWriteOneLine(Processor.FREQ_MAX_FILE, maxFrequency);
            }
            if (minFrequency != null && frequencies != null && frequencies.contains(minFrequency)) {
                Utils.fileWriteOneLine(Processor.FREQ_MIN_FILE, minFrequency);
            }
            Log.d(TAG, "CPU settings restored.");
        }
    }

    private void configureIOSched(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (prefs.getBoolean(IOScheduler.SOB_PREF, false) == false) {
            Log.i(TAG, "Restore disabled by user preference.");
            return;
        }

        String ioscheduler = prefs.getString(IOScheduler.IOSCHED_PREF, null);
        String availableIOSchedulersLine = Utils.fileReadOneLine(IOScheduler.IOSCHED_LIST_FILE);
        boolean noSettings = ((availableIOSchedulersLine == null) || (ioscheduler == null));
        List<String> ioschedulers = null;

        if (noSettings) {
            Log.d(TAG, "No I/O scheduler settings saved. Nothing to restore.");
        } else {
            if (availableIOSchedulersLine != null){
                ioschedulers = Arrays.asList(availableIOSchedulersLine.replace("[", "").replace("]", "").split(" "));
            }
            if (ioscheduler != null && ioschedulers != null && ioschedulers.contains(ioscheduler)) {
                Utils.fileWriteOneLine(IOScheduler.IOSCHED_LIST_FILE, ioscheduler);
            }
            Log.d(TAG, "I/O scheduler settings restored.");
        }
    }

    private void configureKSM(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        boolean ksm = prefs.getBoolean(MemoryManagement.KSM_PREF, false);

        Utils.fileWriteOneLine(MemoryManagement.KSM_RUN_FILE, ksm ? "1" : "0");
        Log.d(TAG, "KSM settings restored.");
    }

    private int getUltraBrightnessMode(Context ctx, int defaultValue) {
        int ultraMode = defaultValue;
        try {
            ultraMode = Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_ULTRA_MODE);
        } catch (SettingNotFoundException snfe) {}
        return ultraMode;
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

    private static boolean insmod(String module, boolean insert) {
        String command;
    if (insert)
        command = "/system/bin/insmod /system/lib/modules/" + module;
    else
        command = "/system/bin/rmmod " + module;
        try {
            Process process = Runtime.getRuntime().exec("su");
            Log.d(TAG, "Executing: " + command);
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream()); 
            DataInputStream inputStream = new DataInputStream(process.getInputStream());
            outputStream.writeBytes(command + "\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
        }
        catch (IOException e) {
            return false;
        }
        catch (InterruptedException e) {
            return false;
        }
        return true;
    }
}
