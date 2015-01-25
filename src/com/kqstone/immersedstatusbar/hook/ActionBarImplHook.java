package com.kqstone.immersedstatusbar.hook;

import android.R.drawable;
import android.graphics.drawable.Drawable;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.ActionBarImplInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActionBarImplHook {
	private static final String sClassName = "com.android.internal.app.ActionBarImpl";

	public static void doHook() {
		Class<?> clazz = ReflectionHelper.getClass(sClassName);
		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				ReflectionHelper.setAdditionalInstanceField(param.thisObject,
						"mActionBarImplInjector", new ActionBarImplInjector(
								param.thisObject));
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "show", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				((ActionBarImplInjector) ReflectionHelper
						.getAdditionalInstanceField(param.thisObject,
								"mActionBarImplInjector")).hookAfterShow();
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "hide", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				((ActionBarImplInjector) ReflectionHelper
						.getAdditionalInstanceField(param.thisObject,
								"mActionBarImplInjector")).hookAfterHide();
			}
		});

		XposedHelpers.findAndHookMethod(clazz, "setBackgroundDrawable",
				Drawable.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActionBarImplInjector) ReflectionHelper
								.getAdditionalInstanceField(param.thisObject,
										"mActionBarImplInjector"))
								.hookAfteSetBackgroundDrawable((Drawable) param.args[0]);
					}
				});
	}

}
