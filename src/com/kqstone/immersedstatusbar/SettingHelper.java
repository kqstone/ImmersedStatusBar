package com.kqstone.immersedstatusbar;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import de.robv.android.xposed.XSharedPreferences;

public class SettingHelper {
	private XSharedPreferences mXPreferences;
	private SharedPreferences mPreferences;
	private Context mContext;
	
	public SettingHelper(String pkgname) {
		mXPreferences = new XSharedPreferences(Constant.PKG_NAME, pkgname);
	}
	
	public SettingHelper(Context context, String pkgName) {
		mContext = context;
		SharedPreferences sp = context.getSharedPreferences(pkgName, Context.MODE_WORLD_READABLE);
	}
	
	public int getColor(String actName) {
		String str = mXPreferences.getString(getKey(actName), null);
		if (str != null) {
			return Color.parseColor("#" + str);
		} else {
			return Constant.UNKNOW_COLOR;
		}
	}
	
	public void writeColor(String pkgName, String actName, int color) {
		Editor edit = mPreferences.edit();
		String r = Integer.toHexString(Color.red(color));
		String g = Integer.toHexString(Color.green(color));
		String b = Integer.toHexString(Color.blue(color));
		edit.putString(actName, r+g+b);
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
	private String getKey(String actName) {
		return actName;
	}

}
