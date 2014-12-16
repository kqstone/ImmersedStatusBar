package com.kqstone.immersedstatusbar;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityOnResumeHook extends XC_MethodHook {
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Activity activity = (Activity) param.thisObject;
		Utils.logoutActivityInform(activity);
		
		sendChangeStatusBarIntent(activity);
	}
	
	public void sendChangeStatusBarIntent(Activity activity) {
		int backgroundtype = 0;
		int color = Color.BLACK;
		String path = null;
		boolean colorHandled = false;
		boolean isdark = false;
		
		Boolean fastTrans = null;
		Object OFastTrans = XposedHelpers.getAdditionalInstanceField(activity, "mFastTrans");
		if (OFastTrans != null)
			fastTrans = (Boolean)OFastTrans;
		if (fastTrans == null) {
			for (String s:FastTransApp) {
				if (s.equals(activity.getPackageName())){
					fastTrans = true;
					break;
				}
			}
		}
		if (fastTrans == null) {
			ProfileHelper helper = (ProfileHelper) XposedHelpers.getAdditionalInstanceField(activity, "mProfileHelper");
			if (helper != null) {
				fastTrans = helper.getFastTrans();
			}
		}
		if (fastTrans == null) fastTrans = false;
		XposedHelpers.setAdditionalInstanceField(activity, "mFastTrans", fastTrans);		
		
		Object objColor;
		backgroundtype = (Integer) XposedHelpers.getAdditionalInstanceField(activity,
				"mBackgroundType");
		switch (backgroundtype) {
		case 1:
			path = (String) XposedHelpers.getAdditionalInstanceField(activity,
					"mBackgroundPath");
			isdark = (Boolean) XposedHelpers
					.getAdditionalInstanceField(activity, "mDarkMode");
			colorHandled = true;
			break;
		case 0:
			objColor = XposedHelpers.getAdditionalInstanceField(activity,
					"mStatusBarBackground");
			if (objColor != null) {
				color = (Integer) objColor;
				Utils.log("get color from mStatusBarBackground:" + color);
				isdark = (Boolean) XposedHelpers
						.getAdditionalInstanceField(activity, "mDarkMode");
				colorHandled = true;
				XposedHelpers.setAdditionalInstanceField(activity,
						"mNeedGetColorFromBackground", false);
			break;
			}
		}
		
		if (colorHandled) {
			Utils.sendTintStatusBarIntent(activity, backgroundtype, color, path, isdark, fastTrans);
			return;
		}
		
		WindowType type = Utils.getWindowType(activity);
		Utils.log("Resume: Window type: " + type);
		switch (type) {
		case Float:
			return;
		case Fullscreen:
			color = Color.parseColor("#33000000");
			colorHandled = true;
			isdark = false;
			break;
		case Translucent:
			objColor = XposedHelpers.getAdditionalInstanceField(activity, "mStatusBarBackground");
			if (objColor != null){
				color = (Integer) objColor;//transparent
				colorHandled = true;
				isdark = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mDarkMode");
				XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", false);
			} else {
				Utils.log("Translucent activity, need get darkmode after window focus changed");
			}
			break;
		default:
			for (String[] ignorApp:IgnorApp) {
				if (ignorApp[0].equals(activity.getPackageName()) && ignorApp[1].equals(activity.getLocalClassName()))
					return;
			}
			boolean exinform = Settings.System.getInt(activity.getContentResolver(), Constant.KEY_PREF_EXPORT_INFORM, 0) ==1 ? true:false;
			if (exinform)
				try {
					Utils.logStandXml(activity);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			boolean exinformtofile = Settings.System.getInt(activity.getContentResolver(), Constant.KEY_PREF_EXPORT_INFORM_TOFILE, 0) ==1 ? true:false;
			if (exinformtofile)
				Utils.exportStandXml(activity);
			
			XposedHelpers.setAdditionalInstanceField(activity, "mContentChangeTimes",0);
			
			Drawable drawable = null;
			if (!colorHandled) {
				ProfileHelper helper = (ProfileHelper) XposedHelpers.getAdditionalInstanceField(activity, "mProfileHelper");
				if (helper != null) {
					XposedHelpers.setAdditionalInstanceField(activity,
							"mHasProfile", true);
					backgroundtype = helper.getBackgroundType();
					XposedHelpers.setAdditionalInstanceField(activity,
							"mBackgroundType", backgroundtype);
					switch (backgroundtype) {
					case 0:
						color = helper.getColor();
						XposedHelpers.setAdditionalInstanceField(activity,
								"mStatusBarBackground", color);

						isdark = Utils.getDarkMode(color);
						XposedHelpers.setAdditionalInstanceField(activity,
								"mDarkMode", isdark);
						colorHandled = true;
						int k = helper.getPaddingOffset();
						if (k != 0) {
							Utils.resetPadding(activity, k);
						}
						drawable = new ColorDrawable(color);
						break;
					case 1:
						path = helper.getBackgroundPath();
						XposedHelpers.setAdditionalInstanceField(activity,
								"mBackgroundPath", path);
						Bitmap tempmap = helper.getBitmap();
						isdark = Utils.getDarkMode(Utils
								.getBitmapColor(tempmap).Color);
						XposedHelpers.setAdditionalInstanceField(activity,
								"mDarkMode", isdark);
						colorHandled = true;
						int j = helper.getPaddingOffset();
						if (j != 0) {
							Utils.resetPadding(activity, j);
						}
						drawable = new BitmapDrawable(tempmap);
						break;
					}
				}
			}
			if (!colorHandled) {
				ActionBar actionBar = activity.getActionBar();
				if (actionBar != null) {
					FrameLayout container = (FrameLayout) XposedHelpers
							.getObjectField(actionBar, "mContainerView");
					if (container != null) {
						Drawable backgroundDrawable = (Drawable) XposedHelpers
								.getObjectField(container, "mBackground");
						if (backgroundDrawable != null) {
							try {
								color = Utils
										.getMainColorFromActionBarDrawable(backgroundDrawable);
								actionBar
										.setBackgroundDrawable(new ColorDrawable(
												color));
								XposedHelpers.setAdditionalInstanceField(
										activity, "mStatusBarBackground",
										color);
								isdark = Utils.getDarkMode(color);
								XposedHelpers.setAdditionalInstanceField(
										activity, "mDarkMode", isdark);
								colorHandled = true;
								drawable = new ColorDrawable(color);
							} catch (IllegalArgumentException e) {
							}
							container.invalidate();
						}
					}
				}
			}
			
			if (drawable != null && !(Boolean)XposedHelpers.getAdditionalInstanceField(activity, "mHasSetWindowBackground")) {
				Utils.setDecorViewBackground(activity, drawable, false);
				XposedHelpers.setAdditionalInstanceField(activity, "mHasSetWindowBackground", true);
			}

			break;
		}

		if (!colorHandled) {
			XposedHelpers.setAdditionalInstanceField(activity,
					"mNeedGetColorFromBackground", true);
			Utils.log("can't handle color, need to get color from drawcache after widow focus changed");
			return;
		}
		
		Utils.sendTintStatusBarIntent(activity, backgroundtype, color, path, isdark, fastTrans);
	}
	
	public static final String[] FastTransApp = {
			"com.miui.home",
			"com.UCMobile",
			"com.tencent.mm",
			"com.sina.weibo"
	};
	
	public static final String[][] IgnorApp = {
		{"com.baidu.netdisk", "ui.MainActivity"}
	};
}
