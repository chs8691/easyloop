package de.egh.easyloop.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Buffer for preferences can be used outside of an activity context, also in a
 * unbound service. You only have to initialize the SettingsBuffer with the
 * refresh method at Application start up and every time, when SharedPreferences
 * have been changed.
 */
public class SettingsBuffer {

	private static SettingsBuffer SETTINGS = new SettingsBuffer();

	private final static String TAG = "SettingsBuffer";

	public static SettingsBuffer getInstance() {
		return SETTINGS;
	}

	private float bufferSizeFactor;
	private long countInTime;
	private int fadeIn;
	private int fadeOut;

	private SettingsBuffer() {
	}

	public float getBufferSizeFactor() {
		return bufferSizeFactor;
	}

	public long getCountInTime() {
		return countInTime;
	}

	public int getFadeIn() {
		return fadeIn;
	}

	public int getFadeOut() {
		return fadeOut;
	}

	/** Updates all shared preferences buffered in this SettingsBuffer */
	public void refreshSharedPreferences(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		countInTime = (long) (1000 * Float.parseFloat(prefs.getString(
				Constants.SharedPreferences.COUNT_IN_TIME, "1.0")));
		bufferSizeFactor = Float.parseFloat(prefs.getString(
				Constants.SharedPreferences.BUFFER_SIZE, "1.0"));
		fadeIn = Integer.parseInt(prefs.getString(
				Constants.SharedPreferences.FADE_IN, "0"));
		fadeOut = Integer.parseInt(prefs.getString(
				Constants.SharedPreferences.FADE_OUT, "0"));

		Log.v(TAG, "countInTime=" + countInTime);
		Log.v(TAG, "bufferSizeFactor=" + bufferSizeFactor);
		Log.v(TAG, "fadeIn=" + fadeIn);
		Log.v(TAG, "fadeOut=" + fadeOut);

	}
}
