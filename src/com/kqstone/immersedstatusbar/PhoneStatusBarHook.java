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
	private String mPrePkgName = null;
	private int mPreColor = Color.BLACK;
	private boolean mPreDarkMode = false;
	private int[] mIconColors = {Color.parseColor("#80000000"),Color.parseColor("#99ffffff")};
	private int mAlphaFilter;
	private long mDelayTime = 1L;
	
	private Drawable mBackgroundBeforeUnbind;
	private Boolean mDarkModeBeforeUndbind;
	
	private Handler handler;
	
	private BroadcastReceiver mActivityResumeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					Constant.INTENT_CHANGE_STATUSBAR_COLOR)) {
				
				String pkgName = intent.getStringExtra(Constant.PKG_NAME);
				String actName = intent.getStringExtra(Constant.ACT_NAME);
				Utils.log("PKG_NAME:" + pkgName + "; ACT_NAME:" + actName);
				if (!(pkgName.equals("com.android.keyguard") && actName.equals("MiuiKeyGuard"))
						&& Utils.isKeyguardLocked(mContext))
					return;
				if (mPrePkgName != null)
					Utils.log("PRE_PKG_NAME:" + mPrePkgName);
				boolean fastTrans = intent.getBooleanExtra(Constant.FAST_TRANSITION, false);
				if (pkgName != null) {
					if (mPrePkgName != null && pkgName.equals(mPrePkgName)) {
						fastTrans = true;
					} else {
						mPrePkgName = pkgName;
					}
				}
				Utils.log("fastTransition: " + fastTrans);
				
				int type = intent.getIntExtra(Constant.STATUSBAR_BACKGROUND_TYPE, 0);
				switch (type) {
				case 0:
					int color = intent.getIntExtra(
							Constant.STATUSBAR_BACKGROUND_COLOR, Color.BLACK);
					if (color != mPreColor) {
						updateStatusBarBackground(new ColorDrawable(color), fastTrans);
						mPreColor = color;
					}
					break;
				case 1:
					String path = intent.getStringExtra(Constant.STATUSBAR_BACKGROUND_PATH);
					Bitmap bitmap = BitmapFactory.decodeFile(path);
					updateStatusBarBackground(new BitmapDrawable(bitmap), fastTrans);
					mPreColor = Constant.UNKNOW_COLOR;
				}
				
				boolean darkMode = intent.getBooleanExtra(Constant.IS_DARKMODE, false);
				Utils.log("Darkmode: " + darkMode + "; PreDarkMode: " + mPreDarkMode);
				
				if (darkMode != mPreDarkMode) {
					updateStatusBarContent(darkMode, fastTrans);
					mPreDarkMode = darkMode;
				}
				
			} else if (intent.getAction().equals(
					Constant.INTENT_UPDATE_NOTIFICATION_ICONS)) {
				mAlphaFilter = (Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;
				refreshNotificationIcons();
			} else if (intent.getAction().equals(
					Constant.INTENT_UPDATE_TRANSANIMASCALE)) {
				float scale = intent.getFloatExtra(Constant.TRANS_ANIM_SCALE, 1F);
				mDelayTime = getDelayTime(scale);
			} else if (intent.getAction().equals(Constant.INTENT_RESTART_SYSTEMUI)){
				restartSystemUI();
			}
		}
		
	};
	
	private void updateDarkMode(Context context) {
		Object simpleStatusbar = XposedHelpers.getObjectField(instancePhoneStatusBar, "mSimpleStatusbar");
		if (simpleStatusbar != null) {		
			XposedHelpers.callMethod(simpleStatusbar, "updateDarkMode");
			
			boolean fastAnim = Settings.System.getInt(context.getContentResolver(), Constant.KEY_PREF_QUICKANIM_CONTENT, 0) ==1 ? true:false;
			Utils.log("Is fast Animate Statusbar Content: " + fastAnim);
			if (!fastAnim)
				ObjectAnimator.ofFloat(simpleStatusbar, "transitionAlpha", new float[] { 0.0F, 1.0F }).setDuration(500L).start();
		}
	}

	private void updateStatusBarContent(boolean darkmode, boolean fastTrans) {
		Utils.log("darkmode: " + darkmode);
		XposedHelpers.setBooleanField(instancePhoneStatusBar, "mDarkMode", darkmode);
		Runnable runnable = (Runnable) XposedHelpers.getAdditionalInstanceField(instancePhoneStatusBar, "mMyUpdateDarkModeRunnable");
		long delaytime = fastTrans ? 0 : mDelayTime;
		handler.postDelayed(runnable, delaytime);
	}
	
	private void updateStatusBarBackground(final Drawable drawable, boolean fastTrans) {
		long delaytime = fastTrans ? 0 : mDelayTime;
		final View statusBarView = (View) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
		Runnable r = new Runnable() {

			@Override
			public void run() {
				statusBarView.setBackground(drawable);
//				ObjectAnimator.ofFloat(statusBarView, "transitionAlpha", new float[] { 0.0F, 0.5F, 1.0F })
//				.setDuration(Constant.TIME_FOR_STATUSBAR_BACKGROUND_TRANSITION).start();
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
		boolean darkmode = XposedHelpers.getBooleanField(instancePhoneStatusBar, "mDarkMode");
		int color = Utils.setAlphaForARGB(mIconColors[darkmode ? 0 : 1], mAlphaFilter);
		int alpha = Color.alpha(mIconColors[darkmode ? 0 : 1]) + 255 - mAlphaFilter;
		alpha = alpha < 255 ? alpha : 255;
		Utils.log("FilterAlpha: " + mAlphaFilter + "; ViewAlpha: " + alpha);
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
    
    private void restartSystemUI() {
    	XposedHelpers.callMethod(instancePhoneStatusBar, "unbindViews");
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
					intentFilter.addAction(Constant.INTENT_RESTART_SYSTEMUI);
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
						}
					};
					XposedHelpers.setObjectField(instancePhoneStatusBar, "mUpdateDarkModeRunnable", darkModeRunnable);	
					
					Runnable MyDarkModeRunnable = new Runnable() {

						@Override
						public void run() {
							updateDarkMode(mContext);
						}
					};
					XposedHelpers.setAdditionalInstanceField(instancePhoneStatusBar, "mMyUpdateDarkModeRunnable", MyDarkModeRunnable);	
					
					Class<?> ServiceManager = XposedHelpers.findClass("android.os.ServiceManager", null);
					Object WindowService = XposedHelpers.callStaticMethod(ServiceManager, "getService", "window");
					Class<?> IWindowManagerStub = XposedHelpers.findClass("android.view.IWindowManager.Stub", null);
					Object WindowManager = XposedHelpers.callStaticMethod(IWindowManagerStub, "asInterface", WindowService);
					float transAnimScal = (Float) XposedHelpers.callMethod(WindowManager, "getAnimationScale", 1);
					mDelayTime = getDelayTime(transAnimScal);
					
					mAlphaFilter = (Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_FILTER_ALPHA, 100) + 100) * 255 / 200;
				}
				
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"bindViews", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					if (mBackgroundBeforeUnbind != null && mDarkModeBeforeUndbind != null) {
						Utils.log("update statusbar background and darkmode>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
						updateStatusBarBackground(mBackgroundBeforeUnbind, true);
						updateStatusBarContent(mDarkModeBeforeUndbind, true);
					}
				}
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"unbindViews", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
					View statusBarView = (View) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
					mBackgroundBeforeUnbind = statusBarView.getBackground();
					mDarkModeBeforeUndbind = mPreDarkMode;
				}
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader), 
					"updateExpandedViewPos", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					ViewGroup statusBarView = (ViewGroup) XposedHelpers.getObjectField(instancePhoneStatusBar, "mStatusBarView");
					float statusBarAlpha = XposedHelpers.getFloatField(instancePhoneStatusBar, "mStatusBarAlpha");
					float statusBarTransitionAlpha = XposedHelpers.getFloatField(instancePhoneStatusBar, "mStatusBarTransitionAlpha");
					statusBarView.setAlpha(statusBarAlpha);
					Drawable background = statusBarView.getBackground();
					int color;
					float offset;
					if (background instanceof ColorDrawable && mPreColor != Color.TRANSPARENT) {
						offset = (1.0F - statusBarTransitionAlpha) * (Utils.getColorVal(mPreColor) - 0.27F) / 1.0F;
						if (offset < 0)
							offset = 0.0F;
						color = Utils.offsetValueForColor(mPreColor, offset);
						statusBarView.setBackground(new ColorDrawable(color));
						Utils.log("offset statusBar background val: " + (1.0F - statusBarTransitionAlpha));
						
						for (int i = 0; i < statusBarView.getChildCount(); i++) {
							if (! (statusBarView.getChildAt(i) instanceof ViewGroup))
								Utils.log("set statusbar content alpha>>>>>>");
								statusBarView.getChildAt(i).setAlpha(statusBarAlpha * statusBarTransitionAlpha);
						}
					} else {
						statusBarView.setAlpha(statusBarAlpha * statusBarTransitionAlpha);
					}
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
		if (animscale == 0)
			return 0;
		return (long) (animscale*340)+50;
	}

}
