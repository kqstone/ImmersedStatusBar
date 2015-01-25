package com.kqstone.immersedstatusbar.hook;

import android.app.Activity;
import android.os.Bundle;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.ActivityInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityHook {
	private static Class<?> sClass = Activity.class;

	public static void doHook() {
		XposedBridge.hookAllConstructors(sClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				ReflectionHelper
						.setAdditionalInstanceField(param.thisObject,
								"mActivityInjector", new ActivityInjector(
										param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(sClass, "onCreate", Bundle.class,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mActivityInjector")).hookAfterOnCreate();
					}
				});

		XposedHelpers.findAndHookMethod(sClass, "performResume",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mActivityInjector"))
								.hookAfterPerformResume();
					}
				});

		XposedHelpers.findAndHookMethod(sClass, "onWindowFocusChanged",
				boolean.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mActivityInjector"))
								.hookAfterOnWindowFocusChanged((Boolean) param.args[0]);
					}
				});

		XposedHelpers.findAndHookMethod(sClass, "onPause", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				((ActivityInjector) ReflectionHelper
						.getAdditionalInstanceField(param.thisObject,
								"mActivityInjector")).hookAfterOnPause();
			}
		});
	}

}
