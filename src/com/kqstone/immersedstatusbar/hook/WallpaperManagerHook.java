package com.kqstone.immersedstatusbar.hook;

import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.WallpaperManager;

import com.kqstone.immersedstatusbar.injector.WallpaperManagerInjector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class WallpaperManagerHook {
	private static Class<?> sClass = WallpaperManager.class;

	public static void doHook() {
		XposedHelpers.findAndHookMethod(sClass, "setWallpaper",
				InputStream.class, FileOutputStream.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						WallpaperManagerInjector
								.hookAfterSetWallpaper(param.thisObject);
					}
				});
	}
}
