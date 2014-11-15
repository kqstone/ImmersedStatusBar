package com.kqstone.immersedstatusbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

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
import android.util.Xml;
import de.robv.android.xposed.XSharedPreferences;

public class ProfileHelper {
	public static final String KEY_NAME = "name";
	public static final String KEY_COLOR = "color";
	public static final String KEY_OFFSET = "offset";
	public static final String KEY_BACKGROUNDTYPE = "backgroundtype"; //background type: 0=color, 1=picture, 2= translucent status;
	public static final String NAME_ALL_ACTIVITIES = "AllActivities";
	private static final String DIR_IMG = "/isb/img/";
	private static final String DIR_PROFILE = "/isb/profile/";
	private static final String DIR_USERPROFILE = "/isb/usrprofile/";
	
	
	private String mProfileName;
	private String mActName;
	
	private ActivityProfile mProfile, mUserProfile;
	private boolean mHasProfile = false, mHasUserProfile = false;;
	
	public ProfileHelper(String pkgname) {
		mProfileName = pkgname;
	}
	
	public void initiateProfile(String activityName) {
		mActName = activityName;
		mProfile = null;
		String profilePath = buildPath(DIR_PROFILE, mProfileName, "xml");
		String userProfilePath = buildPath(DIR_USERPROFILE, mProfileName, "xml");
		try {
			mProfile = getProfileFromXml(activityName, profilePath);
			if (mProfile != null)
				mHasProfile = true;
			Utils.log("Profile \n" + mProfile.getActName() + "\n" + mProfile.getBgType() + "\n" + mProfile.getBgColor());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			mUserProfile = getProfileFromXml(activityName, userProfilePath);
			if (mUserProfile != null)
				mHasUserProfile = true;
			Utils.log("UserProfile \n" + mProfile.getActName() + "\n" + mProfile.getBgType() + "\n" + mProfile.getBgColor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasProfile() {
		if(mHasProfile || mHasUserProfile)
			return true;
		return false;
	}
	
	public  boolean hasUserProfile() {
		return mHasUserProfile;
	}
	
	public int getBackgroundType() {
		Integer i = null;
		if (hasUserProfile())
			i = mUserProfile.getBgType();
		if (i == null && hasProfile())
			i = mProfile.getBgType();
		if (i == null)
			i = 0;
		return i;
	}
	
	public int getColor() {
		String s = null;
		if (hasUserProfile())
			s = mUserProfile.getBgColor();
		if (s == null && hasProfile())
			s = mProfile.getBgColor();
		if (s == null)
			s = "000000";
		return Color.parseColor("#" + s);
	}
	
	public String getBackgroundPath() {
		String filename = mActName;
		filename = mProfileName + "/" + filename;
		return buildPath(DIR_IMG, filename, "png");
	}
	
	public Bitmap getBitmap() {
		return BitmapFactory.decodeFile(getBackgroundPath());
	}
	
	public int getPaddingOffset() {
		Integer i = null;
		if (hasUserProfile())
			i = mUserProfile.getOffset();
		if (i == null && hasProfile())
			i = mProfile.getOffset();
		if (i == null)
			i = 0;
		return i;
	}
	
	public String getActName() {
		return mActName;
	}
	
//	public boolean getTranslucent(String actName) {
//	}
	
	private ActivityProfile getProfileFromXml(String activityName, String filePath) throws XmlPullParserException, NumberFormatException, IOException {
		FileInputStream slideInputStream = new FileInputStream(filePath);  
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();   
        factory.setNamespaceAware(true);   
        XmlPullParser xpp = factory.newPullParser();  
        xpp.setInput(slideInputStream, "UTF-8");  
        int eventType = xpp.getEventType();  
        ActivityProfile profile = null, profileAll = null, profileTemp = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
        	switch (eventType) {
        	case XmlPullParser.START_TAG:
        		String tag = xpp.getName(); 
        		if (tag.equalsIgnoreCase("activity")) {
        			String actName = xpp.getAttributeValue(null, KEY_NAME);
        			if (activityName.equalsIgnoreCase(actName)){
        				profile = new ActivityProfile();
        				profile.setActName(actName);
        			} else if (actName.equalsIgnoreCase(NAME_ALL_ACTIVITIES)) {
        				profileAll = new ActivityProfile();
        				profileAll.setActName(NAME_ALL_ACTIVITIES);
        			}
        			
        		} else {
        			if (profile != null) {
        				if (tag.equalsIgnoreCase(KEY_BACKGROUNDTYPE))
        					profile.setBgType(Integer.parseInt(xpp.nextText()));
        				if (tag.equalsIgnoreCase(KEY_COLOR))
        					profile.setBgColor(xpp.nextText());
        				if (tag.equalsIgnoreCase(KEY_OFFSET))
        					profile.setOffset(Integer.parseInt(xpp.nextText()));
        			} else if (profileAll != null) {
        				if (tag.equalsIgnoreCase(KEY_BACKGROUNDTYPE))
        					profileAll.setBgType(Integer.parseInt(xpp.nextText()));
        				if (tag.equalsIgnoreCase(KEY_COLOR))
        					profileAll.setBgColor(xpp.nextText());
        				if (tag.equalsIgnoreCase(KEY_OFFSET))
        					profileAll.setOffset(Integer.parseInt(xpp.nextText()));
        			}
        		}
        		break;
        	case XmlPullParser.END_TAG:
        		String endtag = xpp.getName();
        		if (endtag.equalsIgnoreCase("activity")) {
        			if (profileTemp == null && profileAll != null) {
        				profileTemp = profileAll;
        				profileAll = null;
        			} else if (profile != null) {
        				profileTemp = profile;
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
		return profileTemp; 
	}
	
	private String buildPath(String relativePath, String filename, String extension) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
			return dir + relativePath + filename + "." + extension;
		}
		return null;
	}

}
