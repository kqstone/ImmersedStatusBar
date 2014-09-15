package com.kqstone.immersedstatusbar;

import de.robv.android.xposed.IXposedHookLoadPackage; 
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DebugClass implements IXposedHookLoadPackage {

    private static final String MODULE = "DebugClass";
    
    private static void log(String log) {
		Utils.log("[" + MODULE + "] " + log);
	}
    
    
	@Override
	public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("com.android.systemui")) {
            log("Start hook in: " + loadPackageParam.packageName);
            ClassLoader classLoader = loadPackageParam.classLoader;
            Class<?> PhoneStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);

            try {
            	XposedHelpers.findAndHookMethod(PhoneStatusBar, "disable", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    	log("disable: "+String.valueOf(methodHookParam.args[0]));
                    	int disable = XposedHelpers.getIntField(methodHookParam.thisObject, "mDisabled");
                    	log("disable before: " + disable);
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    	int disable = XposedHelpers.getIntField(methodHookParam.thisObject, "mDisabled");
                    	log("disable after: " + disable);
                    }
                });
            } catch (Throwable t) {
            	log("Class or method not found");
    			t.printStackTrace();
    		}   
            
        }
		
	}
	
}
