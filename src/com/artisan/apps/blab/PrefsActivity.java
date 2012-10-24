package com.artisan.apps.blab;

import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Activity for setting
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 *
 */
public class PrefsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
}