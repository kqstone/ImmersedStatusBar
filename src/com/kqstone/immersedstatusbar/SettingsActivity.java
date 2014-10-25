package com.kqstone.immersedstatusbar;

import de.robv.android.xposed.XposedHelpers;
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
	
	
	private Preference mPrefAbout;
	private CheckBoxPreference mPrefForceTint;
	private CheckBoxPreference mPreTintNotification;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.title_settings);
		this.addPreferencesFromResource(R.xml.settings);
		mPrefAbout = findPreference(KEY_PREF_ABOUT);
		mPrefAbout.setOnPreferenceClickListener(this);
		mPrefForceTint = (CheckBoxPreference) findPreference(Constant.KEY_PREF_FORCE_TINT);
		mPrefForceTint.setChecked(Settings.System.getInt(getContentResolver(),Constant.KEY_PREF_FORCE_TINT, 0) == 1 ? true:false);
		mPrefForceTint.setOnPreferenceChangeListener(this);
		mPreTintNotification = (CheckBoxPreference) findPreference(Constant.KEY_PREF_TINT_NOTIFICATION);
		mPreTintNotification.setChecked(Settings.System.getInt(getContentResolver(), Constant.KEY_PREF_TINT_NOTIFICATION, 0) ==1 ? true:false);
		mPreTintNotification.setOnPreferenceChangeListener(this);
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
		boolean checked = (Boolean)arg1;
		if (key.equals(Constant.KEY_PREF_FORCE_TINT)) {
			Settings.System.putInt(getContentResolver(), Constant.KEY_PREF_FORCE_TINT, checked ? 1 : 0);
			mPrefForceTint.setChecked(checked);
		} else if (key.equals(Constant.KEY_PREF_TINT_NOTIFICATION)) {
			Settings.System.putInt(getContentResolver(), Constant.KEY_PREF_TINT_NOTIFICATION, checked ? 1 : 0);
			this.mPreTintNotification.setChecked(checked);
//			Intent intent = new Intent(Constant.INTENT_UPDATE_NOTIFICATION_ICONS);
//			this.sendBroadcast(intent);
		}
		
		return false;
	}

}