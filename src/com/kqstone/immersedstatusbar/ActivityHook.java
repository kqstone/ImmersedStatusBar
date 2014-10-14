package com.kqstone.immersedstatusbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityHook implements IXposedHookZygoteInit {

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		// TODO Auto-generated method stub
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
		XposedHelpers.findAndHookMethod(Activity.class, "performResume", new ActivityOnResumeHook());
		XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new OnWindowFocusedHook());
		XposedHelpers.findAndHookMethod(Activity.class, "onContentChanged", new OnContentChangedHook());

	}

}
