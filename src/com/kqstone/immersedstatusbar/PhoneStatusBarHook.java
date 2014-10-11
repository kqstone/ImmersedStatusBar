package com.kqstone.immersedstatusbar;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class PhoneStatusBarHook implements IXposedHookLoadPackage {
	private Object instancePhoneStatusBar;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;
	
	private BroadcastReceiver mActivityResumeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (!intent.getAction().equals(Constant.INTENT_CHANGE_STATUSBAR_COLOR)) 
				return;
			boolean darkHandled = intent.getBooleanExtra(Constant.DARKMODE_HANDLE, false);
			boolean darkMode;
			if (darkHandled) {
				darkMode = intent.getBooleanExtra(Constant.IS_DARKMODE, false);
				if (darkMode != mPreDarkMode) {
					updateStatusBarContent(darkMode);
					mPreDarkMode = darkMode;
				}
				
			} else {
				int disabled = XposedHelpers.getIntField(instancePhoneStatusBar, "mDisabled");
				Utils.log("mDisabled: " + disabled);
				if ((disabled == 0 || disabled == 128 || disabled == 8388608) && mPreDarkMode) {
					updateStatusBarContent(false);
					mPreDarkMode = false;
				}
			}			
			int color = intent.getIntExtra(Constant.STATUSBAR_BACKGROUND_COLOR, Constant.COLOR_BLACK);
			if (color != mPreColor) {
				updateStatusBarBackground(color); 
				mPreColor = color;
			}
		}
		
	};

	private void updateStatusBarContent(boolean darkmode) {
		Utils.log("darkmode: " + darkmode);
		XposedHelpers.setBooleanField(instancePhoneStatusBar, "mTargetDarkMode", darkmode);
		XposedHelpers.callMethod(XposedHelpers.getObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable"), "run");
	}
	
	private void updateStatusBarBackground(int color) {
		View statusBarView = (View) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
		ObjectAnimator.ofFloat(statusBarView, "transitionAlpha", new float[] { 0.0F, 0.1F, 1.0F })
			.setDuration(Constant.TIME_FOR_STATUSBAR_BACKGROUND_TRANSITION).start();
		statusBarView.setBackgroundColor(color);
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		// TODO Auto-generated method stub
		if (lpparam.packageName.equals("com.android.systemui")) {
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"start", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					instancePhoneStatusBar = param.thisObject;
					
					Context context = (Context) XposedHelpers.getObjectField(instancePhoneStatusBar, "mContext");
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
					intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
					context.registerReceiver(mActivityResumeReceiver, intentFilter);
				}
				
			});
		}
		 
	}

}
