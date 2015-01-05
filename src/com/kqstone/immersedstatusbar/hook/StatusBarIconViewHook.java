package com.kqstone.immersedstatusbar.hook;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.widget.ImageView;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class StatusBarIconViewHook {
	private Context mContext;
	private Object mStatusBarIconView;

	public StatusBarIconViewHook(Object statusBarIconViewHook) {
		mStatusBarIconView = statusBarIconViewHook;
		mContext = (Context) ReflectionHelper.getObjectField(
				statusBarIconViewHook, "mContext");
	}

	public void hookAfterSetIcon(Object statusBarIcon) {
		boolean supportDarkMode = (boolean) ReflectionHelper.getObjectField(
				mStatusBarIconView, "mSupportDarkMode");
		boolean enableDarkMode = (boolean) ReflectionHelper.getObjectField(
				mStatusBarIconView, "mEnableDarkMode");
		if (supportDarkMode && enableDarkMode)
			return;

		boolean tinticons = Settings.System.getInt(
				mContext.getContentResolver(),
				Const.KEY_PREF_TINT_NOTIFICATION, 0) == 1 ? true : false;
		Utils.log("tint notification icons: " + tinticons
				+ ", hook getIcon>>>>>>>>");
		if (tinticons) {
			Drawable drawable = getIcon(mContext, statusBarIcon);
			((ImageView) mStatusBarIconView).setImageDrawable(drawable);
		}
	}

	private static Drawable getIcon(Context context, Object icon) {
		Resources r = null;

		String iconPackage = (String) ReflectionHelper.getObjectField(icon,
				"iconPackage");

		if (iconPackage != null) {
			try {
				r = context.getPackageManager().getResourcesForApplication(
						iconPackage);
			} catch (Exception ex) {
				return null;
			}
		} else {
			r = context.getResources();
		}

		int iconId = (int) ReflectionHelper.getObjectField(icon, "iconId");
		if (iconId == 0) {
			return null;
		}

		try {
			return r.getDrawable(iconId);
		} catch (RuntimeException e) {
		}

		return null;
	}
}
