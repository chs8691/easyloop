package de.egh.easyloop.ui;

import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.egh.easyloop.R;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.application.SettingsBuffer;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		final SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		sharedPref.registerOnSharedPreferenceChangeListener(this);

		// Initialize preference's summary
		for (final Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
			updateSummary(sharedPref, entry.getKey());
		}

	}

	@Override
	protected void onResume() {

		super.onResume();

		final String savedOrientation = PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						Constants.SharedPreferences.Orientation.KEY,
						Constants.SharedPreferences.Orientation.SENSOR);
		if (savedOrientation
				.equals(Constants.SharedPreferences.Orientation.LANDSCAPE)) {
			if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (savedOrientation
				.equals(Constants.SharedPreferences.Orientation.PORTRAIT)) {
			if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPref,
			final String key) {

		updateSummary(sharedPref, key);

		// Copy to global buffer
		SettingsBuffer.getInstance().refreshSharedPreferences(this);
	}

	/** Updated summary of some particular preferences with its value */
	private void updateSummary(final SharedPreferences sharedPref,
			final String key) {

		// Case 2: Transform value to name of a ListPreference
		if (key.equals(Constants.SharedPreferences.COUNT_IN_TIME) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(getText(R.string.preferencesCountInTimeSummary)
					+ ": " + pref.getEntry() + " "
					+ getText(R.string.preferencesCountInTimeSummeryUnit));
		}
		// Screen orientation
		else if (key.equals(Constants.SharedPreferences.Orientation.KEY) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(pref.getEntry());
		}
		// Buffer Size
		else if (key.equals(Constants.SharedPreferences.BUFFER_SIZE) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(getString(R.string.preferencesBufferSizeSummary)
					+ " " + pref.getEntry());
		}

	}

}
