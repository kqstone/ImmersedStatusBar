package com.kqstone.immersedstatusbar;

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
		ApplicationInfo info = activity.getApplicationInfo();
		Utils.log(String.valueOf((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
		Utils.log(String.valueOf((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0));
		if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0 || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) !=0) {
			XposedHelpers.setAdditionalInstanceField(param.thisObject, "mIsSystemApp", true);
			Utils.log("Activity from system app: " + activity.getLocalClassName());
		}
	}
	
}
