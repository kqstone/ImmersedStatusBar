package com.kqstone.immersedstatusbar;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;

import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;
import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityOnResumeHook extends XC_MethodHook {
	private SettingHelper mSettingHelper; 
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Activity activity = (Activity) param.thisObject;
		Utils.logoutActivityInform(activity);
		
		sendChangeStatusBarIntent(activity);
	}
	
	public void sendChangeStatusBarIntent(Activity activity) {
		int color = Color.BLACK;
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
				darkHandled = true;
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
				}
				if (!colorHandled) {
					if (mSettingHelper == null) {
						mSettingHelper = new SettingHelper(activity.getPackageCodePath());
					}
					int i = mSettingHelper.getColor(activity.getLocalClassName());
					if (i != Constant.UNKNOW_COLOR) {
						color = i;
						XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground",color);
						isdark = Utils.getDarkMode(color);
						XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
						colorHandled = true;
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
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, color);
		intent.putExtra(Constant.IS_DARKMODE, isdark);
		intent.putExtra(Constant.DARKMODE_HANDLE, darkHandled);
		
		Utils.log("statusbar_background:" + color + "; " + 
		"dark_mode:" + isdark + "; " + "dark_handled:" + darkHandled);

		activity.sendBroadcast(intent);
	}
	
}
