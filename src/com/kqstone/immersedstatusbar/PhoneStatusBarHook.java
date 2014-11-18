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
import android.content.res.XModuleResources;
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

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class PhoneStatusBarHook implements IXposedHookLoadPackage {
	private Object instancePhoneStatusBar;
	private Context mContext;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;
	private int[] mIconColors = {Color.parseColor("#80000000"),Color.parseColor("#99ffffff")};
	private long mDelayTime = 1L;
	
	private Handler handler;
	
	private BroadcastReceiver mActivityResumeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					Constant.INTENT_CHANGE_STATUSBAR_COLOR)) {
				String pkgActName = intent.getStringExtra(Constant.PKG_ACT_NAME);
				int pkgActType = (pkgActName != null && pkgActName.equals("com.miui.home_launcher.Launcher")) ? 1:0;
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
							updateStatusBarContent(darkMode, pkgActType);
							mPreDarkMode = darkMode;
						}

					}
					int color = intent.getIntExtra(
							Constant.STATUSBAR_BACKGROUND_COLOR, Color.BLACK);
					if (color != mPreColor) {
						updateStatusBarBackground(new ColorDrawable(color), pkgActType);
						mPreColor = color;
					}
					break;
				case 1:
					darkMode = intent.getBooleanExtra(Constant.IS_DARKMODE,
							false);
					if (darkMode != mPreDarkMode) {
						updateStatusBarContent(darkMode, pkgActType);
						mPreDarkMode = darkMode;
					}
					String path = intent.getStringExtra(Constant.STATUSBAR_BACKGROUND_PATH);
					Bitmap bitmap = BitmapFactory.decodeFile(path);
					updateStatusBarBackground(new BitmapDrawable(bitmap), pkgActType);
					mPreColor = Constant.UNKNOW_COLOR;
				}
				
			} else if (intent.getAction().equals(
					Constant.INTENT_UPDATE_NOTIFICATION_ICONS)) {
				refreshNotificationIcons();
			} else if (intent.getAction().equals(
					Constant.INTENT_UPDATE_TRANSANIMASCALE)) {
				float scale = intent.getFloatExtra(Constant.TRANS_ANIM_SCALE, 1F);
				mDelayTime = getDelayTime(scale);
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

	private void updateStatusBarContent(boolean darkmode, int pkgActType) {
		Utils.log("darkmode: " + darkmode);
		XposedHelpers.setBooleanField(instancePhoneStatusBar, "mTargetDarkMode", darkmode);
		Runnable runnable = (Runnable) XposedHelpers.getObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable");
		long delaytime = pkgActType == 1 ? 50 : mDelayTime;
		handler.postDelayed(runnable, delaytime);
	}
	
	private void updateStatusBarBackground(final Drawable drawable, int pkgActType) {
		long delaytime = pkgActType == 1 ? 50 : mDelayTime;
		final View statusBarView = (View) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
		Runnable r = new Runnable() {

			@Override
			public void run() {
				statusBarView.setBackground(drawable);
				ObjectAnimator.ofFloat(statusBarView, "transitionAlpha", new float[] { 0.0F, 0.5F, 1.0F })
				.setDuration(Constant.TIME_FOR_STATUSBAR_BACKGROUND_TRANSITION).start();
			}
		};
		handler.postDelayed(r, delaytime);
		
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
		int color = Utils.getRGBFromARGB(mIconColors[darkmode ? 0 : 1]);
		int alpha = Color.alpha(mIconColors[darkmode ? 0 : 1]);
		int k = notificationIcons.getChildCount();
		for (int i=0; i<k; i++) {
			View icon = notificationIcons.getChildAt(i);
			if (icon != null && (icon instanceof ImageView)) {
				ImageView iconimage = (ImageView)icon;
				iconimage.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				iconimage.setAlpha(alpha);
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
					handler = new Handler();
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
					intentFilter.addAction(Constant.INTENT_UPDATE_TRANSANIMASCALE);
					intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
					mContext.registerReceiver(mActivityResumeReceiver, intentFilter);
					
					Resources res = mContext.getResources();
					String[] resNames = {"status_bar_textColor_darkmode", "status_bar_textColor"};
					int k;
					for (int i=0; i<2; i++) {
						try {
							k = res.getIdentifier(resNames[i], "color", "com.android.systemui");
							if (k > 0)
								mIconColors[i] = res.getColor(k);
							Utils.log("mIconColor: " + mIconColors[i]);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}					
					
					Runnable darkModeRunnable = new Runnable() {

						@Override
						public void run() {
							updateDarkMode(mContext);
						}
					};
					XposedHelpers.setObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable", darkModeRunnable);	
					
					Class<?> ServiceManager = XposedHelpers.findClass("android.os.ServiceManager", null);
					Object WindowService = XposedHelpers.callStaticMethod(ServiceManager, "getService", "window");
					Class<?> IWindowManagerStub = XposedHelpers.findClass("android.view.IWindowManager.Stub", null);
					Object WindowManager = XposedHelpers.callStaticMethod(IWindowManagerStub, "asInterface", WindowService);
					float transAnimScal = (Float) XposedHelpers.callMethod(WindowManager, "getAnimationScale", 1);
					mDelayTime = getDelayTime(transAnimScal);
					
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
			
		}
		 
	}
	private long getDelayTime(float animscale) {
		return (long) (animscale*350);
	}

}
