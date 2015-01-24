package com.kqstone.immersedstatusbar.hook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Window;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class WindowHook {
	private Window mWindow;
	private Context mContext;
	private boolean mDarkMode;

	public WindowHook(Object window) {
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
				ActivityHook activityhook = (ActivityHook) ReflectionHelper
						.getAdditionalInstanceField(
								(Activity) mWindow.getCallback(), "mActivityHook");
				ReflectionHelper.setObjectField(activityhook, "mDarkMode",
						mDarkMode);
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
