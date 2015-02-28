package com.kqstone.immersedstatusbar.settings;

import com.koalcat.view.Blur;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
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
	private int blur_bottom_height = 0;
	private int blur_top_height = 0;
	private static int sDisplayWidth;
	private View mFooterView;
	private View mHeaderView;
	private Paint paint;
	private Rect mRectBlurForBottom;
	private Rect mRectBlurForTop;
	private Rect mRect;
	private Bitmap mCanvasBitmapforBottom;
	private Canvas mCanvasforBottom;
	private Bitmap mCanvasBitmapforTop;
	private Canvas mCanvasforTop;
	private Blur mBlur;
	private boolean enableBlur = true;
	
	private ListView mListView;
	private ImageView mBlurForTop;
	private OnScrollListener mOnScrollListener = new OnScrollListener() {
		private int scrollState = 0;

		@Override
		public void onScroll(AbsListView listView, int arg1, int arg2, int arg3) {
			if (scrollState !=0)
				updateBlurMask((ListView)listView);
		}

		@Override
		public void onScrollStateChanged(AbsListView listView, int state) {
			scrollState = state;
		}
		
	};
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bitmap bitmap = msg.getData().getParcelable("bitmap");
			mBlurForTop.setImageBitmap(bitmap);
			mCanvasforTop.drawColor(Color.TRANSPARENT, Mode.CLEAR);
		}
	};
	public LiveBlurPreferenceActivity() {
		super();
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mListView = this.getListView();
//		mListView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mListView.setOnScrollListener(mOnScrollListener);
	}
	
	public void setupBlurMargin(int topMargin, int bottomMargin) {

		DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        sDisplayWidth = metric.widthPixels; 
        
		blur_bottom_height = bottomMargin;
		blur_top_height = topMargin;
		enableBlur = true;
		
		init();
		
		if (blur_bottom_height > 0) {
			int count = mListView.getFooterViewsCount();
			if (count > 0) mListView.removeFooterView(mFooterView);
			mFooterView = new View(mListView.getContext());
			LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, blur_bottom_height);
			mFooterView.setLayoutParams(params);
			mListView.addFooterView(mFooterView, null, false);
		}
		
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
			
			final Rect rect = mRect;
			final Paint mPaint = paint;
			
			final Rect blurbottom = mRectBlurForBottom;
			final Bitmap canvasBitmapbottom = mCanvasBitmapforBottom;
			final Canvas mTempCanvasbottom = mCanvasforBottom;
			if (blur_bottom_height > 0) { 
				mTempCanvasbottom.save();
				mTempCanvasbottom.translate(0, -blurbottom.top);
				listView.draw(mTempCanvasbottom);
				mTempCanvasbottom.restore();
			}
			
			final Rect blurtop = mRectBlurForTop;
			final Bitmap canvasBitmaptop = mCanvasBitmapforTop;
			final Canvas mTempCanvastop = mCanvasforTop;
			if (blur_top_height > 0) {
				mTempCanvastop.save();
				listView.draw(mTempCanvastop);
				mTempCanvastop.restore();
			}
			Thread thread = new Thread() {
				@Override
				public void run() {
					if (blur_bottom_height > 0) {
						Bitmap mBitmap = mBlur.blur(canvasBitmapbottom, true);
//						mBlurForTop.setImageBitmap(mBitmap);
						
					}
					if (blur_top_height > 0) { 
						Bitmap mBitmap = mBlur.blur(canvasBitmaptop, true);
						Message msg = new Message();
						Bundle bundle = new Bundle();
						bundle.putParcelable("bitmap", mBitmap);
						msg.setData(bundle);
						mHandler.sendMessage(msg);
//						mBlurForTop.setImageBitmap(mBitmap);
//						mTempCanvastop.drawColor(Color.TRANSPARENT, Mode.CLEAR);
					}
				}
			};
//			thread.start();
//			mTempCanvastop.drawColor(Color.TRANSPARENT, Mode.CLEAR);

		} 
	}
	
	private void init() {
		if (enableBlur) {
			
			if (paint == null) {
				paint = new Paint();
				paint.setAntiAlias(true);
			}
			
			if (mRectBlurForTop == null) mRectBlurForTop = new Rect();
			mRectBlurForTop.left = 0;
			mRectBlurForTop.right = sDisplayWidth;
			mRectBlurForTop.top = 0;
			mRectBlurForTop.bottom = blur_top_height;
			
			if (mRectBlurForBottom == null) mRectBlurForBottom = new Rect();
			mRectBlurForBottom.left = 0;
			mRectBlurForBottom.right = sDisplayWidth;
			mRectBlurForBottom.bottom = sDisplayWidth;
			mRectBlurForBottom.top = mRectBlurForBottom.bottom - blur_bottom_height;
			
			if (mRect == null) mRect = new Rect();
			mRect.left = 0;
			mRect.right = sDisplayWidth;
			mRect.bottom = mRectBlurForBottom.top;
			mRect.top = mRectBlurForTop.bottom;
			
			recycle(false);
			
			if (blur_bottom_height > 0) {
				mCanvasBitmapforBottom = Bitmap.createBitmap(mRectBlurForBottom.right - mRectBlurForBottom.left,
						blur_bottom_height, Config.ARGB_8888);
				mCanvasforBottom = new Canvas(mCanvasBitmapforBottom);
			}
			
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
	protected void onDestroy() {
		super.onDestroy();
		recycle(true);
	}

	private void recycle(boolean all) {

		if (mCanvasBitmapforBottom != null && !mCanvasBitmapforBottom.isRecycled()) {
			mCanvasBitmapforBottom.recycle();
		}
		
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
