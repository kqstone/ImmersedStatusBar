package com.kqstone.immersedstatusbar;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.kqstone.immersedstatusbar.hook.ActivityHook;
import com.kqstone.immersedstatusbar.hook.MiuiKeyGuardHook;
import com.kqstone.immersedstatusbar.hook.PhoneStatusBarHook;
import com.kqstone.immersedstatusbar.hook.SettingsHook;
import com.kqstone.immersedstatusbar.hook.SimpleStatusbarHook;
import com.kqstone.immersedstatusbar.hook.StatusBarIconViewHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ImmersedStatusBar implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {
	private PhoneStatusBarHook mPhoneStatusBarHook;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		XposedBridge.hookAllConstructors(ActivityHook.sClazz,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) {
						XposedHelpers.setAdditionalInstanceField(
								param.thisObject, "mActivityHook",
								new ActivityHook(param.thisObject));
					}
				});

		XposedHelpers.findAndHookMethod(Activity.class, "onCreate",
				Bundle.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityHook) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mActivityHook")).hookAfterOnCreate();
					}
				});

		XposedHelpers.findAndHookMethod(Activity.class, "performResume",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityHook) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mActivityHook"))
								.hookAfterPerformResume();
					}
				});

		XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged",
				boolean.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityHook) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mActivityHook"))
								.hookAfterOnWindowFocusChanged((Boolean) param.args[0]);
					}
				});
		
		XposedHelpers.findAndHookMethod(Activity.class, "onPause",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						((ActivityHook) XposedHelpers
								.getAdditionalInstanceField(param.thisObject,
										"mActivityHook"))
								.hookAfterOnPause();
					}
				});

	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.android.systemui")) {
			XposedBridge.hookAllConstructors(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.PhoneStatusBar",
					lpparam.classLoader), new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					mPhoneStatusBarHook = new PhoneStatusBarHook(
							param.thisObject);
				}
			});

			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.PhoneStatusBar",
					lpparam.classLoader), "bindViews", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					mPhoneStatusBarHook.hookBeforeBindViews();
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					mPhoneStatusBarHook.hookAfterBindViews();
				}
			});

			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.PhoneStatusBar",
					lpparam.classLoader), "unbindViews", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					mPhoneStatusBarHook.hookBeforeUnBindViews();
				}
			});
			
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.PhoneStatusBar",
					lpparam.classLoader), "interceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					mPhoneStatusBarHook.hookBeforeInterceptTouchEvent((MotionEvent)param.args[0]);
				}
			});

			XposedBridge.hookAllConstructors(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.SimpleStatusBar",
					lpparam.classLoader), new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					XposedHelpers.setAdditionalInstanceField(param.thisObject,
							"mSimpleStatusbarHook", new SimpleStatusbarHook(
									param.thisObject));
				}
			});

			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.SimpleStatusBar",
					lpparam.classLoader), "updateNotificationIcons",
					boolean.class, ArrayList.class,
					LinearLayout.LayoutParams.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							((SimpleStatusbarHook) XposedHelpers
									.getAdditionalInstanceField(
											param.thisObject,
											"mSimpleStatusbarHook"))
									.hookAfterUpdateNotificationIcons();
						}
					});

			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.phone.SimpleStatusBar",
					lpparam.classLoader), "updateDarkMode",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							((SimpleStatusbarHook) XposedHelpers
									.getAdditionalInstanceField(
											param.thisObject,
											"mSimpleStatusbarHook"))
									.hookAfterUpdateDarkMode();
						}
					});

			Class<?> StatusBarIcon = XposedHelpers.findClass(
					"com.android.internal.statusbar.StatusBarIcon", null);
			XposedBridge.hookAllConstructors(XposedHelpers.findClass(
					"com.android.systemui.statusbar.StatusBarIconView",
					lpparam.classLoader), new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					XposedHelpers.setAdditionalInstanceField(param.thisObject,
							"mStatusBarIconViewHook",
							new StatusBarIconViewHook(param.thisObject));
				}
			});

			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.systemui.statusbar.StatusBarIconView",
					lpparam.classLoader), "setIcon", StatusBarIcon,
					new XC_MethodHook() {

						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							((StatusBarIconViewHook) XposedHelpers
									.getAdditionalInstanceField(
											param.thisObject,
											"mStatusBarIconViewHook"))
									.hookAfterSetIcon(param.args[0]);
						}
					});
		}

		if (lpparam.packageName.equals("com.android.keyguard")) {
			XposedHelpers.findAndHookMethod(
					"com.android.keyguard.MiuiKeyguardViewMediator",
					lpparam.classLoader, "handleShow", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							MiuiKeyGuardHook
									.hookAfterHandleShow(param.thisObject);
						}
					});
		}

		if (lpparam.packageName.equals("com.android.settings")) {
			XposedHelpers.findAndHookMethod(
					"com.android.settings.DevelopmentSettings",
					lpparam.classLoader, "a", int.class, ListPreference.class,
					Object.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							Object developmentSettings = param.thisObject;
							int which = (Integer) param.args[0];
							Object value = param.args[2];
							SettingsHook.hookAfterA(developmentSettings, which,
									value);
						}
					});
		}
	}

}
