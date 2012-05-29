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
import android.text.TextUtils;
import android.util.Config;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import java.io.DataOutputStream;
import java.io.DataInputStream;

public class A2SDInfo extends AlertActivity {

    private static final String APP2SDINFO_PATH = "/data/app2sdinfo.log";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runA2sd(0);
        runA2sd(1);

        InputStreamReader inputReader = null;
        StringBuilder data = null;
        try {
            data = new StringBuilder(2048);
            char tmp[] = new char[2048];
            int numRead;
            inputReader = new FileReader(APP2SDINFO_PATH);
            while ((numRead = inputReader.read(tmp)) >= 0) {
                data.append(tmp, 0, numRead);
            }
        } catch (IOException e) {
            showErrorAndFinish();
            return;
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (IOException e) {
            }
        }

        if (TextUtils.isEmpty(data)) {
            showErrorAndFinish();
            return;
        }

        WebView webView = new WebView(this);

        // Begin the loading.  This will be done in a separate thread in WebView.
        webView.loadDataWithBaseURL(null, data.toString(), "text/plain", "utf-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Change from 'Loading...' to the real title
                mAlert.setTitle(getString(R.string.a2sd_dialog));
            }
        });

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.a2sd_loading);
        p.mView = webView;
        p.mForceInverseBackground = true;
        setupAlert();
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.a2sd_error, Toast.LENGTH_LONG)
                .show();
        finish();
    }
    
    public static int runA2sd(int mode) {
        String command = "";
        if (mode == 0)
            command = "a2sd partlist > /data/app2sdinfo.log";
        else if (mode == 1) //a2sd
            command = "a2sd diskspace >> /data/app2sdinfo.log";
        if (!command.equals("")) {
            try {
                Process process = Runtime.getRuntime().exec("su");
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

