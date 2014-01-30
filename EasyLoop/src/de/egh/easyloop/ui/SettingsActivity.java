package de.egh.easyloop.ui;

import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
		} else if (key.equals(Constants.SharedPreferences.BUFFER_SIZE) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(getString(R.string.preferencesBufferSizeSummary)
					+ " x " + pref.getEntry());
		} else if (key.equals(Constants.SharedPreferences.FADE_IN) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(getString(R.string.preferencesFadeInSummary) + " "
					+ pref.getEntry());
		} else if (key.equals(Constants.SharedPreferences.FADE_OUT) //
		) {
			final ListPreference pref = (ListPreference) findPreference(key);
			pref.setSummary(getString(R.string.preferencesFadeOutSummary) + " "
					+ pref.getEntry());
		}

	}

}
