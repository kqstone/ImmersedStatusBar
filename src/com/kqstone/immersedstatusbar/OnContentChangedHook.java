package com.kqstone.immersedstatusbar;

import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class OnContentChangedHook extends XC_MethodHook {
	private int mChangeTimes= 0;

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		mChangeTimes++;
		final Activity activity = (Activity) param.thisObject;	
		boolean isSysApp = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mIsSystemApp");
		if (isSysApp) {
			Utils.log("System app, change color to transparent");
			return;
		} 
		WindowType type = Utils.getWindowType(activity);
		if (type != WindowType.Normal)
			return;
		boolean hasProfile = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mHasProfile");
		if (hasProfile)
			return;
		if (mChangeTimes == 1) {
			return;
		}
		Utils.log("Content changed for " + mChangeTimes + " times, re-tint statusbar");

		XposedHelpers.setAdditionalInstanceField(activity,
				"mStatusBarBackground", null);
		XposedHelpers.setAdditionalInstanceField(activity,
				"mNeedGetColorFromBackground", true);
		dialog(activity);
		// ActivityOnResumeHook.sendChangeStatusBarIntent(activity);
//		OnWindowFocusedHook.sendChangeStatusBarIntent(activity);
	}
	
	private void dialog(Activity activity) {
		AlertDialog.Builder builder = new Builder(activity);
		AlertDialog dialog = builder.create();
		dialog.show();
		dialog.dismiss();
	}
}
