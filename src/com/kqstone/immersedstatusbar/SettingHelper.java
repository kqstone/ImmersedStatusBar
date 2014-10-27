package com.kqstone.immersedstatusbar;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import de.robv.android.xposed.XSharedPreferences;

public class SettingHelper {
	public final static String PREF = "color";
	private XSharedPreferences mXPreferences;
	private SharedPreferences mPreferences;
	private static Context mContext;
	private static SettingHelper mInstance;	
	
	public SettingHelper(XSharedPreferences xprefs) {
		mXPreferences = xprefs;
	}
	
	private SettingHelper(SharedPreferences prefs) {
		mPreferences = prefs;
	}
	
	public static SettingHelper getInstance(Context context) {
		if (mInstance == null) {
			mContext = context;
			SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_WORLD_READABLE);
			mInstance = new SettingHelper(sp);
		}
		return mInstance;		
	}
	
	
	public int getColor(String pkgName, String actName) {
		return mXPreferences.getInt(getKey(pkgName, actName), Constant.UNKNOW_COLOR);
	}
	
	public void writeColor(String pkgName, String actName, int color) {
		Editor edit = mPreferences.edit();
		edit.putInt(getKey(pkgName, actName), color);
		edit.commit();
		Intent intent = new Intent(Constant.INTENT_UPDATE_SETTINGS);
		mContext.sendBroadcast(intent);
	}
	
	public void reload() {
		if (mPreferences != null)
			AndroidAppHelper.reloadSharedPreferencesIfNeeded(mPreferences);
		if (mXPreferences != null)
			mXPreferences.reload();
	}
	
	public boolean updateSettingsFromInternet() {
		//add download sources
		Intent intent = new Intent(Constant.INTENT_UPDATE_SETTINGS);
		mContext.sendBroadcast(intent);
		return false;
		
	}
	private String getKey(String pkgName, String actName) {
		return pkgName + "_" + actName;
	}

}
