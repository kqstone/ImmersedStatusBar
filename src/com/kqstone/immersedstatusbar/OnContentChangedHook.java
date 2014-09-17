package com.kqstone.immersedstatusbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class OnContentChangedHook extends XC_MethodHook {
	private boolean mHasChange= false;

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		final Activity activity = (Activity) param.thisObject;	
		boolean isSysApp = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mIsSystemApp");
		if (isSysApp) {
			Utils.log("System app, change color to transparent");
			return;
		} 
		
		if (!mHasChange) {
			mHasChange = true;
			return;
		}
		Utils.log("Content changed, re-tint statusbar");

		XposedHelpers.setAdditionalInstanceField(activity,
				"mStatusBarBackground", null);
		XposedHelpers.setAdditionalInstanceField(activity,
				"mNeedGetColorFromBackground", true);
		dialog(activity);
		// ActivityOnResumeHook.sendChangeStatusBarIntent(activity);
		// OnWindowFocusedHook.sendChangeStatusBarIntent(activity);
	}
	
	private void dialog(Activity activity) {
		AlertDialog.Builder builder = new Builder(activity);

		AlertDialog dialog = builder.create();
		dialog.show();

		dialog.dismiss();
	}
}
