package com.kqstone.immersedstatusbar.injector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Window;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class WindowInjector {
	private Window mWindow;
	private Context mContext;
	private boolean mDarkMode;

	public WindowInjector(Object window) {
		mWindow = (Window) window;
		mContext = (Context) ReflectionHelper
				.getObjectField(window, "mContext");
	}

	public void hookAfterSetExtraFlags(int flagval, int flag) {
		updateDarkMode(flagval, flag);
	}

	private void updateDarkMode(int flagval, int flag) {
		Class<?> miuiLayoutParams = ReflectionHelper
				.getClass("android.view.MiuiWindowManager$LayoutParams");
		int darkmodeFlag = (int) ReflectionHelper.getStaticField(
				miuiLayoutParams, "EXTRA_FLAG_STATUS_BAR_DARK_MODE");
		if (flag == darkmodeFlag) {
			mDarkMode = flagval != 0 ? true : false;
			try {
				ActivityInjector activityInjector = (ActivityInjector) ReflectionHelper
						.getAdditionalInstanceField(
								(Activity) mWindow.getCallback(), "mActivityHook");
				activityInjector.setCurrentDarkMode(mDarkMode);
				sendBroadCast();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendBroadCast() {
		Intent intent = new Intent(Const.INTENT_CHANGE_STATUSBAR_DARKMODE);
		intent.putExtra(Const.IS_DARKMODE, mDarkMode);
		mContext.sendBroadcast(intent);
	}

}
