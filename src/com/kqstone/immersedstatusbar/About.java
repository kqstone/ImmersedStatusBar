package com.kqstone.immersedstatusbar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class About extends Activity {
	public static final String URL_AUTHOR = "https://github.com/kqstone/ImmersedStatusBar";
	public static final String URL_ACKNOW = "https://github.com/MohammadAG/Xposed-Tinted-Status-Bar";
	public static final String URL_PROFILE = "https://github.com/watcgfw/ISBpreferences";
	private View mAboutAuthorContent, mAcknowContent, mAboutProfileContent;
	
	private OnTouchListener mTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			boolean flag = false;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				((TextView)view).setTextColor(Color.parseColor("#ff888888"));
				flag = true;
				break;
			case MotionEvent.ACTION_MOVE:
				if (isInView(view, event)) {
					flag = true;
				} else {
					((TextView)view).setTextColor(Color.parseColor("#ff000000"));
					flag = false;
				}
				break;
			case MotionEvent.ACTION_UP:
				if(isInView(view, event)) {
					((TextView) view).setTextColor(Color.parseColor("#ff000000"));
					switch (view.getId()) {
					case R.id.acknow_content:
						jumpToUrl(URL_ACKNOW);
						break;
					case R.id.author_content:
						jumpToUrl(URL_AUTHOR);
						break;
					case R.id.profile_content:
						jumpToUrl(URL_PROFILE);
						break;
					}
					flag = true;
				}
				break;
			}
			return flag;
		}
		
		private boolean isInView(View view, MotionEvent event) {
			int[] location = {0, 0};
			view.getLocationOnScreen(location);
			int w = view.getWidth();
			int h = view.getHeight();
			int left = location[0];
			int top = location[1];
			float x = event.getRawX();
			float y = event.getRawY();
			if (x > left && x < left+w && y > top && y < top+h) {
				return true;
			}
			return false;
		}
	};
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.about);
		setTitle(R.string.about);
		mAboutAuthorContent = findViewById(R.id.author_content);
		mAboutAuthorContent.setOnTouchListener(mTouchListener);
		mAboutProfileContent = findViewById(R.id.profile_content);
		mAboutProfileContent.setOnTouchListener(mTouchListener);
		mAcknowContent = findViewById(R.id.acknow_content);
		mAcknowContent.setOnTouchListener(mTouchListener);
	}
	
	private void jumpToUrl(String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);
	}

}
