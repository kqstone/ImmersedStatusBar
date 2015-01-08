package com.kqstone.immersedstatusbar.helper;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;

public class WallpaperManagerHook {

	public static void hookAfterSetWallpaper(Object wallpaperManager) {
		Intent intent = new Intent(Const.INTENT_SET_WALLPAPER);
		Context context = (Context) ReflectionHelper.getObjectField(
				wallpaperManager, "mContext");
		context.sendBroadcast(intent);
		Utils.log("Send change wallpaper broadcast...");
	}
}
