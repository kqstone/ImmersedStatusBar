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
	private Boolean mDarkModeTranslucent;

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
		switch (type) {
		case Normal:
			boolean exinformtofile = Settings.System.getInt(activity.getContentResolver(), Constant.KEY_PREF_EXPORT_INFORM_TOFILE, 0) ==1 ? true:false;
			if (exinformtofile) {
				View view = activity.getWindow().getDecorView();
				view.destroyDrawingCache();
				view.setDrawingCacheEnabled(true);
				Bitmap bitmap = view.getDrawingCache();	
				if (bitmap != null)
					Utils.outputBitmapToFile(bitmap, activity);
			}
			
			boolean needGetColorFromBackground = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mNeedGetColorFromBackground");
			if (!needGetColorFromBackground)
				return;
			int delay = Constant.DELAY_GET_CACHEDRAWABLE;			
			Handler handler = new Handler();
			handler.postDelayed(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Bitmap bitmap = Utils.getBitMapFromActivityBackground(activity);
					int color = Color.BLACK;
					boolean isdark = false;
					boolean darkHandled = false;
					
					if (bitmap != null) {
						BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
						if (bitmapColor.mType == Type.FLAT) {
							Utils.log("Flat BitMap found...");
							color = bitmapColor.Color;
							XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", color);
							isdark = Utils.getDarkMode(color);
							XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
							darkHandled = true;
						} else if (bitmapColor.mType == Type.GRADUAL) {
							Utils.log("GRADUAL BitMap found, rePadding viewgroup...");
							color = bitmapColor.Color;
							XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", color);
							isdark = Utils.getDarkMode(color);
							XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
							darkHandled = true;
							if (!(Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mRepaddingHandled")) {
								Utils.resetPadding(activity, Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
								XposedHelpers.setAdditionalInstanceField(activity, "mRepaddingHandled", true);
							}
							
						} else if (bitmapColor.mType == Type.PICTURE) {
							Utils.log("Flat BitMap found...");
							if (Settings.System.getInt(
									activity.getContentResolver(),
									Constant.KEY_PREF_FORCE_TINT, 0) == 1) {
								color = bitmapColor.Color;
								XposedHelpers.setAdditionalInstanceField(activity,
										"mStatusBarBackground", color);
								isdark = Utils.getDarkMode(color);
								XposedHelpers.setAdditionalInstanceField(activity,
										"mDarkMode", isdark);
								darkHandled = true;
							}
						}
					}
					
					Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
					intent.putExtra(Constant.PKG_ACT_NAME, activity.getPackageName() + "_" + activity.getLocalClassName());
					intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, color);
					intent.putExtra(Constant.IS_DARKMODE, isdark);
					intent.putExtra(Constant.DARKMODE_HANDLE, darkHandled);

					activity.sendBroadcast(intent);
					XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", false);
				}}, delay);
			break;
		case Translucent:
			if (this.mDarkModeTranslucent == null) {
				Bitmap bitmap = Utils.getBitMapFromActivityBackground(activity);
				if (bitmap != null) {
					BitMapColor bitmapColor= Utils.getBitmapColor(bitmap);
					int color = bitmapColor.Color;
					mDarkModeTranslucent = Utils.getDarkMode(color);
				}
			}
			Intent intent = new Intent(
					Constant.INTENT_CHANGE_STATUSBAR_COLOR);
			intent.putExtra(Constant.PKG_ACT_NAME, 
					activity.getPackageName() + "_" + activity.getLocalClassName());
			intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR,
					Color.TRANSPARENT);
			intent.putExtra(Constant.IS_DARKMODE, mDarkModeTranslucent);
			intent.putExtra(Constant.DARKMODE_HANDLE, true);
			activity.sendBroadcast(intent);
			break;
		default:
			break;
		}

	}
}
