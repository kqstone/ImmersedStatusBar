package com.kqstone.immersedstatusbar.hook;

import android.preference.ListPreference;

import com.kqstone.immersedstatusbar.injector.SettingsInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class DevelopmentSettingsHook {
	private static final String sClassName = "com.android.settings.DevelopmentSettings";

	public static void doHook(ClassLoader loader) {
		XposedHelpers.findAndHookMethod(sClassName, loader, "a", int.class,
				ListPreference.class, Object.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						Object developmentSettings = param.thisObject;
						int which = (Integer) param.args[0];
						Object value = param.args[2];
						SettingsInjector.hookAfterA(developmentSettings, which,
								value);
					}
				});
	}

}
