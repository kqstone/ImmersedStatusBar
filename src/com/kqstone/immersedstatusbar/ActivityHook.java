package com.kqstone.immersedstatusbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityHook implements IXposedHookZygoteInit {
	private SettingHelper mSettingHelper;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		// TODO Auto-generated method stub
		mSettingHelper = new SettingHelper(new XSharedPreferences(Constant.PKG_NAME, SettingHelper.PREF));
		
		XposedBridge.hookAllConstructors(Activity.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable  {
				XposedHelpers.setAdditionalInstanceField(param.thisObject, "mIsSystemApp", false);
				XposedHelpers.setAdditionalInstanceField(param.thisObject, "mNeedGetColorFromBackground", false);
				XposedHelpers.setAdditionalInstanceField(param.thisObject, "mStatusBarBackground", null);
				XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDarkMode", false);
				XposedHelpers.setAdditionalInstanceField(param.thisObject, "mRepaddingHandled", false);
			}
		});
		
		XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new ActivityOnCreateHook());
		XposedHelpers.findAndHookMethod(Activity.class, "performResume", new ActivityOnResumeHook(mSettingHelper));
		XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new OnWindowFocusedHook(mSettingHelper));
		XposedHelpers.findAndHookMethod(Activity.class, "onContentChanged", new OnContentChangedHook());

	}

}
