package com.kqstone.immersedstatusbar.hook;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.StatusBarIconViewInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class StatusBarIconViewHook {
	private static final String sClassName = "com.android.systemui.statusbar.StatusBarIconView";

	public static void doHook(ClassLoader loader) {
		Class<?> clazz = XposedHelpers.findClass(sClassName, loader);
		Class<?> StatusBarIcon = XposedHelpers.findClass(
				"com.android.internal.statusbar.StatusBarIcon", null);

		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				ReflectionHelper.setAdditionalInstanceField(param.thisObject,
						"mStatusBarIconViewHook",
						new StatusBarIconViewInjector(param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "setIcon", StatusBarIcon,
				new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((StatusBarIconViewInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mStatusBarIconViewHook"))
								.hookAfterSetIcon(param.args[0]);
					}
				});
	}

}
