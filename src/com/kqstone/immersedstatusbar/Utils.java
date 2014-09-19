package com.kqstone.immersedstatusbar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class Utils {
	
	public static void log(String log) {
		if (Constant.DEBUG)
			Log.d(Constant.MODULE, log);
	}
	
	public static int getMainColorFromActionBarDrawable(Drawable drawable) throws IllegalArgumentException {
		/* This should fix the bug where a huge part of the ActionBar background is drawn white. */
		Drawable copyDrawable = drawable.getConstantState().newDrawable();

		if (copyDrawable instanceof ColorDrawable) {
			return ((ColorDrawable) drawable).getColor();
		}

		Bitmap bitmap = drawableToBitmap(copyDrawable);
		int pixel = bitmap.getPixel(0, 40);
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		int alpha = Color.alpha(pixel);
		copyDrawable = null;
		return Color.argb(alpha, red, green, blue);
	}

	public static boolean getDarkMode(int color) {

		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		float saturation = hsv[1];
		float value = hsv[2];
		Utils.log("Color Saturation:" + saturation + "; Color Value: " + value);
		if (saturation < 0.33 && value > 0.67) {
			return true;
		}
		return false;
	}
	
	@SuppressLint("NewApi")
	public static boolean isKeyguardLocked(Context context) {
		KeyguardManager kgm = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		boolean keyguardLocked;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			keyguardLocked = kgm.isKeyguardLocked();
		} else {
			keyguardLocked = kgm.inKeyguardRestrictedInputMode();
		}
		return keyguardLocked;
	}
	
	public static Bitmap getBitMapFromActivityBackground(Activity activity) {
		View view = activity.getWindow().getDecorView().getRootView();
		view.destroyDrawingCache();
		view.setDrawingCacheEnabled(true);
		Bitmap bitmap1 = view.getDrawingCache();	
		
		Rect rect = new Rect();
		view.getWindowVisibleDisplayFrame(rect);
		int statusbarHeight = rect.top;	
		Utils.log("statusbar height: " + statusbarHeight);
		
		if (bitmap1 == null) 
			return null;
		
		int width = bitmap1.getWidth() / 4;
		Bitmap bitmap = Bitmap.createBitmap(bitmap1, width / 2, statusbarHeight, width, Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
		
		return bitmap;
	}
	
//	public static Bitmap getBitMapFromActivityBackgroundOld(Activity activity) {
//		View view = activity.getWindow().getDecorView().getRootView();
//
//		view.destroyDrawingCache();
//		view.setDrawingCacheEnabled(true);
//		Bitmap bitmap1 = view.getDrawingCache();			
//				
//		if (bitmap1 == null) 
//			return null;
//		
//		outputBitmapToFile(bitmap1, activity);
//		
//		int corpTop = Constant.STATUS_BAR_HEIGHT;
//		int bitmap1Height = bitmap1.getHeight();
//		Utils.log("bitmap1Height: "+ bitmap1Height);
//		if (bitmap1Height < Constant.DISPLAY_HEIGHT) {
//			corpTop = 0;
//		} 
//		
//		// Crop and compress the image so that we don't get a TransactionTooLargeException.		
//		int width = bitmap1.getWidth() / 4;
//		Bitmap bitmap = Bitmap.createBitmap(bitmap1, width / 2, corpTop, width, Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
////		bitmap1.recycle();
//
//		return bitmap;
//	}
	
	public static BitMapColor getBitmapColor(Bitmap bitmap) {
		BitMapColor bmc = new BitMapColor();
		int width = bitmap.getWidth();
		int height = Constant.OFFEST_FOR_GRADUAL_ACTIVITY;
		int color1 = bitmap.getPixel(0, 0);
		int color2 = bitmap.getPixel(width - 1, 0);
		int color3 = bitmap.getPixel(0, height -1);
		int color4 = bitmap.getPixel(width - 1, height - 1);
		Utils.log("color1:" +color1);
		Utils.log("color2:" +color2);
		Utils.log("color3:" +color3);
		Utils.log("color4:" +color4);
		if (color1 != color2 || color3 != color4) {
			bmc.mType = BitMapColor.Type.PICTURE;
			return bmc;
		}
		if (color1 != color3) {
			bmc.mType = BitMapColor.Type.GRADUAL;
			bmc.Color = color3;
			return bmc;
		}
		bmc.mType = BitMapColor.Type.FLAT;
		bmc.Color = color1;
		bitmap.recycle();
		return bmc;		
	}
	
	public static void resetPadding(Activity activity, int offsetHeight) {
		
		ViewGroup decorView = (ViewGroup) activity.findViewById(android.R.id.content);
//		ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
		int top = decorView.getPaddingTop() - offsetHeight;
		int left = decorView.getPaddingLeft();
		int right = decorView.getPaddingRight();
		int bottom = decorView.getPaddingBottom();
		decorView.setPadding(left, top, right, bottom);
//		
	}
	
	public static void logoutActivityInform(Activity activity) {
		String packageName = activity.getPackageName();
		String activityName = activity.getLocalClassName();
		Utils.log("PackageName: " + packageName + "\n" +"ActivityName: " + activityName);
	}

	private static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}
		Bitmap bitmap;

		try {
			bitmap = Bitmap.createBitmap(1, 80, Config.ARGB_8888);
			bitmap.setDensity(480);
			Canvas canvas = new Canvas(bitmap); 
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			throw e;
		}

		return bitmap;
	}
	
	private static void outputBitmapToFile(final Bitmap bitmap, final Activity activity)  {
		if (!Constant.DBG_IMAGE)
			return;

		final String fname = "/sdcard/debug/" + activity.getLocalClassName() + ".png";
		new Thread(){
			@Override
			public void run() {
				FileOutputStream out;
				try {
					out = new FileOutputStream(fname);
					bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
					// bitmap.recycle();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}.start();

	}

}