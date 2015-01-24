package com.kqstone.immersedstatusbar.injector;

import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;

import com.kqstone.immersedstatusbar.Const;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SettingsInjector {

	public static void hookAfterA(Object developmentSettings, int which,
			Object value) {
		if (which != 1)
			return;
		float scale = (value != null) ? Float.parseFloat((String) value) : 1;
		Context context = (Context) XposedHelpers.callMethod(
				developmentSettings, "getActivity");
		Intent intent = new Intent(Const.INTENT_UPDATE_TRANSANIMASCALE);
		intent.putExtra(Const.TRANS_ANIM_SCALE, scale);
		context.sendBroadcast(intent);
	}

}
