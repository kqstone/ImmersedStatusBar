package com.kqstone.immersedstatusbar;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener{
	private final static String KEY_PREF_ABOUT = "about";
	public final static String KEY_PREF_FORCE_TINT = "key_force_tint";
	
	private Preference mPrefAbout;
	private CheckBoxPreference mPrefForceTint;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.title_settings);
		this.addPreferencesFromResource(R.xml.settings);
		mPrefAbout = findPreference(KEY_PREF_ABOUT);
		mPrefAbout.setOnPreferenceClickListener(this);
		mPrefForceTint = (CheckBoxPreference) findPreference(KEY_PREF_FORCE_TINT);
		mPrefForceTint.setChecked(Settings.System.getInt(getContentResolver(),SettingsActivity.KEY_PREF_FORCE_TINT, 0) == 1 ? true:false);
		mPrefForceTint.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		// TODO Auto-generated method stub
		String key = arg0.getKey();
		if (key.equals(KEY_PREF_ABOUT)) {
			Intent intent = new Intent();
			intent.setClass(this, About.class);
			this.startActivity(intent);
		}
		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		String key = arg0.getKey();
		if (key.equals(KEY_PREF_FORCE_TINT)) {
			Settings.System.putInt(getContentResolver(), KEY_PREF_FORCE_TINT, (Boolean)arg1 ? 1 : 0);
		}
		mPrefForceTint.setChecked(Settings.System.getInt(getContentResolver(),SettingsActivity.KEY_PREF_FORCE_TINT, 0) == 1 ? true:false);
		return false;
	}

}
