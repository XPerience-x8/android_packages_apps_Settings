package com.android.settings.cyanogenmod;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class LightsController {
	public static String LCD_BUTTON_BACKLIGHT = "/sys/class/leds/lcd-backlight/brightness";
	static String LED_BUTTON_BACKLIGHT_Brightness = "/sys/devices/platform/msm_pmic_misc_led.0/brightness";
	static String LED_BUTTON_MODE_user = "/sys/devices/platform/msm_pmic_misc_led.0/control::mode";
	static String LED_BUTTON_BACKLIGHT_Current_ma = "/sys/devices/platform/msm_pmic_misc_led.0/max::current_ma";
	static String LED_BUTTON_BACKLIGHT_cut_off = "/sys/devices/platform/msm_pmic_misc_led.0/als::cut-off";
	static String DISPLAY_BRIGHTNESS1 = "/sys/devices/platform/i2c-adapter/i2c-0/0-0036/br::intensity";
	static String DISPLAY_BRIGHTNESS2 = "/sys/devices/i2c-0/0-0036/br::intensity";

	public LightsController(final Context cnxt) {

		SensorManager sensorManager = (SensorManager) cnxt
				.getSystemService(Context.SENSOR_SERVICE);
		Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		SensorEventListener lightSensorEvent = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {

				if (event.sensor.getType() == Sensor.TYPE_LIGHT) {

					/*Log.d("LightSensor Changed", "Lightsensor value: "
							+ event.values[0]);*/
					try {
						if (Settings.System.getInt(cnxt.getContentResolver(),
								Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
							if (event.values[0] == -1) {
								WriteDisplay(10);
							}
							if (event.values[0] == 0) {
								WriteDisplay(20);
							}
							if (event.values[0] <= 16) {
								WriteDisplay(30);
							} else {
								WriteDisplay((int) event.values[0]);
							}
						}
					} catch (SettingNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Check if keyboard exists
					File f = new File(LED_BUTTON_BACKLIGHT_Brightness);
					if (f.isFile() && f.canRead()) {
						if (cnxt.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
							lockOffButtonBkLight();
							Log.d("LightSensor Changed",
									"Keyboard is hidden but Lightsensor changed,Turning Off Keyboard Light");
						}
						if (event.values[0] >= 64
								&& cnxt.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
							lockOffButtonBkLight();
							Log.d("LightSensor Changed",
									"Keyboard is open but Lightsensor reports Light");
						}

						if (event.values[0] <= 0
								&& cnxt.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
							lockOffButtonBkLight();
							Log.d("LightSensor Woke up",
									"Turning Off Keyboard Light");
						}

						if (event.values[0] < 64
								&& cnxt.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
							lockOnButtonBkLight();
							Log.d("LightSensor Reported",
									"Its Dark.Turning On Keyboard Light");
						}
					}
				}

			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}
		};
		sensorManager.registerListener(lightSensorEvent, lightSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		initialize();
	}

	public static void WriteDisplay(int text) {
		try {
			File f = new File(DISPLAY_BRIGHTNESS1);
			FileWriter fstream;
			if (f.isFile() && f.canRead()) {
				fstream = new FileWriter(DISPLAY_BRIGHTNESS1);
			}
			else {
				fstream = new FileWriter(DISPLAY_BRIGHTNESS2);
			}
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(String.valueOf(text));
			out.close();
			fstream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void WriteFile(String text, String file) {
		File f = new File(file);
		if (f.isFile() && f.canRead()) {
			try {
				FileWriter fstream = new FileWriter(file);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(text);
				out.close();
				fstream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void initialize() {
		Log.e("LC", "Initialize()");
		//WriteFile("user", LED_BUTTON_MODE_user);
		//WriteFile("40", LED_BUTTON_BACKLIGHT_Current_ma);
		//WriteFile("1", LED_BUTTON_BACKLIGHT_cut_off);

	}

	public void lockOffButtonBkLight() {
		//WriteFile("0", LED_BUTTON_BACKLIGHT_Current_ma);
		WriteFile("0", LED_BUTTON_BACKLIGHT_Brightness);
	}

	public void lockOnButtonBkLight() {
		initialize();
		//WriteFile("40", LED_BUTTON_BACKLIGHT_Current_ma);
		WriteFile("220", LED_BUTTON_BACKLIGHT_Brightness);

	}

}
