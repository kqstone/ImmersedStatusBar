package com.kqstone.immersedstatusbar;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;
import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityOnResumeHook extends XC_MethodHook {
	
	/* Floating Window Intent ID */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Activity activity = (Activity) param.thisObject;
		Utils.logoutActivityInform(activity);
		
		sendChangeStatusBarIntent(activity);
	}
	
	public static void sendChangeStatusBarIntent(Activity activity) {
		Intent activityIntent = activity.getIntent();
		if (activityIntent != null
				&& (activityIntent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW) {
			Utils.log("is Floating window, ignore");
			return;
		}

		// From Xposed SwipeBack by PeterCxy
		// https://github.com/LOSP/SwipeBack/blob/master/src/us/shandian/mod/swipeback/hook/ModSwipeBack.java
		int isFloating = getStaticIntField(findClass("com.android.internal.R.styleable", null), "Window_windowIsFloating");
		if (activity.getWindow().getWindowStyle().getBoolean(isFloating, false)) {
			Utils.log("is Floating window, ignore");
			return;
		}

		int color = Constant.COLOR_BLACK;
		boolean colorHandled = false;
		boolean isdark = false;
		boolean darkHandled = false;
		
		boolean isSysApp = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mIsSystemApp");
		if (isSysApp) {
			Utils.log("System app, change color to transparent");
			color = Constant.COLOR_TRANSPARENT;
			colorHandled = true;
		} 
		
		if (!colorHandled) {
			darkHandled = true;
			Object obj = XposedHelpers.getAdditionalInstanceField(activity, "mStatusBarBackground");
			if(obj !=null) {
				color = (Integer)obj;
				Utils.log("get color from mStatusBarBackground:" + color);
				isdark = Utils.getDarkMode(color);
				colorHandled = true;

				XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", false);
			}
		}
				
		if (!colorHandled) {
			darkHandled = true;
			ActionBar actionBar = activity.getActionBar();
			if (actionBar != null) {
				FrameLayout container = (FrameLayout) XposedHelpers.getObjectField(actionBar, "mContainerView");
				if (container != null) {
					Drawable backgroundDrawable = (Drawable) XposedHelpers.getObjectField(container, "mBackground");
					if (backgroundDrawable != null) {
						try {
							color = Utils.getMainColorFromActionBarDrawable(backgroundDrawable);
//							actionBar.setBackgroundDrawable(new ColorDrawable(color));
							XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", color);
							isdark = Utils.getDarkMode(color);
							colorHandled = true;
						} catch (IllegalArgumentException e) {
						}
						container.invalidate();
					}
				}
			}		
		}
		
		if (!colorHandled) {
			XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", true);
			Utils.log("can't handle color, need to get color from drawcache after widow focus changed");
			return;
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
