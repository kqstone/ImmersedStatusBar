package com.kqstone.immersedstatusbar.injector;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;

public class WallpaperManagerInjector {

	public static void hookAfterSetWallpaper(Object wallpaperManager) {
		Intent intent = new Intent(Const.INTENT_SET_WALLPAPER);
		Context context = (Context) ReflectionHelper.getObjectField(
				wallpaperManager, "mContext");
		context.sendBroadcast(intent);
		Utils.log("Send change wallpaper broadcast...");
	}
}
