package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Looper;
import android.util.Log;

public class LightsEvent extends BroadcastReceiver {
	Context cntxt = null;
	String mStrAction = null;
	LightsController controller;

	public LightsEvent(LightsController controller) {
		this.controller = controller;
	}

	private void changeBkLightOfButtons(Context paramContext, String paramString) {

		if (paramString.equals("android.intent.action.CONFIGURATION_CHANGED")) {
			Log.d("Changed Configuration", "Received a change in Configuration");

			if (paramContext.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
				controller.lockOffButtonBkLight();
				Log.d("Keyboard Listener", "Keyboard was slid in");
			}
			if (paramContext.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
				controller.lockOnButtonBkLight();
				Log.d("Keyboard Listener", "Keyboard was slid out");
			}

		}
	}

	public LightsController getController() {
		return this.controller;
	}

	@Override
	public void onReceive(Context paramContext, Intent paramIntent) {
		// TODO Auto-generated method stub
		this.mStrAction = paramIntent.getAction();
		this.cntxt = paramContext;

		Log.i("LightsEvent", "onReceive");
		new Thread() {
			public void run() {
				Looper.prepare();
				LightsEvent.this.changeBkLightOfButtons(
						LightsEvent.this.cntxt,
						LightsEvent.this.mStrAction);
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

}
