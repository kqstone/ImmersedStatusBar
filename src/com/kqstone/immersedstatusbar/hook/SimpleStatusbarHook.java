package com.kqstone.immersedstatusbar.hook;

import java.util.ArrayList;

import android.widget.LinearLayout;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.SimpleStatusbarInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SimpleStatusBarHook {
	private static final String sClassName = "com.android.systemui.statusbar.phone.SimpleStatusBar";

	public static void doHook(ClassLoader loader) {
		Class<?> clazz = XposedHelpers.findClass(sClassName, loader);
		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				ReflectionHelper.setAdditionalInstanceField(param.thisObject,
						"mSimpleStatusbarHook", new SimpleStatusbarInjector(
								param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "updateNotificationIcons",
				boolean.class, ArrayList.class,
				LinearLayout.LayoutParams.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((SimpleStatusbarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mSimpleStatusbarHook"))
								.hookAfterUpdateNotificationIcons();
					}
				});

		XposedHelpers.findAndHookMethod(clazz, "updateDarkMode",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((SimpleStatusbarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mSimpleStatusbarHook"))
								.hookAfterUpdateDarkMode();
					}
				});
	}

}
