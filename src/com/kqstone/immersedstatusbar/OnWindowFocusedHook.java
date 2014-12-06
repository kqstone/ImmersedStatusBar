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
		WindowType type = Utils.getWindowType(activity);
		
		int color = Color.BLACK;
		boolean isdark = false;
		final boolean fastTrans = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mFastTrans");
		Bitmap bitmap;
		
		boolean needGetColorFromBackground = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mNeedGetColorFromBackground");
		if (!needGetColorFromBackground)
			return;
		
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

			break;
		case Translucent:
			bitmap = Utils.getBitMapFromActivityBackground(activity, true);
			if (bitmap != null) {
				BitMapColor bitmapColor= Utils.getBitmapColor(bitmap);
				color = bitmapColor.Color;
				isdark = Utils.getDarkMode(color);
				XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
				XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", Color.TRANSPARENT);
				}
			Utils.log("darkmode: " + isdark);

			break;
		default:
			return;
		}
		
		Intent intent = new Intent(
				Constant.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Constant.PKG_NAME, activity.getPackageName());
		intent.putExtra(Constant.ACT_NAME, activity.getLocalClassName());
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR,
				Color.TRANSPARENT);
		intent.putExtra(Constant.IS_DARKMODE, isdark);
		intent.putExtra(Constant.FAST_TRANSITION, fastTrans);
		activity.sendBroadcast(intent);
		XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", false);

	}
}
