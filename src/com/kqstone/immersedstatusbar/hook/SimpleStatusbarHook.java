package com.kqstone.immersedstatusbar.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class SimpleStatusbarHook {
	private int[] mIconColors = { Color.parseColor("#80000000"),
			Color.parseColor("#99ffffff") };

	private Object mSimpleStatusbar;
	private Context mContext;
	private int mAlphaFilter;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					Const.INTENT_UPDATE_NOTIFICATION_ICONS)) {
				mAlphaFilter = (Settings.System.getInt(
						mContext.getContentResolver(),
						Const.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;
				refreshNotificationIcons();
			}
		}

	};

	public SimpleStatusbarHook(Object simpleStatusbar) {
		mSimpleStatusbar = simpleStatusbar;
		mContext = (Context) ReflectionHelper.getObjectField(simpleStatusbar,
				"mContext");
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.INTENT_UPDATE_NOTIFICATION_ICONS);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		mContext.registerReceiver(mReceiver, intentFilter);

		mAlphaFilter = (Settings.System.getInt(mContext.getContentResolver(),
				Const.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;

		refreshIconsColor();
	}

	public void hookAfterUpdateNotificationIcons() {
		updateNotificationIcons();
	}

	public void hookAfterUpdateDarkMode() {
		updateNotificationIcons();
	}

	private void updateNotificationIcons() {
		boolean showNotificationIcons = (boolean) ReflectionHelper
				.getObjectField(ReflectionHelper.getObjectField(
						mSimpleStatusbar, "mService"), "mShowNotificationIcons");
		if (!showNotificationIcons)
			return;
		boolean tinticons = Settings.System.getInt(
				mContext.getContentResolver(),
				Const.KEY_PREF_TINT_NOTIFICATION, 0) == 1 ? true : false;
		Utils.log("is tint notification: " + tinticons);
		if (!tinticons)
			return;

		ViewGroup notificationIcons = (ViewGroup) ReflectionHelper
				.getObjectField(mSimpleStatusbar, "mNotificationIcons");
		boolean darkmode = (boolean) ReflectionHelper.getObjectField(
				ReflectionHelper.getObjectField(mSimpleStatusbar, "mService"),
				"mDarkMode");
		int color = Utils.setAlphaForARGB(mIconColors[darkmode ? 0 : 1],
				mAlphaFilter);
		int alpha = Color.alpha(mIconColors[darkmode ? 0 : 1]) + 255
				- mAlphaFilter;
		alpha = alpha < 255 ? alpha : 255;
		Utils.log("FilterAlpha: " + mAlphaFilter + "; ViewAlpha: " + alpha);
		int k = notificationIcons.getChildCount();
		for (int i = 0; i < k; i++) {
			View icon = notificationIcons.getChildAt(i);
			if (icon != null && (icon instanceof ImageView)) {
				ImageView iconimage = (ImageView) icon;
				iconimage.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				iconimage.setAlpha(alpha);
			}
		}
	}

	private void refreshIconsColor() {
		Resources res = mContext.getResources();
		String[] resNames = { "status_bar_textColor_darkmode",
				"status_bar_textColor" };
		int k;
		for (int i = 0; i < 2; i++) {
			try {
				k = res.getIdentifier(resNames[i], "color",
						"com.android.systemui");
				if (k > 0)
					mIconColors[i] = res.getColor(k);
				Utils.log("mIconColor: " + mIconColors[i]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void refreshNotificationIcons() {
		Utils.log("refresh notification icons >>>>>>>>>>>>>>>>");
		ReflectionHelper.callMethod(
				ReflectionHelper.getObjectField(mSimpleStatusbar, "mService"),
				"updateNotificationIcons");
		ReflectionHelper.callMethod(
				ReflectionHelper.getObjectField(mSimpleStatusbar, "mService"),
				"updateViewsInStatusBar");
	}

}
