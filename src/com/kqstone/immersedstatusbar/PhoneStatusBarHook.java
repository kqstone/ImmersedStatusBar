package com.kqstone.immersedstatusbar;

import java.util.ArrayList;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class PhoneStatusBarHook implements IXposedHookLoadPackage {
	private Object instancePhoneStatusBar;
	private Context mContext;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;
	
	private BroadcastReceiver mActivityResumeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					Constant.INTENT_CHANGE_STATUSBAR_COLOR)) {
				int type = intent.getIntExtra(Constant.STATUSBAR_BACKGROUND_TYPE, 0);
				switch (type) {
				case 0:
					boolean darkHandled = intent.getBooleanExtra(
							Constant.DARKMODE_HANDLE, false);
					boolean darkMode;
					if (darkHandled) {
						darkMode = intent.getBooleanExtra(Constant.IS_DARKMODE,
								false);
						if (darkMode != mPreDarkMode) {
							updateStatusBarContent(darkMode);
							mPreDarkMode = darkMode;
						}

					} else {
						int disabled = XposedHelpers.getIntField(
								instancePhoneStatusBar, "mDisabled");
						Utils.log("mDisabled: " + disabled);
						if ((disabled == 0 || disabled == 128 || disabled == 8388608)
								&& mPreDarkMode) {
							updateStatusBarContent(false);
							mPreDarkMode = false;
						}
					}
					int color = intent.getIntExtra(
							Constant.STATUSBAR_BACKGROUND_COLOR, Color.BLACK);
					if (color != mPreColor) {
						updateStatusBarBackground(new ColorDrawable(color));
						mPreColor = color;
					}
					break;
				case 1:
					darkMode = intent.getBooleanExtra(Constant.IS_DARKMODE,
							false);
					if (darkMode != mPreDarkMode) {
						updateStatusBarContent(darkMode);
						mPreDarkMode = darkMode;
					}
					String path = intent.getStringExtra(Constant.STATUSBAR_BACKGROUND_PATH);
					Bitmap bitmap = BitmapFactory.decodeFile(path);
					updateStatusBarBackground(new BitmapDrawable(bitmap));
					mPreColor = Constant.UNKNOW_COLOR;
				}
				
			} else if (intent.getAction().equals(
					Constant.INTENT_UPDATE_NOTIFICATION_ICONS)) {
				refreshNotificationIcons();
			}
		}
		
	};
	
	private void updateDarkMode(Context context) {
		boolean darkMode = XposedHelpers.getBooleanField(instancePhoneStatusBar, "mDarkMode");
		boolean targetDarkMode = XposedHelpers.getBooleanField(instancePhoneStatusBar, "mTargetDarkMode");
		if (darkMode == targetDarkMode)
			return;
		XposedHelpers.setBooleanField(instancePhoneStatusBar, "mDarkMode", targetDarkMode);
		Object simpleStatusbar = XposedHelpers.getObjectField(instancePhoneStatusBar, "mSimpleStatusbar");
		if (simpleStatusbar != null) {			
			boolean fastAnim = Settings.System.getInt(context.getContentResolver(), Constant.KEY_PREF_QUICKANIM_CONTENT, 0) ==1 ? true:false;
			Utils.log("Is fast Animate Statusbar Content: " + fastAnim);
			long duration = 500L;
			if (fastAnim)
				duration = 100L;
			XposedHelpers.callMethod(simpleStatusbar, "updateDarkMode");
			ObjectAnimator.ofFloat(simpleStatusbar, "transitionAlpha", new float[] { 0.0F, 1.0F }).setDuration(duration).start();
		}
	}

	private void updateStatusBarContent(boolean darkmode) {
		Utils.log("darkmode: " + darkmode);
		XposedHelpers.setBooleanField(instancePhoneStatusBar, "mTargetDarkMode", darkmode);
		XposedHelpers.callMethod(XposedHelpers.getObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable"), "run");
	}
	
	private void updateStatusBarBackground(Drawable drawable) {
		View statusBarView = (View) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
		ObjectAnimator.ofFloat(statusBarView, "transitionAlpha", new float[] { 0.0F, 0.1F, 1.0F })
			.setDuration(Constant.TIME_FOR_STATUSBAR_BACKGROUND_TRANSITION).start();
		statusBarView.setBackground(drawable);
	}
	
	private void updateNotificationIcons() {
		boolean showNotificationIcons = XposedHelpers.getBooleanField(instancePhoneStatusBar, "mShowNotificationIcons");
		if (!showNotificationIcons)
			return;
		if (mContext == null) {
			mContext = (Context) XposedHelpers.getObjectField(instancePhoneStatusBar, "mContext");
		}
		boolean tinticons = Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_TINT_NOTIFICATION, 0) ==1 ? true:false;
		Utils.log("is tint notification: " + tinticons);
		if (!tinticons)
			return;
		Object simpleStatusbar = XposedHelpers.getObjectField(instancePhoneStatusBar, "mSimpleStatusbar");
		ViewGroup notificationIcons = (ViewGroup) XposedHelpers.getObjectField(simpleStatusbar, "mNotificationIcons");
		boolean darkmode = XposedHelpers.getBooleanField(instancePhoneStatusBar, "mTargetDarkMode");
		int color = darkmode ? Color.parseColor("#505050") : Color.parseColor("#E0E0E0");
		int k = notificationIcons.getChildCount();
		for (int i=0; i<k; i++) {
			View icon = notificationIcons.getChildAt(i);
			if (icon != null && (icon instanceof ImageView)) {
				ImageView iconimage = (ImageView)icon;
				iconimage.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				iconimage.setAlpha(0.8F);
			}
		}
	}
	
	private void refreshNotificationIcons() {
		Utils.log("refresh notification icons >>>>>>>>>>>>>>>>");
		XposedHelpers.callMethod(instancePhoneStatusBar, "updateNotificationIcons");
		XposedHelpers.callMethod(instancePhoneStatusBar, "updateViewsInStatusBar");	
	}
	
    private Drawable getIcon(Context context, Object icon) {
        Resources r = null;
        
        String iconPackage = (String) XposedHelpers.getObjectField(icon, "iconPackage");

        if (iconPackage != null) {
            try {
                int userId = (Integer) XposedHelpers.callMethod(((UserHandle)XposedHelpers.getObjectField(icon, "user")), "getIdentifier");
                if (userId == XposedHelpers.getStaticIntField(UserHandle.class, "USER_ALL")) {
                    userId = XposedHelpers.getStaticIntField(UserHandle.class, "USER_OWNER");
                }
                r = (Resources) XposedHelpers.callMethod(context.getPackageManager(), "getResourcesForApplicationAsUser", iconPackage, userId);
            } catch (Exception ex) {
                return null;
            }
        } else {
            r = context.getResources();
        }
        
        int iconId = XposedHelpers.getIntField(icon, "iconId");
        if (iconId == 0) {
            return null;
        }

        try {
            return r.getDrawable(iconId);
        } catch (RuntimeException e) {
        }

        return null;
    }
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		// TODO Auto-generated method stub
		if (lpparam.packageName.equals("com.android.systemui")) {
			XposedBridge.hookAllConstructors(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.PhoneStatusBar",
					lpparam.classLoader), new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					instancePhoneStatusBar = param.thisObject;
				}
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"start", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					if (mContext == null) {
						mContext = (Context) XposedHelpers.getObjectField(instancePhoneStatusBar, "mContext");
					}
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
					intentFilter.addAction(Constant.INTENT_UPDATE_NOTIFICATION_ICONS);
					intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
					mContext.registerReceiver(mActivityResumeReceiver, intentFilter);
					
					Runnable darkModeRunnable = new Runnable() {

						@Override
						public void run() {
							updateDarkMode(mContext);
						}
					};
					XposedHelpers.setObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable", darkModeRunnable);	
				}
				
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.SimpleStatusBar", lpparam.classLoader), 
					"updateNotificationIcons", boolean.class, ArrayList.class, LinearLayout.LayoutParams.class, new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					updateNotificationIcons();
				}
			});	
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.SimpleStatusBar", lpparam.classLoader), 
					"updateDarkMode", new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					updateNotificationIcons();
				}
			});	
			
			Class<?> StatusBarIcon = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIcon", null);
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader), 
					"setIcon", StatusBarIcon, new XC_MethodHook(){

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					boolean supportDarkMode = XposedHelpers.getBooleanField(param.thisObject, "mSupportDarkMode");
					boolean enableDarkMode = XposedHelpers.getBooleanField(param.thisObject, "mEnableDarkMode");
					if (supportDarkMode && enableDarkMode)
						return;
					
					if (mContext == null) {
						mContext = (Context) XposedHelpers.getObjectField(instancePhoneStatusBar, "mContext");
					}
					boolean tinticons = Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_TINT_NOTIFICATION, 0) ==1 ? true:false;
					Utils.log("tint notification icons: " + tinticons + ", hook getIcon>>>>>>>>");
					if (tinticons) {
						Drawable drawable = getIcon(mContext, param.args[0]);
						((ImageView)param.thisObject).setImageDrawable(drawable);
					} 
				}
			});	
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"disable", int.class, new XC_MethodHook(){

				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					if (instancePhoneStatusBar == null)
						return;
					int disable = XposedHelpers.getIntField(instancePhoneStatusBar, "mDisabled");
					int targetDisable = (Integer) param.args[0];
					Utils.log("disable: " + disable + "/" + targetDisable);
					int j = targetDisable ^ disable;
					if ((j & 0x40) != 0) {
						XposedHelpers.setObjectField(instancePhoneStatusBar, "mTargetDarkMode", true);
						if ((0x40 & disable) != 0) {
							XposedHelpers.setObjectField(instancePhoneStatusBar, "mTargetDarkMode", false);
						}
						long time = 300L;
						boolean fastAnim = Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_QUICKANIM_CONTENT, 0) ==1 ? true:false;
						Utils.log("Is fast Animate Statusbar Content: " + fastAnim);
						if (fastAnim) {
							time = 50L;
						}
						Handler handler = (Handler) XposedHelpers.getObjectField(instancePhoneStatusBar, "mHandler");
						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								updateDarkMode(mContext);
							}}, time);
					}
				}
			});	
			
		}
		 
	}

}
