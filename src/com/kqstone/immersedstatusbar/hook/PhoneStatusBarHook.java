package com.kqstone.immersedstatusbar.hook;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.BitMapColor;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class PhoneStatusBarHook {
	private static final int DELAY_FAST_TRANS = 80;

	public Object mPhoneStatusBar;
	private Context mContext;

	private String mPrePkgName = null;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;

	private long mDelayTime = 1L;

	private Drawable mBackgroundBeforeUnbind;
	private Boolean mDarkModeBeforeUndbind;

	private Handler mHandler;
	private Runnable mUpdateDarkModeRunnable;

	private boolean mHasActionDownBefore = false;

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

	public void hookBeforeInterceptTouchEvent(MotionEvent motionEvent) {
		boolean getUsrColor = Settings.System.getInt(
				mContext.getContentResolver(), Const.KEY_PREF_GET_USR_COLOR, 0) == 1 ? true
				: false;
		if (!getUsrColor)
			return;
		View statusbarView = ((View) ReflectionHelper.getObjectField(
				mPhoneStatusBar, "mStatusBarView"));
		int x = (int) motionEvent.getRawX();
		int statusbarWidth = statusbarView.getWidth();
		if (x >= statusbarWidth / 5 && x <= statusbarWidth * 4 / 5)
			return;
		int action = motionEvent.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!mHasActionDownBefore) {
				Intent intent = new Intent(Const.INTENT_GET_USER_COLOR);
				if (x < statusbarWidth / 5) {
					intent.putExtra("IS_GET", false);
				} else if (x > statusbarWidth * 4 / 5) {
					intent.putExtra("IS_GET", true);
					Bitmap bitmap = Utils.getScreenShot(mContext);
					if (bitmap != null) {
						BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
						int color = bitmapColor.Color;
						intent.putExtra(Const.STATUSBAR_BACKGROUND_COLOR, color);
						if (bitmapColor.mType == BitMapColor.Type.GRADUAL) {
							intent.putExtra(Const.ACTIVITY_OFFSET,
									Const.OFFEST_FOR_GRADUAL_ACTIVITY);
						}
					}
				}
				mContext.sendBroadcast(intent);
				StringBuilder builder = new StringBuilder();
				builder.append("send get_usr_color intent; ");
				builder.append("statusbarWidth:" + statusbarWidth);
				builder.append("x:" + x);
				Utils.log(builder.toString());
				mHasActionDownBefore = true;
			}
			break;
		case MotionEvent.ACTION_UP:
			mHasActionDownBefore = false;
			break;
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
		float transAnimScal = (float) ReflectionHelper.callMethod(
				IWindowManager, WindowManager, "getAnimationScale", 1);
		mDelayTime = getDelayTime(transAnimScal);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.INTENT_CHANGE_STATUSBAR_COLOR);
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

	private void restartSystemUI() {
		ReflectionHelper.callMethod(mPhoneStatusBar, "unbindViews");
	}

	private long getDelayTime(float animscale) {
		if (animscale == 0)
			return 0;
		return (long) (animscale * 340) + 50;
	}

}
