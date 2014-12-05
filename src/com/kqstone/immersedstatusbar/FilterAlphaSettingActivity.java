package com.kqstone.immersedstatusbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FilterAlphaSettingActivity extends Activity {
	private SeekBar mFilterAlpha;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		mContext = this;
		this.setContentView(R.layout.filter_alpha_setting);
		this.setTitle(R.string.title_filter_alpha);
		mFilterAlpha = (SeekBar) findViewById(R.id.filter_alpha);
		int alpha = Settings.System.getInt(mContext.getContentResolver(), Constant.KEY_PREF_FILTER_ALPHA, 100);
		mFilterAlpha.setMax(50);
		mFilterAlpha.setProgress(alpha-50);
		mFilterAlpha.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (arg0 == mFilterAlpha) {
					Settings.System.putInt(mContext.getContentResolver(), Constant.KEY_PREF_FILTER_ALPHA, arg1 + 50);
					Intent intent = new Intent(Constant.INTENT_UPDATE_NOTIFICATION_ICONS);
					mContext.sendBroadcast(intent);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}});
	}

}
