package de.egh.easyloop.ui;

import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.egh.easyloop.R;
import de.egh.easyloop.helper.Constants;

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
		validate(sharedPref, key);
		updateSummary(sharedPref, key);
	}

	/** Updated summary of some particular preferences with its value */
	private void updateSummary(final SharedPreferences sharedPref,
			final String key) {

		// Case 2: Transform value to name of a ListPreference
		if (key.equals(Constants.SharedPreferences.PreferencesPlayNr.KEY) //
		) {
			final EditTextPreference pref = (EditTextPreference) findPreference(key);
			pref.setSummary(pref.getText());
		}
	}

	/** User input validation */
	private void validate(final SharedPreferences sharedPref, final String key) {
		// if (key.equals(Constants.SharedPref.Timeout.KEY) //
		// ) {
		// final EditTextPreference timeoutPref = (EditTextPreference)
		// findPreference(key);
		// int value = Integer
		// .parseInt(getString(R.string.preferencesTimeoutDefault));
		//
		// final String valueString = timeoutPref.getText();
		// try {
		// value = Integer.parseInt(valueString);
		// if (value < TIMEOUT_MIN_SEC)
		// value = TIMEOUT_MIN_SEC;
		// else if (value > TIMEOUT_MAX_SEC)
		// value = TIMEOUT_MAX_SEC;
		// } catch (final NumberFormatException e) {
		// // User input is not a number: Take default
		// }
		//
		// timeoutPref.setText(String.valueOf(value));
	}

}
