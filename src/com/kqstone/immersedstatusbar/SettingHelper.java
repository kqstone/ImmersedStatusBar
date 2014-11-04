package com.kqstone.immersedstatusbar;

import java.io.File;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import de.robv.android.xposed.XSharedPreferences;

public class SettingHelper {
	private static final String KEY_COLOR = "color";
	private static final String KEY_OFFSET = "offset";
	private static final String KEY_TRANSLUCENT = "translucent";
	private static final String KEY_BACKGROUNDTYPE = "backgroundtype"; //background type: 0=color, 1=picture
	private static final String KEY_BACKGROUNDFILE = "backgroundfile";
	
	private static final String DIR_IMG = "/isb/img/";
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
	
	public int getBackgroundType(String actName) {
		return mXPreferences.getInt(getKey(actName, KEY_BACKGROUNDTYPE), 0);
	}
	
	public int getColor(String actName) {
		String str = mXPreferences.getString(getKey(actName, KEY_COLOR), null);
		if (str != null) {
			return Color.parseColor("#" + str);
		} else {
			return Constant.UNKNOW_COLOR;
		}
	}
	
	public String getBackgroundPath(String actName) {
		String path = Environment.getExternalStorageDirectory().getAbsolutePath();
		Utils.log("external storage path: " + path);
		path = path + DIR_IMG;
		String filename = mXPreferences.getString(getKey(actName, KEY_BACKGROUNDFILE), null);
		if (filename != null) {
			path = path + filename + ".png";
		} else {
			path = path + actName + ".png";
		}
		return path;
	}
	
	public Bitmap getBitmap(String actName) {
		return BitmapFactory.decodeFile(getBackgroundPath(actName));
	}
	
	public int getPaddingOffset(String actName) {
		return mXPreferences.getInt(getKey(actName, KEY_OFFSET), 0);
	}
	
	public boolean getTranslucent(String actName) {
		return mXPreferences.getBoolean(getKey(actName, KEY_TRANSLUCENT ), false);
	}
	
	public void writeBackgroundType(String actName, int type) {
		Editor edit = mPreferences.edit();
		edit.putInt(getKey(actName, KEY_BACKGROUNDTYPE), type);
		edit.commit();
		reload();
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
