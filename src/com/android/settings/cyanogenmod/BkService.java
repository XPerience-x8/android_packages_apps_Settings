package com.android.settings.cyanogenmod;


import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class BkService extends Service {
	LightsEvent mOriEvnt = null;
	LightsController controller;

	public IBinder onBind(Intent paramIntent) {
		return null;
	}

	public void onCreate() {
		// Log.i("Service","Oncreate");
		this.controller = new LightsController(getApplicationContext());
		this.mOriEvnt = new LightsEvent(controller);
		super.onCreate();
	}

	public void onDestroy() {
		// Log.i("Service","onDestroy()");
		if (this.mOriEvnt != null)
			;
		try {
			unregisterReceiver(this.mOriEvnt);
			super.onDestroy();
			return;
		} catch (Exception localException) {

			localException.printStackTrace();
		}
	}

	public void onStart(Intent paramIntent, int paramInt) {
		try {
			// Log.i("Service","onStart()");
			IntentFilter localIntentFilter = new IntentFilter(
					"android.intent.action.CONFIGURATION_CHANGED");
			registerReceiver(this.mOriEvnt, localIntentFilter);

			// super.onStartCommand(paramIntent, START_FLAG_REDELIVERY,
			// paramInt);
			super.onStart(paramIntent, paramInt);
			return;
		} catch (Exception localException) {

			localException.printStackTrace();
		}
	}
}
