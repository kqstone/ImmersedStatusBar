package com.kqstone.immersedstatusbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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

public class ProfileHelper {
	private static final String KEY_NAME = "name";
	private static final String KEY_COLOR = "color";
	private static final String KEY_OFFSET = "offset";
	private static final String KEY_TRANSLUCENT = "translucent";
	private static final String KEY_BACKGROUNDTYPE = "backgroundtype"; //background type: 0=color, 1=picture
	private static final String KEY_BACKGROUNDFILE = "backgroundfile";
	
	private static final String DIR_IMG = "/isb/img/";
	private static final String DIR_PROFILE = "/isb/profile/";
	
	private String mProfileName;
	private String mActName;
	
	private ActivityProfile mProfile;
	
	public ProfileHelper(String pkgname) {
		mProfileName = pkgname;
	}
	
	public void initiateProfile(String activityName) throws XmlPullParserException, NumberFormatException, IOException {
		mActName = activityName;
		mProfile = null;
		String profilePath = buildPath(DIR_PROFILE, mProfileName, "xml");
		FileInputStream slideInputStream = new FileInputStream(profilePath);  
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();   
        factory.setNamespaceAware(true);   
        XmlPullParser xpp = factory.newPullParser();  
        xpp.setInput(slideInputStream, "UTF-8");  
        int eventType = xpp.getEventType();  
        ActivityProfile profile = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
        	switch (eventType) {
        	case XmlPullParser.START_TAG:
        		String tag = xpp.getName(); 
        		if (tag.equalsIgnoreCase("activity")) {
        			String actName = xpp.getAttributeValue(null, KEY_NAME);
        			if (!activityName.equalsIgnoreCase(actName))
        				break;
                	profile = new ActivityProfile();
        			profile.setActName(actName);
        		} else {
        			if (profile != null) {
        				if (tag.equalsIgnoreCase(KEY_BACKGROUNDTYPE))
        					profile.setBgType(Integer.parseInt(xpp.nextText()));
        				if (tag.equalsIgnoreCase(KEY_BACKGROUNDFILE))
        					profile.setBgPath(xpp.nextText());
        				if (tag.equalsIgnoreCase(KEY_COLOR))
        					profile.setBgColor(xpp.nextText());
        				if (tag.equalsIgnoreCase(KEY_OFFSET))
        					profile.setOffset(Integer.parseInt(xpp.nextText()));
        			}
        		}
        		break;
        	case XmlPullParser.END_TAG:
        		String endtag = xpp.getName();
        		if (endtag.equalsIgnoreCase("activity")) {
        			if (profile != null) {
        				mProfile = profile;
        				profile = null;
        			}
        		}
        		break;
        	default: break;
        	}
        	eventType = xpp.next();
        }
        if (slideInputStream != null) {  
            slideInputStream.close();  
            slideInputStream = null;  
        }  
        Utils.log("Profile \n" + mProfile.getActName() + "\n" + mProfile.getBgType() + "\n" + mProfile.getBgColor());
	}
	
	public int getBackgroundType() {
		return mProfile.getBgType();
	}
	
	public int getColor() {
		return Color.parseColor("#" + mProfile.getBgColor());
	}
	
	public String getBackgroundPath() {
		String path = mProfile.getBgPath();
		if (path == null) {
			path = mActName;
		}
		return buildPath(DIR_IMG, path, "png");
	}
	
	public Bitmap getBitmap() {
		return BitmapFactory.decodeFile(getBackgroundPath());
	}
	
	public int getPaddingOffset() {
		return mProfile.getOffset();
	}
	
	public String getActName() {
		return mActName;
	}
	
//	public boolean getTranslucent(String actName) {
//	}
	
	private String buildPath(String relativePath, String filename, String extension) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
			return dir + relativePath + filename + "." + extension;
		}
		return null;
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
