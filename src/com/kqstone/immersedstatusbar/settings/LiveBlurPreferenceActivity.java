package com.kqstone.immersedstatusbar.settings;

import com.koalcat.view.Blur;
import com.kqstone.immersedstatusbar.Utils;
import com.kqstone.immersedstatusbar.helper.BitMapColor;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

public class LiveBlurPreferenceActivity extends PreferenceActivity {
	private int blur_top_height = 0;
	private static int sDisplayWidth = 0;
	private static int sStatusBarHeight;
	private boolean mHasInit = false;
	private View mHeaderView;
	private Rect mRectBlurForTop;
	private Bitmap mCanvasBitmapforTop;
	private Canvas mCanvasforTop;
	private Blur mBlur;
	private boolean enableBlur = true;
	
	private ListView mListView;
	private ImageView mBlurForTop;
	private OnScrollListener mOnScrollListener = new OnScrollListener() {
		private int scrollState = 0;
		private long pastMillis = 0;
		private static final long threadMillis = 20;

		@Override
		public void onScroll(AbsListView listView, int arg1, int arg2, int arg3) {
			if (scrollState !=0) {
				long currentMillis = System.currentTimeMillis();
				if (currentMillis - pastMillis > threadMillis) {
					updateBlurMask((ListView)listView);
					pastMillis = currentMillis;
				}
			}
			
		}

		@Override
		public void onScrollStateChanged(AbsListView listView, int state) {
			scrollState = state;
		}
		
	};
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mListView = this.getListView();
		mListView.setOnScrollListener(mOnScrollListener);
		
		DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        sDisplayWidth = metric.widthPixels; 

        Utils.setTranslucentStatus(this);
	}
	
	public void setupBlurMargin(int blurMargin) {
		blur_top_height = blurMargin;
		enableBlur = true;
		
		init();
		
		if (blur_top_height > 0) {
			int count = mListView.getHeaderViewsCount();
			if (count > 0) mListView.removeHeaderView(mHeaderView);
			mHeaderView = new View(mListView.getContext());
			LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, blur_top_height);
			mHeaderView.setLayoutParams(params);
			mListView.addHeaderView(mHeaderView, null, false);
		}
		
		mListView.invalidate();
		
		FrameLayout contentView = (FrameLayout) this.getWindow().findViewById(android.R.id.content);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, blur_top_height);
		mBlurForTop = new ImageView(this);
		contentView.addView(mBlurForTop, lp);
	}
	
	public ImageView getViewForTop() {
		return mBlurForTop;
	}
	
	private void updateBlurMask(ListView listView) {
		if (enableBlur) {
			if (blur_top_height > 0) {
				mCanvasforTop.save();
				listView.draw(mCanvasforTop);
				mCanvasforTop.restore();
				Bitmap bitmap = mBlur.blur(mCanvasBitmapforTop, true);
				mBlurForTop.setImageBitmap(bitmap);
				mCanvasforTop.drawColor(Color.TRANSPARENT, Mode.CLEAR);
			}
		}
	}
	
	private void init() {
		if (enableBlur) {
			if (mRectBlurForTop == null) mRectBlurForTop = new Rect();
			mRectBlurForTop.left = 0;
			mRectBlurForTop.right = sDisplayWidth;
			mRectBlurForTop.top = 0;
			mRectBlurForTop.bottom = blur_top_height;
			
			recycle(false);
			
			if (blur_top_height > 0) {
				mCanvasBitmapforTop = Bitmap.createBitmap(mRectBlurForTop.right - mRectBlurForTop.left,
						blur_top_height, Config.ARGB_8888);
				mCanvasforTop = new Canvas(mCanvasBitmapforTop);
			}
			
			if (mBlur == null) {
				mBlur = new Blur(mListView.getContext());
			}
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean focused) {
		super.onWindowFocusChanged(focused);
		if (focused && !mHasInit) {
			int height;
			int color = Color.BLACK;
			if (sStatusBarHeight == 0) {
				Rect frame = new Rect();
				getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
				sStatusBarHeight = frame.top;
			}

			ActionBar actionbar = this.getActionBar();
			if (actionbar != null && actionbar.isShowing()) {
				FrameLayout container = (FrameLayout) ReflectionHelper
						.getObjectField(actionbar, "mContainerView");
				if (container != null) {
					Drawable backgroundDrawable = (Drawable) ReflectionHelper
							.getObjectField(container, "mBackground");
					if (backgroundDrawable != null) {
						BitMapColor bc = Utils
								.getBitmapColor(backgroundDrawable);
						color = bc.Color;
						int tempcolor = Color.argb(200, Color.red(bc.Color),
								Color.green(bc.Color), Color.blue(bc.Color));
						actionbar.setBackgroundDrawable(new ColorDrawable(
								tempcolor));
					}
				}
				height = actionbar.getHeight() + sStatusBarHeight;
			} else {
				height = sStatusBarHeight;
			}
			this.setupBlurMargin(height);
			mBlurForTop.setBackgroundColor(color);
			mHasInit = true;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		recycle(true);
	}

	private void recycle(boolean all) {
		
		if (mCanvasBitmapforTop != null && !mCanvasBitmapforTop.isRecycled()) {
			mCanvasBitmapforTop.recycle();
		}
		
		if (all) {
			if (mBlur != null) {
				mBlur.Destroy();
			}
		}
	}

}
