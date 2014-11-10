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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
		boolean darkHandled = false;
		
		boolean isSysApp = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mIsSystemApp");
		if (isSysApp) {
			Utils.log("System app, change color to transparent");
			color = Color.TRANSPARENT;
			colorHandled = true;
		} else {
			WindowType type = Utils.getWindowType(activity);
			switch (type) {
			case Float:
				return;
			case Fullscreen:
				color = Color.parseColor("#66000000");
				colorHandled = true;
				isdark = false;
				darkHandled = true;
				break;
			case Translucent:
				Utils.log("Translucent activity, need get darkmode after window focus changed");
				return;
			default:
				boolean exinform = Settings.System.getInt(activity.getContentResolver(), Constant.KEY_PREF_EXPORT_INFORM, 0) ==1 ? true:false;
				if (exinform) {
					Utils.logStandXml(activity);
					Utils.exportStandXml(activity);
				}
				XposedHelpers.setAdditionalInstanceField(activity, "mContentChangeTimes",1);
				darkHandled = true;
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
					Object obj = XposedHelpers.getAdditionalInstanceField(activity,
							"mStatusBarBackground");
					if (obj != null) {
						color = (Integer) obj;
						Utils.log("get color from mStatusBarBackground:" + color);
						isdark = (Boolean) XposedHelpers
								.getAdditionalInstanceField(activity, "mDarkMode");
						colorHandled = true;
						XposedHelpers.setAdditionalInstanceField(activity,
								"mNeedGetColorFromBackground", false);
					break;
					}
				}
				if (!colorHandled) {
					ProfileHelper helper = (ProfileHelper) XposedHelpers.getAdditionalInstanceField(activity, "mProfileHelper");
					if (helper != null) {
					try {
						backgroundtype = helper.getBackgroundType();
						XposedHelpers.setAdditionalInstanceField(activity,
								"mBackgroundType", backgroundtype);
						switch (backgroundtype) {
						case 0:
							int i = helper.getColor();
							if (i != Constant.UNKNOW_COLOR) {
								color = i;
								XposedHelpers
										.setAdditionalInstanceField(activity,
												"mStatusBarBackground", color);
								XposedHelpers.setAdditionalInstanceField(
										activity, "mHasProfile", true);
								isdark = Utils.getDarkMode(color);
								XposedHelpers.setAdditionalInstanceField(
										activity, "mDarkMode", isdark);
								colorHandled = true;
								int k = helper.getPaddingOffset();
								if (k != 0) {
									Utils.resetPadding(activity, k);
								}
							}
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
							int k = helper.getPaddingOffset();
							if (k != 0) {
								Utils.resetPadding(activity, k);
							}
							break;
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
					}
				}
				if (!colorHandled) {
					darkHandled = true;
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
								} catch (IllegalArgumentException e) {
								}
								container.invalidate();
							}
						}
					}
				}

				if (!colorHandled) {
					XposedHelpers.setAdditionalInstanceField(activity,
							"mNeedGetColorFromBackground", true);
					Utils.log("can't handle color, need to get color from drawcache after widow focus changed");
					return;
				}
				break;
			}

		} 
		

		Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_TYPE, backgroundtype);
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, color);
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_PATH, path);
		intent.putExtra(Constant.IS_DARKMODE, isdark);
		intent.putExtra(Constant.DARKMODE_HANDLE, darkHandled);
		
		Utils.log("backgroundtype:" + backgroundtype + ";" + "statusbar_background:" + color + "; " + 
		"path:" + path + ";" + "dark_mode:" + isdark + "; " + "dark_handled:" + darkHandled);

		activity.sendBroadcast(intent);
	}
	
}
