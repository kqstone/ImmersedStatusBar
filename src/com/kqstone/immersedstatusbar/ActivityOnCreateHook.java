package com.kqstone.immersedstatusbar;

import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.pm.ApplicationInfo;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityOnCreateHook extends XC_MethodHook {

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Activity activity = (Activity) param.thisObject;
		boolean issysapp = Utils.isSystemApp(activity);
		XposedHelpers.setAdditionalInstanceField(param.thisObject,
				"mIsSystemApp", issysapp);
		Utils.log(activity.getLocalClassName() + ": is sys app:" + issysapp);
		
		ProfileHelper helper = new ProfileHelper(activity.getPackageName());
		helper.initiateProfile(activity.getLocalClassName());
		XposedHelpers.setAdditionalInstanceField(activity, "mProfileHelper", helper);
		int backgroundtype = helper.getBackgroundType();
		if (backgroundtype == 2)
			Utils.setTranslucentStatus(activity);
	}

}
