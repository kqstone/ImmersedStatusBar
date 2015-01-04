package com.kqstone.immersedstatusbar.hook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MiuiKeyGuardHook {

	public static void hookAfterHandleShow(Object miuiKeyguardViewMediator) {
		Context ctx = (Context) XposedHelpers.getObjectField(
				miuiKeyguardViewMediator, "mContext");
		Intent intent = new Intent(Const.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Const.PKG_NAME, "com.android.keyguard");
		intent.putExtra(Const.ACT_NAME, "MiuiKeyGuard");
		intent.putExtra(Const.STATUSBAR_BACKGROUND_COLOR, Color.TRANSPARENT);
		intent.putExtra(Const.IS_DARKMODE, false);
		intent.putExtra(Const.FAST_TRANSITION, true);
		ctx.sendBroadcast(intent);
		Utils.log("MiuiKeyGuard show, send intent");
	}

}
