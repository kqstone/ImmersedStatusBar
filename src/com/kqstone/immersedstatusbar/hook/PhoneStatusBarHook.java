package com.kqstone.immersedstatusbar.hook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class PhoneStatusBarHook {
	private static final int DELAY_FAST_TRANS = 80;
	private int[] mIconColors = { Color.parseColor("#80000000"),
			Color.parseColor("#99ffffff") };

	public Object mPhoneStatusBar;
	private Context mContext;

	private String mPrePkgName = null;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;

	private int mAlphaFilter;
	private long mDelayTime = 1L;

	private Drawable mBackgroundBeforeUnbind;
	private Boolean mDarkModeBeforeUndbind;

	private Handler mHandler;
	private Runnable mUpdateDarkModeRunnable;

	private BroadcastReceiver mActivityResumeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Const.INTENT_CHANGE_STATUSBAR_COLOR)) {

				String pkgName = intent.getStringExtra(Const.PKG_NAME);
				String actName = intent.getStringExtra(Const.ACT_NAME);
				if (Const.DEBUG) {
					Utils.log("PKG_NAME:"
							+ pkgName
							+ "; ACT_NAME:"
							+ actName
							+ "; BackgroundType:"
							+ intent.getIntExtra(
									Const.STATUSBAR_BACKGROUND_TYPE, 0)
							+ "; Color:"
							+ intent.getIntExtra(
									Const.STATUSBAR_BACKGROUND_COLOR, 0));
				}

				if (!(pkgName.equals("com.android.keyguard") && actName
						.equals("MiuiKeyGuard"))
						&& (mPrePkgName != null
								&& mPrePkgName.equals("com.android.keyguard") && Utils
									.isKeyguardLocked(mContext))) {
					return;
				}

				boolean fastTrans = intent.getBooleanExtra(
						Const.FAST_TRANSITION, false);
				if (pkgName != null) {
					if (mPrePkgName != null
							&& mPrePkgName.equals("com.android.keyguard")) {
						Utils.log("mPrePkg is KeyGuard, fastTran statusbar");
						fastTrans = true;
					}
					if (mPrePkgName != null && pkgName.equals(mPrePkgName)) {
						fastTrans = true;
					} else {
						mPrePkgName = pkgName;
					}
				}
				Utils.log("fastTransition: " + fastTrans);

				int type = intent.getIntExtra(Const.STATUSBAR_BACKGROUND_TYPE,
						0);
				switch (type) {
				case 0:
					int color = intent.getIntExtra(
							Const.STATUSBAR_BACKGROUND_COLOR, Color.BLACK);
					if (color != mPreColor) {
						updateStatusBarBackground(new ColorDrawable(color),
								fastTrans);
						mPreColor = color;
					}
					break;
				case 1:
					String path = intent
							.getStringExtra(Const.STATUSBAR_BACKGROUND_PATH);
					Bitmap bitmap = BitmapFactory.decodeFile(path);
					updateStatusBarBackground(new BitmapDrawable(bitmap),
							fastTrans);
					mPreColor = Const.UNKNOW_COLOR;
					break;
				}

				boolean darkMode = intent.getBooleanExtra(Const.IS_DARKMODE,
						false);
				Utils.log("Darkmode: " + darkMode + "; PreDarkMode: "
						+ mPreDarkMode);

				if (darkMode != mPreDarkMode) {
					updateStatusBarContent(darkMode, fastTrans);
					mPreDarkMode = darkMode;
				}

			} else if (intent.getAction().equals(
					Const.INTENT_UPDATE_NOTIFICATION_ICONS)) {
				mAlphaFilter = (Settings.System.getInt(
						mContext.getContentResolver(),
						Const.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;
				refreshNotificationIcons();
			} else if (intent.getAction().equals(
					Const.INTENT_UPDATE_TRANSANIMASCALE)) {
				float scale = intent.getFloatExtra(Const.TRANS_ANIM_SCALE, 1F);
				mDelayTime = getDelayTime(scale);
			} else if (intent.getAction().equals(Const.INTENT_RESTART_SYSTEMUI)) {
				restartSystemUI();
			}
		}

	};

	public PhoneStatusBarHook(Object phoneStatusBar) {
		mPhoneStatusBar = phoneStatusBar;
	}

	public void hookBeforeBindViews() {
		prepare();
	}

	public void hookAfterBindViews() {
		if (mBackgroundBeforeUnbind != null && mDarkModeBeforeUndbind != null) {
			updateStatusBarBackground(mBackgroundBeforeUnbind, true);
			updateStatusBarContent(mDarkModeBeforeUndbind, true);
		}
	}

	public void hookBeforeUnBindViews() {
		mBackgroundBeforeUnbind = ((View) ReflectionHelper.getObjectField(
				mPhoneStatusBar, "mStatusBarView")).getBackground();
		mDarkModeBeforeUndbind = mPreDarkMode;
	}

	public void hookAfterUpdateNotificationIcons() {
		updateNotificationIcons();
	}

	public void hookAfterUpdateDarkMode() {
		updateNotificationIcons();
	}

	public void hookAfterSetIcon(Object statusBarIconView, Object statusBarIcon) {
		boolean supportDarkMode = (boolean) ReflectionHelper.getObjectField(
				statusBarIconView, "mSupportDarkMode");
		boolean enableDarkMode = (boolean) ReflectionHelper.getObjectField(
				statusBarIconView, "mEnableDarkMode");
		if (supportDarkMode && enableDarkMode)
			return;

		if (mContext == null) {
			mContext = (Context) ReflectionHelper.getObjectField(
					mPhoneStatusBar, "mContext");
		}
		boolean tinticons = Settings.System.getInt(
				mContext.getContentResolver(),
				Const.KEY_PREF_TINT_NOTIFICATION, 0) == 1 ? true : false;
		Utils.log("tint notification icons: " + tinticons
				+ ", hook getIcon>>>>>>>>");
		if (tinticons) {
			Drawable drawable = getIcon(mContext, statusBarIcon);
			((ImageView) statusBarIconView).setImageDrawable(drawable);
		}
	}

	private void prepare() {
		mContext = (Context) ReflectionHelper.getObjectField(mPhoneStatusBar,
				"mContext");
		mHandler = new Handler();
		mUpdateDarkModeRunnable = new Runnable() {

			@Override
			public void run() {
				updateDarkMode(mContext);
			}
		};

		Class<?> ServiceManager = ReflectionHelper
				.getClass("android.os.ServiceManager");
		Class<?> IWindowManagerStub = ReflectionHelper
				.getClass("android.view.IWindowManager$Stub");
		Class<?> IWindowManager = ReflectionHelper
				.getClass("android.view.IWindowManager");
		Object WindowService = ReflectionHelper.callStaticMethod(
				ServiceManager, "getService", "window");
		Object WindowManager = ReflectionHelper.callStaticMethod(
				IWindowManagerStub, "asInterface", WindowService);
		float transAnimScal = (float) ReflectionHelper
				.callMethod(IWindowManager, WindowManager, "getAnimationScale",
						1);
		mDelayTime = getDelayTime(transAnimScal);

		mAlphaFilter = (Settings.System.getInt(mContext.getContentResolver(),
				Const.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.INTENT_CHANGE_STATUSBAR_COLOR);
		intentFilter.addAction(Const.INTENT_UPDATE_NOTIFICATION_ICONS);
		intentFilter.addAction(Const.INTENT_UPDATE_TRANSANIMASCALE);
		intentFilter.addAction(Const.INTENT_RESTART_SYSTEMUI);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		mContext.registerReceiver(mActivityResumeReceiver, intentFilter);

		ReflectionHelper.setObjectField(mPhoneStatusBar,
				"mUpdateDarkModeRunnable", new Runnable() {

					@Override
					public void run() {
					}
				});
		updateIconsColor();

	}

	private void updateDarkMode(Context context) {
		Object simpleStatusbar = ReflectionHelper.getObjectField(
				mPhoneStatusBar, "mSimpleStatusbar");
		if (simpleStatusbar != null) {
			ReflectionHelper.callMethod(simpleStatusbar, "updateDarkMode");

			boolean fastAnim = Settings.System.getInt(
					context.getContentResolver(),
					Const.KEY_PREF_QUICKANIM_CONTENT, 0) == 1 ? true : false;
			Utils.log("Is fast Animate Statusbar Content: " + fastAnim);
			if (!fastAnim)
				ObjectAnimator
						.ofFloat(simpleStatusbar, "transitionAlpha",
								new float[] { 0.0F, 1.0F }).setDuration(500L)
						.start();
		}
	}

	private void updateStatusBarContent(boolean darkmode, boolean fastTrans) {
		Utils.log("darkmode: " + darkmode);
		ReflectionHelper.setObjectField(mPhoneStatusBar, "mDarkMode", darkmode);
		long delaytime = fastTrans ? DELAY_FAST_TRANS : mDelayTime;
		mHandler.postDelayed(mUpdateDarkModeRunnable, delaytime);
	}

	private void updateStatusBarBackground(final Drawable drawable,
			boolean fastTrans) {
		long delaytime = fastTrans ? DELAY_FAST_TRANS : mDelayTime;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				((View) ReflectionHelper.getObjectField(mPhoneStatusBar,
						"mStatusBarView")).setBackground(drawable);
			}
		};
		mHandler.postDelayed(r, delaytime);

	}

	private void updateNotificationIcons() {
		boolean showNotificationIcons = (boolean) ReflectionHelper
				.getObjectField(mPhoneStatusBar, "mShowNotificationIcons");
		if (!showNotificationIcons)
			return;
		if (mContext == null) {
			mContext = (Context) ReflectionHelper.getObjectField(
					mPhoneStatusBar, "mContext");
		}
		boolean tinticons = Settings.System.getInt(
				mContext.getContentResolver(),
				Const.KEY_PREF_TINT_NOTIFICATION, 0) == 1 ? true : false;
		Utils.log("is tint notification: " + tinticons);
		if (!tinticons)
			return;
		Object simpleStatusbar = ReflectionHelper.getObjectField(
				mPhoneStatusBar, "mSimpleStatusbar");
		ViewGroup notificationIcons = (ViewGroup) ReflectionHelper
				.getObjectField(simpleStatusbar, "mNotificationIcons");
		boolean darkmode = (boolean) ReflectionHelper.getObjectField(
				mPhoneStatusBar, "mDarkMode");
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

	private void refreshNotificationIcons() {
		Utils.log("refresh notification icons >>>>>>>>>>>>>>>>");
		ReflectionHelper.callMethod(mPhoneStatusBar, "updateNotificationIcons");
		ReflectionHelper.callMethod(mPhoneStatusBar, "updateViewsInStatusBar");
	}

	private Drawable getIcon(Context context, Object icon) {
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

	private void restartSystemUI() {
		ReflectionHelper.callMethod(mPhoneStatusBar, "unbindViews");
	}

	private void updateIconsColor() {
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

	private long getDelayTime(float animscale) {
		if (animscale == 0)
			return 0;
		return (long) (animscale * 340) + 50;
	}

}
