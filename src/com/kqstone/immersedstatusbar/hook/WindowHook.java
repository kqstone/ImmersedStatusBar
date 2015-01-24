package com.kqstone.immersedstatusbar.hook;

import android.view.Window;

import com.kqstone.immersedstatusbar.helper.ReflectionHelper;
import com.kqstone.immersedstatusbar.injector.WindowInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class WindowHook {
	private static Class<?> sClass = Window.class;
	
	public static void doHook() {
		XposedBridge.hookAllConstructors(sClass,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) {
						ReflectionHelper.setAdditionalInstanceField(
								param.thisObject, "mWindowHook",
								new WindowInjector(param.thisObject));
					}
				});
		
		XposedHelpers.findAndHookMethod(sClass, "setExtraFlags",
				int.class, int.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((WindowInjector) ReflectionHelper.getAdditionalInstanceField(
								param.thisObject, "mWindowHook"))
								.hookAfterSetExtraFlags((int) param.args[0],
										(int) param.args[1]);
					}
				});

	}

}
