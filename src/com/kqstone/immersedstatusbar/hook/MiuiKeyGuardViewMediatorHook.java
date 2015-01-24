package com.kqstone.immersedstatusbar.hook;

import com.kqstone.immersedstatusbar.injector.MiuiKeyGuardViewMediatorInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MiuiKeyGuardViewMediatorHook {
	private static final String sClassName = "com.android.keyguard.MiuiKeyguardViewMediator";

	public static void doHook(ClassLoader loader) {
		Class<?> clazz = XposedHelpers.findClass(sClassName, loader);
		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				XposedHelpers.setAdditionalInstanceField(param.thisObject,
						"mMiuiKeyguardViewMediatorHook",
						new MiuiKeyGuardViewMediatorInjector(param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "handleShow",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((MiuiKeyGuardViewMediatorInjector) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mMiuiKeyguardViewMediatorHook"))
								.hookAfterHandleShow();
					}
				});

		XposedHelpers.findAndHookMethod(clazz, "handleHide",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((MiuiKeyGuardViewMediatorInjector) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mMiuiKeyguardViewMediatorHook"))
								.hookAfterHandleHide();
					}
				});
	}

}
