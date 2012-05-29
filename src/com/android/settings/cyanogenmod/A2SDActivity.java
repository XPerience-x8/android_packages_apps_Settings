/*
 * Copyright (C) 2012 nobodyAtall @ xda
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

import com.android.settings.R;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.preference.CheckBoxPreference;
import android.os.SystemProperties;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.*;

public class A2SDActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    public static final String A2SD_MODE_PREF = "pref_a2sd_mode";

    private static final String TAG = "A2SDSettings";

    private static final String EXT_PROP = "ev.sdextpart";

    private String mModeFormat;

    private ListPreference mA2sdPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String temp;
        mModeFormat = getString(R.string.a2sd_mode_summary);
        String[] availableModes = { "none", "a2sd", "dc2sd" };
        setTitle(R.string.a2sd_title);
        addPreferencesFromResource(R.xml.a2sd_settings);
        PreferenceScreen PrefScreen = getPreferenceScreen();
        mA2sdPref = (ListPreference) PrefScreen.findPreference(A2SD_MODE_PREF);
        temp = getCurMode();
        if (!ext_exists()) {
            mA2sdPref.setSummary("No ext partition found!");
            mA2sdPref.setEnabled(false);
        }
        else {
            mA2sdPref.setEntryValues(availableModes);
            mA2sdPref.setEntries(availableModes);
            mA2sdPref.setValue(temp);
            mA2sdPref.setSummary(String.format(mModeFormat, temp));
            mA2sdPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        String temp;
        super.onResume();
        temp = getCurMode();
        mA2sdPref.setValue(temp);
        mA2sdPref.setSummary(String.format(mModeFormat, temp));

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mA2sdPref) {
        	    setMode((String) newValue);
        	    return true;
        	}
        }
        return false;
    }

    public static String getCurMode() {
        File file_dc2sd = new File("/sd-ext/.dc2sd");
        boolean exists_dc2sd = file_dc2sd.exists();
        if (!exists_dc2sd) {
            File file_a2sd = new File("/sd-ext/.a2sd");
            boolean exists_a2sd = file_a2sd.exists();
            if (!exists_a2sd)
                return "none";
            else
                return "a2sd";
        }
        else {
            return "dc2sd";
        }
    }

    public void setMode(String mode) {
        if (mode.equals("none")) {
            runA2sd(0);
        }
        else if (mode.equals("a2sd")) {
            File file_a2sd = new File("/sd-ext/.a2sd");
            boolean exists_a2sd = file_a2sd.exists();
            if (!exists_a2sd) {
                runA2sd(1);
            }
            File file_dc2sd = new File("/sd-ext/.dc2sd");
            if (file_dc2sd.exists())
                file_dc2sd.delete();
        }
        else if (mode.equals("dc2sd")) {
            File file_dc2sd = new File("/sd-ext/.dc2sd");
            boolean exists_dc2sd = file_dc2sd.exists();
            if (!exists_dc2sd) {
                runA2sd(2);
            }
        }
        String temp = getCurMode();
        mA2sdPref.setValue(temp);
        mA2sdPref.setSummary(String.format(mModeFormat, temp));
    }

    public static boolean ext_exists() {
        String ext_partition = SystemProperties.get(EXT_PROP);
        if (ext_partition.equals(""))
            return false;
        else {
            File file=new File(ext_partition);
            if (!file.exists()) {
                Log.e(TAG, ext_partition + " does not exist!");
                return false;
            }
            else {
                Log.e(TAG, ext_partition + " exists!");
                return true;
            }
        }
    }

    public static int runA2sd(int mode) {
        String command = "";
        if (mode == 0) //remove a2sd
            command = "echo \"n\" | /system/bin/a2sd remove";
        else if (mode == 1) //a2sd
            command = "echo \"n\" | /system/bin/a2sd install";
        else if (mode == 2) //dc2sd
            command = "echo \"y\" | /system/bin/a2sd install";
        if (!command.equals("")) {
            try {
                Process process = Runtime.getRuntime().exec("su");
                Log.e(TAG, "Executing: " + command);
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream()); 
                DataInputStream inputStream = new DataInputStream(process.getInputStream());
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n"); 
                outputStream.flush(); 
                process.waitFor();
            }
            catch (IOException e) {
                return -1;
            }
            catch (InterruptedException e) {
                return -2;
            }
            return 0;
        }
        return -3;
    }
}

