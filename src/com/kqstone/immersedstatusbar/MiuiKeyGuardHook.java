package com.kqstone.immersedstatusbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MiuiKeyGuardHook implements IXposedHookLoadPackage {

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.keyguard"))
			return;
		Utils.log("MiuiKeyGuard Hooked...");
		XposedHelpers.findAndHookMethod("com.android.keyguard.MiuiKeyguardViewMediator", lpparam.classLoader, "handleShow", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
				Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
				intent.putExtra(Constant.PKG_NAME, "com.android.keyguard");
				intent.putExtra(Constant.ACT_NAME, "MiuiKeyGuard");
				intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, Color.TRANSPARENT);
				intent.putExtra(Constant.IS_DARKMODE, false);
				intent.putExtra(Constant.DARKMODE_HANDLE, true);
				Context ctx = (Context) XposedHelpers.getObjectField(methodHookParam.thisObject, "mContext");

				ctx.sendBroadcast(intent);
				Utils.log("MiuiKeyGuard show, send intent");
			}
		});
	}

}
