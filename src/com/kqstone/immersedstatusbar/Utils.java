package com.kqstone.immersedstatusbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
	
	public static Bitmap getBitMapFromActivityBackground(Activity activity, boolean transparent) {
		View view = activity.getWindow().getDecorView();
		view.destroyDrawingCache();
		view.setDrawingCacheEnabled(true);
		Bitmap bitmap1 = view.getDrawingCache();	
		
		if (bitmap1 == null) 
			return null;
		int top = 0;
		if (!transparent){
			Rect rect = new Rect();
			view.getWindowVisibleDisplayFrame(rect);
			top = rect.top;	
			Utils.log("statusbar height: " + top);
		}
		
		int width = bitmap1.getWidth() / 4;
		try {
			Bitmap bitmap = Bitmap.createBitmap(bitmap1, width / 2, top, width, Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
			
			return bitmap;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
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
			bmc.Color = color3;
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
		
		ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
		if (rootView == null)
			return;
		int top = rootView.getPaddingTop() - offsetHeight;
		int left = rootView.getPaddingLeft();
		int right = rootView.getPaddingRight();
		int bottom = rootView.getPaddingBottom();
		rootView.setPadding(left, top, right, bottom);
//		
	}
	
	public static void logoutActivityInform(Activity activity) {
		String packageName = activity.getPackageName();
		String activityName = activity.getLocalClassName();
		Utils.log("PackageName: " + packageName + "\n" +"ActivityName: " + activityName);
	}
	
	public static WindowType getWindowType(Activity activity) {
		String pkgName = activity.getPackageName();
		if (pkgName.equals("cn.wps.moffice_eng") || pkgName.equals("com.tencent.mobileqq")) {
			return WindowType.Normal;
		}
		Intent activityIntent = activity.getIntent();
		int flags = activity.getWindow().getAttributes().flags;
		if ((flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) == WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) {
			return WindowType.Translucent;
		}

		if ((flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
			return WindowType.Fullscreen;
		}

		if (activityIntent != null
				&& (activityIntent.getFlags() & 0x00002000) == 0x00002000) {
			return WindowType.Float;
		}
		// From Xposed SwipeBack by PeterCxy
		// https://github.com/LOSP/SwipeBack/blob/master/src/us/shandian/mod/swipeback/hook/ModSwipeBack.java
		int isFloating = XposedHelpers.getStaticIntField(XposedHelpers
				.findClass("com.android.internal.R.styleable", null),
				"Window_windowIsFloating");
		if (activity.getWindow().getWindowStyle().getBoolean(isFloating, false)) {
			Utils.log("is Floating window, ignore");
			return WindowType.Float;
		}
		return WindowType.Normal;
	}
	
	public static boolean isSystemApp(Activity activity) {
		ApplicationInfo info = activity.getApplicationInfo();
		boolean issysapp = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
				|| (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
		return issysapp;
	}
	
	public static Bitmap toGrayscale(Bitmap bmpOriginal) {
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
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
	
	public static void logStandXml(Activity activity) {
		Log.d(Constant.MODULE,
				"\n\n\n" +
				"-------------------------------------------------------------");
		String pkgName = activity.getPackageName();
		String actName = activity.getLocalClassName();
		Log.d(Constant.MODULE, "PackageName:" + pkgName);
		Log.d(Constant.MODULE, "ActivityName:" + actName);
		Log.d(Constant.MODULE, "Copy the following text and stored as "
				+ pkgName + ".xml file.");
		Log.d(Constant.MODULE,
				"*************************************************************");
		
		Log.d(Constant.MODULE, genStandXmls(pkgName, actName));
		Log.d(Constant.MODULE,
				"*************************************************************");
	}

	public static boolean exportStandXml(Activity activity) {
		try {
			String path = Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ "/isb/log/profile/";
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			path = path + activity.getPackageName() + ".xml";
			FileOutputStream os = new FileOutputStream(path);
			OutputStreamWriter osw = new OutputStreamWriter(os);
			String log = genStandXmls(activity.getPackageName(),
					activity.getLocalClassName());
			osw.write(log);
			osw.close();
			os.close();
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static void setTranslucentStatus(Activity activity) {
		Window win = activity.getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		win.setAttributes(winParams);
	}
	
	public static String getHexFromColor(int color) {
		String r = Integer.toHexString(Color.red(color));
		if (r.length() ==1)
			r = "0"+r;
		String g = Integer.toHexString(Color.green(color));
		if (g.length() ==1)
			g = "0"+g;
		String b = Integer.toHexString(Color.blue(color));
		if (b.length() ==1)
			b = "0"+b;
		return r+g+b;
	}
	
	public static int getRGBFromARGB(int argbColor) {
		int r = Color.red(argbColor);
		int b = Color.blue(argbColor);
		int g = Color.green(argbColor);
		return Color.rgb(r, g, b);
	}
	
	public static int setAlphaForARGB(int argbColor, int alpha) {
		int r = Color.red(argbColor);
		int b = Color.blue(argbColor);
		int g = Color.green(argbColor);
		return Color.argb(alpha, r, g, b);
	}
	
	public static int offsetValueForColor(int color, float offset) {
		float[] hsv = {0,0,0};
		Color.colorToHSV(color, hsv);
		hsv[2] = hsv[2] - offset;
		return Color.HSVToColor(hsv);
	}
	
	public static float getColorVal(int color) {
		float[] hsv = {0,0,0};
		Color.colorToHSV(color, hsv);
		return hsv[2];
	}


	public static void outputBitmapToFile(final Bitmap bitmap, final Activity activity)  {
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/isb/log/img/";
		path = path + activity.getPackageName() + "/";
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		final String fname = path + activity.getLocalClassName() + ".png";
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
	

	public static String genStandXml(String pkgName, String actName) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			// <?xml version="1.0" encoding=”UTF-8″ standalone=”yes”?>
			serializer.startDocument("UTF-8", true);
			// <profile>
			serializer.startTag(null, "profile");
			// <activity name="actName">
			serializer.startTag(null, "activity");
			serializer.attribute(null, ProfileHelper.KEY_NAME, actName);
			// <backgroundtype>Android XML</backgroundtype>
			serializer.startTag(null, ProfileHelper.KEY_BACKGROUNDTYPE);
			serializer
					.text("replace this text with 0 or 1 (0 = color, 1=image)");
			serializer.endTag(null, ProfileHelper.KEY_BACKGROUNDTYPE);
			// <color>Android XML</color>
			serializer.startTag(null, ProfileHelper.KEY_COLOR);
			serializer.text("replace this text with RGB(like c6c6c6)");
			serializer.endTag(null, ProfileHelper.KEY_COLOR);
			// <offset>Android XML</offset>
			serializer.startTag(null, ProfileHelper.KEY_OFFSET);
			serializer.text("replace this text with offset value (like 5)");
			serializer.endTag(null, ProfileHelper.KEY_OFFSET);
			// </activity>
			serializer.endTag(null, "activity");
			// </profile>
			serializer.startTag(null, "profile");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String genStandXmls(String pkgName, String actName) {
		StringWriter writer = new StringWriter();
		writer.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>");
		writer.append("\n");
		writer.append("<profile>");
		writer.append("\n");
		writer.append("<activity name=\"" + actName + "\">");
		writer.append("\n");
		writer.append("\t");
		writer.append("<backgroundtype>replace with 0 or 1</backgroundtype>");
		writer.append("\n");
		writer.append("\t");
		writer.append("<color>replace with RGB value</color>");
		writer.append("\n");
		writer.append("\t");
		writer.append("<offset>replace with offset value</offset>");
		writer.append("\n");
		writer.append("</activity>");
		writer.append("\n");
		writer.append("</profile>");
		return writer.toString();
	}

	public static void copyFile(File sourcefile, File targetFile)
			throws IOException {

		// 新建文件输入流并对它进行缓冲
		FileInputStream input = new FileInputStream(sourcefile);
		BufferedInputStream inbuff = new BufferedInputStream(input);

		// 新建文件输出流并对它进行缓冲
		FileOutputStream out = new FileOutputStream(targetFile);
		BufferedOutputStream outbuff = new BufferedOutputStream(out);

		// 缓冲数组
		byte[] b = new byte[1024 * 5];
		int len = 0;
		while ((len = inbuff.read(b)) != -1) {
			outbuff.write(b, 0, len);
		}

		// 刷新此缓冲的输出流
		outbuff.flush();

		// 关闭流
		inbuff.close();
		outbuff.close();
		out.close();
		input.close();

	}

	public static void copyDirectiory(String sourceDir, String targetDir)
			throws IOException {

		// 新建目标目录

		(new File(targetDir)).mkdirs();

		// 获取源文件夹当下的文件或目录
		File[] file = (new File(sourceDir)).listFiles();

		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()) {
				// 源文件
				File sourceFile = file[i];
				// 目标文件
				File targetFile = new File(
						new File(targetDir).getAbsolutePath() + File.separator
								+ file[i].getName());

				copyFile(sourceFile, targetFile);

			}

			if (file[i].isDirectory()) {
				// 准备复制的源文件夹
				String dir1 = sourceDir + file[i].getName();
				// 准备复制的目标文件夹
				String dir2 = targetDir + "/" + file[i].getName();

				copyDirectiory(dir1, dir2);
			}
		}

	}
	
	public static boolean deleteDirectory(String sPath) {  
	    //如果sPath不以文件分隔符结尾，自动添加文件分隔符  
	    if (!sPath.endsWith(File.separator)) {  
	        sPath = sPath + File.separator;  
	    }  
	    File dirFile = new File(sPath);  
	    //如果dir对应的文件不存在，或者不是一个目录，则退出  
	    if (!dirFile.exists() || !dirFile.isDirectory()) {  
	        return false;  
	    }  
	    //删除文件夹下的所有文件(包括子目录)  
	    boolean flag = false;
	    File[] files = dirFile.listFiles();  
	    for (int i = 0; i < files.length; i++) {  
	        //删除子文件  
	        if (files[i].isFile()) {  
	            flag = deleteFile(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        } //删除子目录  
	        else {  
	            flag = deleteDirectory(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        }  
	    }  
	    if (!flag) return false;  
	    //删除当前目录  
	    if (dirFile.delete()) {  
	        return true;  
	    } else {  
	        return false;  
	    }  
	}  
	
	public static boolean deleteFile(String sPath) { 
	    File file = new File(sPath);  
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	        return true;  
	    }  
	    return false;  
	}  
	
	public static void sendTintStatusBarIntent(Activity activity, int backgroundtype, int color, String path, boolean isdark, boolean fastTrans) {
		Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Constant.PKG_NAME, activity.getPackageName());
		intent.putExtra(Constant.ACT_NAME, activity.getLocalClassName());
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_TYPE, backgroundtype);
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, color);
		intent.putExtra(Constant.STATUSBAR_BACKGROUND_PATH, path);
		intent.putExtra(Constant.IS_DARKMODE, isdark);
		intent.putExtra(Constant.FAST_TRANSITION, fastTrans);
		
		Utils.log("backgroundtype:" + backgroundtype + ";" + "statusbar_background:" + color + "; " + 
		"path:" + path + ";" + "dark_mode:" + isdark + "; " + "fast_transition:" + fastTrans);

		activity.sendBroadcast(intent);
	}
	
	enum WindowType {
		Normal, Float, Translucent, Fullscreen
	}

}