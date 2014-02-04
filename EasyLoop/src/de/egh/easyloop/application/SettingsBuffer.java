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

	private SettingsBuffer() {
	}

	public float getBufferSizeFactor() {
		return bufferSizeFactor;
	}

	public long getCountInTime() {
		return countInTime;
	}

	/** Updates all shared preferences buffered in this SettingsBuffer */
	public void refreshSharedPreferences(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		countInTime = (long) (1000 * Float.parseFloat(prefs.getString(
				Constants.SharedPreferences.COUNT_IN_TIME, "1.0")));
		bufferSizeFactor = Float.parseFloat(prefs.getString(
				Constants.SharedPreferences.BUFFER_SIZE, "1.0"));

		Log.v(TAG, "countInTime=" + countInTime);
		Log.v(TAG, "bufferSizeFactor=" + bufferSizeFactor);

	}
}
