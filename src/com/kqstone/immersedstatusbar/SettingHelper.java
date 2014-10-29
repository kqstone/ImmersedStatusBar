package com.kqstone.immersedstatusbar;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import de.robv.android.xposed.XSharedPreferences;

public class SettingHelper {
	private static final String KEY_COLOR = "color";
	private static final String KEY_OFFSET = "offset";
	private static final String KEY_TRANSLUCENT = "translucent";
	private XSharedPreferences mXPreferences;
	private SharedPreferences mPreferences;
	private Context mContext;
	
	public SettingHelper(String pkgname) {
		mXPreferences = new XSharedPreferences(Constant.PKG_NAME, pkgname);
	}
	
	public SettingHelper(Context context, String pkgName) {
		mContext = context;
		mPreferences = context.getSharedPreferences(pkgName, Context.MODE_WORLD_READABLE);
	}
	
	public int getColor(String actName) {
		String str = mXPreferences.getString(getKey(actName, KEY_COLOR), null);
		if (str != null) {
			return Color.parseColor("#" + str);
		} else {
			return Constant.UNKNOW_COLOR;
		}
	}
	
	public int getPaddingOffset(String actName) {
		return mXPreferences.getInt(getKey(actName, KEY_OFFSET), 0);
	}
	
	public boolean getTranslucent(String actName) {
		return mXPreferences.getBoolean(getKey(actName, KEY_TRANSLUCENT ), false);
	}
	
	public void writeColor(String actName, int color) {
		String hex = getHexFromColor(color);
		Editor edit = mPreferences.edit();
		edit.putString(getKey(actName, KEY_COLOR), hex);
		edit.commit();
		reload();
	}
	
	public void writePaddingOffset(String actName, int offset) {
		Editor edit = mPreferences.edit();
		edit.putInt(getKey(actName, KEY_OFFSET), offset);
		edit.commit();
		reload();
	}
	
	public void setTranslucent(String actName, boolean translucent) {
		Editor edit = mPreferences.edit();
		edit.putBoolean(getKey(actName, KEY_TRANSLUCENT), translucent);
		edit.commit();
		reload();
	}
	
	public void reload() {
		if (mPreferences != null)
			AndroidAppHelper.reloadSharedPreferencesIfNeeded(mPreferences);
		if (mXPreferences != null)
			mXPreferences.reload();
	}
	
	public boolean updateSettingsFromInternet() {
		//add download sources
		reload();
		return false;
		
	}
	private String getKey(String actName, String key) {
		return actName + "_" + key;
	}
	
	private String getHexFromColor(int color) {
		String r = Integer.toHexString(Color.red(color));
		if (r.length() ==1)
			r = "0"+r;
		String g = Integer.toHexString(Color.green(color));
		if (g.length() ==1)
			g = "0"+g;
		String b = Integer.toHexString(Color.blue(color));
		if (b.length() ==1)
			b = "0"+b;
		return r+g+b;
	}

}
