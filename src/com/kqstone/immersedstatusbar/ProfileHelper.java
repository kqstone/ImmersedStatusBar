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
	private static final String KEY_NAME = "name";
	private static final String KEY_COLOR = "color";
	private static final String KEY_OFFSET = "offset";
	private static final String KEY_BACKGROUNDTYPE = "backgroundtype"; //background type: 0=color, 1=picture, 2= translucent status;
	private static final String NAME_ALL_ACTIVITIES = "AllActivities";
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
        ActivityProfile profileAll = null;
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
        			if (mProfile == null && profileAll != null) {
        				mProfile = profileAll;
        				profileAll = null;
        			} else if (profile != null) {
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
		String filename = mActName;
		filename = mProfileName + "/" + filename;
		return buildPath(DIR_IMG, filename, "png");
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

	public static String genStandXml(String pkgName, String actName) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			// <?xml version="1.0" encoding=¡±UTF-8¡å standalone=¡±yes¡±?>
			serializer.startDocument("UTF-8", true);
			// <profile>
			serializer.startTag(null, "profile");
			// <activity name="actName">
			serializer.startTag(null, "activity");
			serializer.attribute(null, ProfileHelper.KEY_NAME, actName);
			// <backgroundtype>Android XML</backgroundtype>
			serializer.startTag(null, ProfileHelper.KEY_BACKGROUNDTYPE);
			serializer
					.text("replace this text with 0 or 1 (0 = color, 1=image)");
			serializer.endTag(null, ProfileHelper.KEY_BACKGROUNDTYPE);
			// <color>Android XML</color>
			serializer.startTag(null, ProfileHelper.KEY_COLOR);
			serializer.text("replace this text with RGB(like c6c6c6)");
			serializer.endTag(null, ProfileHelper.KEY_COLOR);
			// <offset>Android XML</offset>
			serializer.startTag(null, ProfileHelper.KEY_OFFSET);
			serializer.text("replace this text with offset value (like 5)");
			serializer.endTag(null, ProfileHelper.KEY_OFFSET);
			// </activity>
			serializer.endTag(null, "activity");
			// </profile>
			serializer.startTag(null, "profile");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String genStandXmls(String pkgName, String actName) {
		StringWriter writer = new StringWriter();
		writer.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>");
		writer.append("\n");
		writer.append("<profile>");
		writer.append("\n");
		writer.append("<activity name=\"" + actName + "\">");
		writer.append("\n");
		writer.append("\t");
		writer.append("<backgroundtype>replace with 0 or 1</backgroundtype>");
		writer.append("\n");
		writer.append("\t");
		writer.append("<color>replace with RGB value</color>");
		writer.append("\n");
		writer.append("\t");
		writer.append("<offset>replace with offset value</offset>");
		writer.append("\n");
		writer.append("</activity>");
		writer.append("\n");
		writer.append("</profile>");
		return writer.toString();
	}
	
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
