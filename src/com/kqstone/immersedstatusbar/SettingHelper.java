package com.kqstone.immersedstatusbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import de.robv.android.xposed.XSharedPreferences;

public class SettingHelper {
	public final static String PREF = "color";
	private XSharedPreferences mXPreferences;
	private static SettingHelper mInstance;	
	
	private SettingHelper(XSharedPreferences xprefs) {
		mXPreferences = xprefs;
	}
	
	public static SettingHelper getInstance(XSharedPreferences xprefs) {
		if (mInstance == null) {
			mInstance = new SettingHelper(xprefs);
		}
		return mInstance;		
	}
	
	public int getColor(String pkgName, String actName) {
		return mXPreferences.getInt(getKey(pkgName, actName), Constant.UNKNOW_COLOR);
	}
	
	public void writeColor(String pkgName, String actName, int color) {
		Editor edit = mXPreferences.edit();
		edit.putInt(getKey(pkgName, actName), color);
		edit.commit();
	}
	
	private String getKey(String pkgName, String actName) {
		return pkgName + "_" + actName;
	}

}
