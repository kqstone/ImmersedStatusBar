package com.kqstone.immersedstatusbar;

import com.kqstone.immersedstatusbar.hook.ActionBarImplHook;
import com.kqstone.immersedstatusbar.hook.ActivityHook;
import com.kqstone.immersedstatusbar.hook.DevelopmentSettingsHook;
import com.kqstone.immersedstatusbar.hook.MiuiKeyGuardViewMediatorHook;
import com.kqstone.immersedstatusbar.hook.PhoneStatusBarHook;
import com.kqstone.immersedstatusbar.hook.SimpleStatusBarHook;
import com.kqstone.immersedstatusbar.hook.StatusBarIconViewHook;
import com.kqstone.immersedstatusbar.hook.WallpaperManagerHook;
import com.kqstone.immersedstatusbar.hook.WindowHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ImmersedStatusBar implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		ActivityHook.doHook();
		WallpaperManagerHook.doHook();
		WindowHook.doHook();
		ActionBarImplHook.doHook();
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		ClassLoader loader = lpparam.classLoader;
		if (lpparam.packageName.equals("com.android.systemui")) {
			PhoneStatusBarHook.doHook(loader);
			SimpleStatusBarHook.doHook(loader);
			StatusBarIconViewHook.doHook(loader);
		}

		if (lpparam.packageName.equals("com.android.keyguard")) {
			MiuiKeyGuardViewMediatorHook.doHook(loader);
		}

		if (lpparam.packageName.equals("com.android.settings")) {
			DevelopmentSettingsHook.doHook(loader);
		}

	}

}
