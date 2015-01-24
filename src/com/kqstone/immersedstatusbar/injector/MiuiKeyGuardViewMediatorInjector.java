package com.kqstone.immersedstatusbar.injector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.BitMapColor;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class MiuiKeyGuardViewMediatorInjector {
	private Context mContext;
	private boolean mDarkMode = false;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().equals(Const.INTENT_SET_LOCK_WALLPAPER)) {
				Utils.log("get set lock_wallpaper broadcast.....");
				genDarkMode();
			}
		}
	};

	public MiuiKeyGuardViewMediatorInjector(Object miuiKeyGuardViewMediator) {
		mContext = (Context) ReflectionHelper.getObjectField(
				miuiKeyGuardViewMediator, "mContext");
		IntentFilter filter = new IntentFilter();
		filter.addAction(Const.INTENT_SET_LOCK_WALLPAPER);
		mContext.registerReceiver(mReceiver, filter);
		genDarkMode();
	}

	public void hookAfterHandleShow() {
		sendKeyGuardStateChangedBroadcast(true);
		sendChangeStatusBroadcast();
		Utils.log("MiuiKeyGuard show, send intent");
	}
	
	public void hookAfterHandleHide() {
		sendKeyGuardStateChangedBroadcast(false);
		Utils.log("MiuiKeyGuard hide, send intent");
	}


	private void genDarkMode() {
		Bitmap lockwallPaper = null;
		try {
			lockwallPaper = BitmapFactory
					.decodeFile("/data/system/theme/lock_wallpaper");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (lockwallPaper != null) {
			BitMapColor bc = Utils.getBitmapColor(lockwallPaper);
			mDarkMode = Utils.getDarkMode(bc.Color);
			lockwallPaper.recycle();
		}
	}
	
	private void sendChangeStatusBroadcast() {
		Intent intent = new Intent(Const.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Const.PKG_NAME, "com.android.keyguard");
		intent.putExtra(Const.ACT_NAME, "MiuiKeyGuard");
		intent.putExtra(Const.STATUSBAR_BACKGROUND_COLOR, Color.TRANSPARENT);
		intent.putExtra(Const.IS_DARKMODE, mDarkMode);
		intent.putExtra(Const.FAST_TRANSITION, true);
		mContext.sendBroadcast(intent);
	}
	
	private void sendKeyGuardStateChangedBroadcast(boolean isLocked) {
		Intent intent = new Intent(Const.INTENT_KEYGUARD_STATE_CHANGED);
		intent.putExtra(Const.IS_LOCKED, isLocked);
		mContext.sendBroadcast(intent);
	}

}
