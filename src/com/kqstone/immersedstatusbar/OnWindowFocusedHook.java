package com.kqstone.immersedstatusbar;

import com.kqstone.immersedstatusbar.BitMapColor.Type;
import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class OnWindowFocusedHook extends XC_MethodHook {

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Utils.log("windows focus changed to " + param.args[0]);
		if(!(Boolean)param.args[0])
			return;	
		
		final Activity activity = (Activity) param.thisObject;
		sendChangeStatusBarIntent(activity);
	}
	
	public void sendChangeStatusBarIntent(final Activity activity) {
		boolean needGetColorFromBackground = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mNeedGetColorFromBackground");
		if (!needGetColorFromBackground)
			return;
		
		Bitmap bitmap;
		int color = Color.BLACK;
		boolean isdark = false;
		boolean fastTrans = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mFastTrans");
		
		WindowType type = Utils.getWindowType(activity);
		switch (type) {
		case Normal:
			boolean exinformtofile = Settings.System.getInt(activity.getContentResolver(), Constant.KEY_PREF_EXPORT_INFORM_TOFILE, 0) ==1 ? true:false;
			if (exinformtofile) {
				View view = activity.getWindow().getDecorView();
				view.destroyDrawingCache();
				view.setDrawingCacheEnabled(true);
				bitmap = view.getDrawingCache();	
				if (bitmap != null)
					Utils.outputBitmapToFile(bitmap, activity);
			}

			bitmap = Utils.getBitMapFromActivityBackground(activity, false);
			if (bitmap != null) {
				BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
				if (bitmapColor.mType == Type.FLAT) {
					Utils.log("Flat BitMap found...");
					color = bitmapColor.Color;
					XposedHelpers.setAdditionalInstanceField(activity,
							"mStatusBarBackground", color);
					isdark = Utils.getDarkMode(color);
					XposedHelpers.setAdditionalInstanceField(activity,
							"mDarkMode", isdark);
				} else if (bitmapColor.mType == Type.GRADUAL) {
					Utils.log("GRADUAL BitMap found, rePadding viewgroup...");
					color = bitmapColor.Color;
					XposedHelpers.setAdditionalInstanceField(activity,
							"mStatusBarBackground", color);
					isdark = Utils.getDarkMode(color);
					XposedHelpers.setAdditionalInstanceField(activity,
							"mDarkMode", isdark);
					if (!(Boolean) XposedHelpers.getAdditionalInstanceField(
							activity, "mRepaddingHandled")) {
						Utils.resetPadding(activity,
								Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
						XposedHelpers.setAdditionalInstanceField(activity,
								"mRepaddingHandled", true);
					}

				} else if (bitmapColor.mType == Type.PICTURE) {
					Utils.log("Flat BitMap found...");
					if (Settings.System.getInt(activity.getContentResolver(),
							Constant.KEY_PREF_FORCE_TINT, 0) == 1) {
						color = bitmapColor.Color;
						XposedHelpers.setAdditionalInstanceField(activity,
								"mStatusBarBackground", color);
						isdark = Utils.getDarkMode(color);
						XposedHelpers.setAdditionalInstanceField(activity,
								"mDarkMode", isdark);
					}
				}
			}

			XposedHelpers.setAdditionalInstanceField(activity,
					"mNeedGetColorFromBackground", false);
			Utils.sendTintStatusBarIntent(activity, 0, color, null, isdark,
					fastTrans);
			break;
		case Translucent:
			bitmap = Utils.getBitMapFromActivityBackground(activity, true);
			if (bitmap != null) {
				BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
				color = bitmapColor.Color;
				isdark = Utils.getDarkMode(color);
				XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode",
						isdark);
				XposedHelpers.setAdditionalInstanceField(activity,
						"mStatusBarBackground", Color.TRANSPARENT);
			}
			Utils.sendTintStatusBarIntent(activity, 0, Color.TRANSPARENT, null,
					isdark, fastTrans);
			break;
		default:
			break;
		}

	}
}
