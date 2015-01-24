package com.kqstone.immersedstatusbar.hook;

import android.view.MotionEvent;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.PhoneStatusBarInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PhoneStatusBarHook {
	private static final String sClassName = "com.android.systemui.statusbar.phone.PhoneStatusBar";

	public static void doHook(ClassLoader loader) {
		Class<?> clazz = XposedHelpers.findClass(sClassName, loader);
		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				ReflectionHelper.setAdditionalInstanceField(param.thisObject,
						"mPhoneStatusBarInjector", new PhoneStatusBarInjector(
								param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "bindViews",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						((PhoneStatusBarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mPhoneStatusBarInjector"))
								.hookBeforeBindViews();
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((PhoneStatusBarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mPhoneStatusBarInjector"))
								.hookAfterBindViews();
					}
				});

		XposedHelpers.findAndHookMethod(clazz, "unbindViews",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						((PhoneStatusBarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mPhoneStatusBarInjector"))
								.hookBeforeUnBindViews();
					}
				});

		XposedHelpers.findAndHookMethod(clazz, "interceptTouchEvent",
				MotionEvent.class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						((PhoneStatusBarInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mPhoneStatusBarInjector"))
								.hookBeforeInterceptTouchEvent((MotionEvent) param.args[0]);
					}
				});
	}

}
