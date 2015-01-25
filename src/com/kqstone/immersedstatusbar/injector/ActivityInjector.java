package com.kqstone.immersedstatusbar.injector;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.R;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.Utils.WindowType;
import com.kqstone.immersedstatusbar.helper.BitMapColor;
import com.kqstone.immersedstatusbar.helper.BitMapColor.Type;
import com.kqstone.immersedstatusbar.helper.ProfileHelper;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class ActivityInjector {
	private static final String SHAREDPREF_NAME = "isb";
	private static final String[] FastTransApp = { "com.miui.home",
			"com.UCMobile", "com.tencent.mm", "com.sina.weibo" };

	private static final String[][] IgnorApp = { { "com.baidu.netdisk",
			"ui.MainActivity" } };

	public Activity mActivity;
	private String mPkgName = null, mActName = null;
	private ProfileHelper mHelper = null;
	private boolean mHasProfile = false;
	private boolean mIsLauncher = false;

	private Integer mBackgroundType = null;
	private int mColor = 0;
	private String mPath = null;
	private boolean mDarkMode = false;
	private boolean mFastTrans = false;

	private boolean mNeedGetColorFromBackground = false;
	private boolean mRepaddingHandled = false;
	private boolean mHasSetWindowBackground = false;
	private boolean mCreateAct = false;

	private SharedPreferences mPref;
	private IntentFilter mFilter = new IntentFilter();
	private Handler mHandler = new Handler();

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Const.INTENT_GET_USER_COLOR)) {
				boolean isget = intent.getBooleanExtra("IS_GET", false);
				String msg = null;
				Resources res = null;
				try {
					res = mActivity.getPackageManager()
							.getResourcesForApplication(Const.PKG_NAME_SELF);
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (isget) {
					int color = intent.getIntExtra(
							Const.STATUSBAR_BACKGROUND_COLOR,
							Const.UNKNOW_COLOR);
					int offset = intent.getIntExtra(Const.ACTIVITY_OFFSET, 0);
					if (color != Const.UNKNOW_COLOR) {
						if (mColor != color) {
							if (mPref.contains(Const.ACTIVITY_OFFSET + "_"
									+ mActName)) {
								clearUserSet();
								msg = (res != null) ? res
										.getString(R.string.toast_cancel_usr_color_in_set)
										: null;
							} else {
								setUserSet(color, offset);
								msg = (res != null) ? res
										.getString(R.string.toast_prefix_get_usr_color)
										+ Utils.getHexFromColor(mColor)
										: null;
							}
						} else {
							msg = (res != null) ? res
									.getString(R.string.toast_get_usr_color_unnecessary)
									: null;
						}
					} else {
						msg = (res != null) ? res
								.getString(R.string.toast_get_usr_color_fail)
								: null;
					}
				} else {
					if (mPref.contains(Const.STATUSBAR_BACKGROUND_COLOR + "_"
							+ mActName)) {
						clearUserSet();
						msg = (res != null) ? res
								.getString(R.string.toast_cancel_usr_color)
								: null;
					} else {
						msg = (res != null) ? res
								.getString(R.string.toast_cancel_usr_color_unnecessary)
								: null;
					}
				}
				if (msg != null)
					Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();

			}
		}
	};

	public ActivityInjector(Object activity) {
		mActivity = (Activity) activity;
	}

	public void hookAfterOnCreate() {
		mPkgName = mActivity.getPackageName();
		mActName = mActivity.getLocalClassName();
		mCreateAct = true;
		if (mPkgName.equals("com.miui.home")
				&& mActName.equals("launcher.Launcher")) {
			mIsLauncher = true;
			IntentFilter filter = new IntentFilter();
			filter.addAction(Const.INTENT_SET_WALLPAPER);
			mActivity.registerReceiver(new BroadcastReceiver() {

				@Override
				public void onReceive(Context arg0, Intent arg1) {
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							boolean darkmode = Utils
									.darkModeStatusBarMiuiActivity(mActivity);
							if (mDarkMode != darkmode) {
								mDarkMode = darkmode;
								Utils.sendTintStatusBarIntent(mActivity,
										mBackgroundType, mColor, mPath,
										mDarkMode, mFastTrans);
								Utils.log("get set wallpaper broadcast, reset mDarkMode: "
										+ mDarkMode);
							}
						}
					}, 300);
				}
			}, filter);
			return;
		}

		mHelper = new ProfileHelper(mActivity.getPackageName());
		mHelper.initiateProfile(mActivity.getLocalClassName());
		if (mHelper != null && mHelper.hasProfile()) {
			mHasProfile = true;
			mBackgroundType = mHelper.getBackgroundType();
			if (mBackgroundType == 2)
				Utils.setTranslucentStatus(mActivity);
		}

		mPref = mActivity.getSharedPreferences(SHAREDPREF_NAME,
				Context.MODE_PRIVATE);

		mFilter.addAction(Const.INTENT_GET_USER_COLOR);
		mFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
	}

	public void hookAfterPerformResume() {
		Utils.logoutActivityInform(mActivity);

		// Ignor app
		for (String[] ignorApp : IgnorApp) {
			if (ignorApp[0].equals(mPkgName) && ignorApp[1].equals(mActName))
				return;
		}

		WindowType type = Utils.getWindowType(mActivity);
		Utils.log("Resume: Window type: " + type);
		if (type == WindowType.Float)
			return;

		// For Activity that has been resumed before
		Utils.log("mCreateAct: " + mCreateAct);
		if (!mCreateAct) {
			Utils.sendTintStatusBarIntent(mActivity, mBackgroundType, mColor,
					mPath, mDarkMode, mFastTrans);
			if (type == WindowType.Normal)
				mActivity.registerReceiver(mReceiver, mFilter);
			return;
		}

		// mFastTrans
		for (String s : FastTransApp) {
			if (s.equals(mPkgName)) {
				mFastTrans = true;
				break;
			}
		}
		if (!mFastTrans) {
			if (mHasProfile) {
				mFastTrans = mHelper.getFastTrans();
			}
		}

		boolean handled = false;
		switch (type) {
		case Fullscreen:
			mColor = Color.parseColor("#33000000");
			handled = true;
			mDarkMode = false;
			Utils.sendTintStatusBarIntent(mActivity, mBackgroundType, mColor,
					mPath, mDarkMode, mFastTrans);
			break;
		case Translucent:
			if (mIsLauncher) {
				mBackgroundType = 0;
				mColor = 0;
				handled = true;
				mDarkMode = Utils.darkModeStatusBarMiuiActivity(mActivity);
				mNeedGetColorFromBackground = false;
				Utils.sendTintStatusBarIntent(mActivity, mBackgroundType,
						mColor, mPath, mDarkMode, mFastTrans);
			} else if (mDarkMode = Utils
					.darkModeStatusBarMiuiActivity(mActivity)) {
				mBackgroundType = 0;
				mColor = Color.TRANSPARENT;
				handled = true;
				mNeedGetColorFromBackground = false;
				Utils.sendTintStatusBarIntent(mActivity, mBackgroundType,
						mColor, mPath, mDarkMode, mFastTrans);
			} else {
				Utils.log("Translucent activity, need get darkmode after window focus changed");
				mNeedGetColorFromBackground = true;
			}
			break;
		case Normal:
			boolean exinform = Settings.System.getInt(
					mActivity.getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM, 0) == 1 ? true : false;
			if (exinform)
				Utils.logStandXml(mActivity);
			boolean exinformtofile = Settings.System.getInt(
					mActivity.getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM_TOFILE, 0) == 1 ? true : false;
			if (exinformtofile)
				Utils.exportStandXml(mActivity);

			Drawable drawable = null;

			int color = mPref.getInt(Const.STATUSBAR_BACKGROUND_COLOR + "_"
					+ mActName, Const.UNKNOW_COLOR);
			if (color != Const.UNKNOW_COLOR) {
				if (mPref.contains(Const.ACTIVITY_OFFSET + "_" + mActName))
					Utils.resetPadding(mActivity,
							Const.OFFEST_FOR_GRADUAL_ACTIVITY);
				mBackgroundType = 0;
				mColor = color;
				mDarkMode = Utils.getDarkMode(mColor);
				handled = true;
				drawable = new ColorDrawable(mColor);
			}

			if (!handled && mHasProfile) {
				mBackgroundType = mHelper.getBackgroundType();
				int k = mHelper.getPaddingOffset();
				if (k != 0) {
					Utils.resetPadding(mActivity, k);
				}
				switch (mBackgroundType) {
				case 0:
					mColor = mHelper.getColor();
					mDarkMode = Utils.getDarkMode(mColor);
					handled = true;
					drawable = new ColorDrawable(mColor);
					break;
				case 1:
					mColor = Const.UNKNOW_COLOR;
					mPath = mHelper.getBackgroundPath();
					Bitmap tempmap = mHelper.getBitmap();
					mDarkMode = Utils
							.getDarkMode(Utils.getBitmapColor(tempmap).Color);
					handled = true;
					drawable = new BitmapDrawable(tempmap);
					break;
				}
			}

			if (!handled) {
				ActionBar actionBar = mActivity.getActionBar();
				if (actionBar != null && actionBar.isShowing()) {
					FrameLayout container = (FrameLayout) ReflectionHelper
							.getObjectField(actionBar, "mContainerView");
					if (container != null) {
						Drawable backgroundDrawable = (Drawable) ReflectionHelper
								.getObjectField(container, "mBackground");
						if (backgroundDrawable != null) {
							BitMapColor bmColor = Utils.getBitmapColor(backgroundDrawable);
							mColor = bmColor.Color;
							mDarkMode = Utils.getDarkMode(mColor);
							handled = true;
							drawable = new ColorDrawable(mColor);
							if (bmColor.mType != BitMapColor.Type.FLAT) {
								actionBar.setBackgroundDrawable(drawable);
								container.invalidate();
							}
							mBackgroundType = 0;
						}
					}
				}
			}

			if (drawable != null && !mHasSetWindowBackground) {
				Utils.setDecorViewBackground(mActivity, drawable, false);
				mHasSetWindowBackground = true;
			}

			if (!handled) {
				mNeedGetColorFromBackground = true;
				Utils.log("can't handle color, need to get color from drawcache after widow focus changed");
			} else {
				Utils.log("get it, send tintstatusbar intent>>>>>>>>>>>>>>>");
				Utils.sendTintStatusBarIntent(mActivity, mBackgroundType,
						mColor, mPath, mDarkMode, mFastTrans);
			}

			mActivity.registerReceiver(mReceiver, mFilter); // register
															// get_user_color
															// intent receiver;
			break;
		case Float:
			break;
		default:
			break;
		}

		mCreateAct = false;

	}

	public void hookAfterOnWindowFocusChanged(boolean focused) {
		if (mIsLauncher || !focused || !mNeedGetColorFromBackground)
			return;

		Bitmap bitmap;
		WindowType type = Utils.getWindowType(mActivity);
		switch (type) {
		case Normal:
			boolean exinformtofile = Settings.System.getInt(
					mActivity.getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM_TOFILE, 0) == 1 ? true : false;
			if (exinformtofile) {
				View view = mActivity.getWindow().getDecorView();
				view.destroyDrawingCache();
				view.setDrawingCacheEnabled(true);
				bitmap = view.getDrawingCache();
				if (bitmap != null)
					Utils.outputBitmapToFile(bitmap, mActivity);
			}

			mBackgroundType = 0;
			mPath = null;

			Drawable drawable = null;
			bitmap = Utils.getBitMapFromActivityBackground(mActivity, false);
			if (bitmap != null) {
				BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
				if (bitmapColor.mType == Type.FLAT) {
					Utils.log("Flat BitMap found...");
					mColor = bitmapColor.Color;
					mDarkMode = Utils.getDarkMode(mColor);
					drawable = new ColorDrawable(mColor);
				} else if (bitmapColor.mType == Type.GRADUAL) {
					Utils.log("GRADUAL BitMap found, rePadding viewgroup...");
					mColor = bitmapColor.Color;
					mDarkMode = Utils.getDarkMode(mColor);
					if (!mRepaddingHandled) {
						Utils.resetPadding(mActivity,
								Const.OFFEST_FOR_GRADUAL_ACTIVITY);
						mRepaddingHandled = true;
					}
					drawable = new ColorDrawable(mColor);
				} else if (bitmapColor.mType == Type.PICTURE) {
					Utils.log("Flat BitMap found...");
					if (Settings.System.getInt(mActivity.getContentResolver(),
							Const.KEY_PREF_FORCE_TINT, 0) == 1) {
						mColor = bitmapColor.Color;
						mDarkMode = Utils.getDarkMode(mColor);
						drawable = new ColorDrawable(mColor);
					}
				}
			}

			if (drawable != null && !mHasSetWindowBackground) {
				Utils.setDecorViewBackground(mActivity, drawable, true);
				mHasSetWindowBackground = true;
			}

			Utils.sendTintStatusBarIntent(mActivity, mBackgroundType, mColor,
					mPath, mDarkMode, mFastTrans);
			break;

		case Translucent:
			mBackgroundType = 0;
			mColor = Color.TRANSPARENT;
			mPath = null;

			bitmap = Utils.getBitMapFromActivityBackground(mActivity, true);
			if (bitmap != null) {
				BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
				int color = bitmapColor.Color;
				mDarkMode = Utils.getDarkMode(color);
			}
			Utils.sendTintStatusBarIntent(mActivity, mBackgroundType, mColor,
					mPath, mDarkMode, mFastTrans);
			break;

		default:
			break;
		}

		mNeedGetColorFromBackground = false;
	}

	public void hookAfterOnPause() {
		WindowType type = Utils.getWindowType(mActivity);
		if (type == WindowType.Normal)
			mActivity.unregisterReceiver(mReceiver);
	}
	
	public void hookAfterOnWindowAttributesChanged(WindowManager.LayoutParams winParams) {
		boolean darkmode = false;
		int extraFlags = (int) ReflectionHelper.getObjectField(winParams, "extraFlags");
		Class<?> miuiLayoutParams = ReflectionHelper.getClass("android.view.MiuiWindowManager$LayoutParams");
		int darkmodeFlag = (int) ReflectionHelper.getStaticField(miuiLayoutParams, "EXTRA_FLAG_STATUS_BAR_DARK_MODE");
		Utils.log("extraFlags:" + extraFlags + "; darkmodeFlag" + darkmodeFlag);
		if ((extraFlags & darkmodeFlag) == darkmodeFlag) {
			darkmode = true;
		}
		if (mDarkMode != darkmode) {
			mDarkMode = darkmode;
			Intent intent = new Intent(Const.INTENT_CHANGE_STATUSBAR_DARKMODE);
			intent.putExtra(Const.IS_DARKMODE, mDarkMode);
			mActivity.sendBroadcast(intent);
			Utils.log("Change DarkMode by WindowAttributesChanged: " + mDarkMode);
		}
	}

	private void setUserSet(int color, int offset) {
		if (offset != 0)
			Utils.resetPadding(mActivity, Const.OFFEST_FOR_GRADUAL_ACTIVITY);
		mColor = color;
		mBackgroundType = 0;
		mDarkMode = Utils.getDarkMode(mColor);
		Drawable drawable = new ColorDrawable(mColor);
		if (drawable != null) {
			Utils.setDecorViewBackground(mActivity, drawable, true);
			mHasSetWindowBackground = true;
		}
		Editor editor = mPref.edit();
		editor.putInt(Const.STATUSBAR_BACKGROUND_COLOR + "_" + mActName, mColor);
		if (offset != 0)
			editor.putInt(Const.ACTIVITY_OFFSET + "_" + mActName,
					Const.OFFEST_FOR_GRADUAL_ACTIVITY);
		editor.commit();
		Utils.sendTintStatusBarIntent(mActivity, mBackgroundType, mColor,
				mPath, mDarkMode, mFastTrans);
	}

	private void clearUserSet() {
		Editor editor = mPref.edit();
		editor.remove(Const.STATUSBAR_BACKGROUND_COLOR + "_" + mActName);
		if (mPref.contains(Const.ACTIVITY_OFFSET + "_" + mActName)) {
			editor.remove(Const.ACTIVITY_OFFSET + "_" + mActName);
			Utils.resetPadding(mActivity, -Const.OFFEST_FOR_GRADUAL_ACTIVITY);
		}
		editor.commit();
		mCreateAct = true;
		mColor = 0;
		hookAfterPerformResume();
	}
	
	public int getCurrentColor() {
		return mColor;
	}
	
	public boolean getCurrentDarkMode() {
		return mDarkMode;
	}
	
	public void setCurrentColor(int color) {
		mColor = color;
	}
	
	public void setCurrentDarkMode (boolean darkMode) {
		mDarkMode = darkMode;
	}
}
