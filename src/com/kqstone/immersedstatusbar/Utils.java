package com.kqstone.immersedstatusbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources.NotFoundException;
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
import android.graphics.drawable.LayerDrawable;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.kqstone.immersedstatusbar.helper.BitMapColor;
import com.kqstone.immersedstatusbar.helper.ProfileHelper;
import com.kqstone.immersedstatusbar.helper.ReflectionHelper;

public class Utils {

	public static void log(String log) {
		if (Const.DEBUG)
			Log.d(Const.MODULE, log);
	}

	private static int sStatusbarHeight = 0;
	private static int sDisplayHeight = 0;

	public static int getMainColorFromActionBarDrawable(Drawable drawable)
			throws IllegalArgumentException {
		/*
		 * This should fix the bug where a huge part of the ActionBar background
		 * is drawn white.
		 */
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
		KeyguardManager kgm = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		boolean keyguardLocked;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			keyguardLocked = kgm.isKeyguardLocked();
		} else {
			keyguardLocked = kgm.inKeyguardRestrictedInputMode();
		}
		return keyguardLocked;
	}

	public static Bitmap getScreenShot(Context context) {
		Bitmap bitmap1 = (Bitmap) ReflectionHelper.callStaticMethod(
				ReflectionHelper.getClass("miui.util.ScreenshotUtils"),
				"getScreenshot", context);
		if (bitmap1 == null)
			return null;
		if (sStatusbarHeight == 0) {
			int id = (int) ReflectionHelper.getStaticField(
					ReflectionHelper.getClass("com.android.internal.R$dimen"),
					"status_bar_height");
			sStatusbarHeight = context.getResources().getDimensionPixelSize(id);
			Utils.log("static statusbar height: " + sStatusbarHeight);
		}

		int width = bitmap1.getWidth() / 4;
		try {
			Bitmap bitmap = Bitmap.createBitmap(bitmap1, width / 2,
					sStatusbarHeight, width, Const.OFFEST_FOR_GRADUAL_ACTIVITY);

			return bitmap;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static Bitmap getBitMapFromActivityBackground(Activity activity,
			boolean transparent) {
		View view = activity.getWindow().getDecorView();
		view.destroyDrawingCache();
		view.setDrawingCacheEnabled(true);
		Bitmap bitmap1 = view.getDrawingCache();

		if (bitmap1 == null)
			return null;
		int top = 0;
		if (!transparent) {
			if (sStatusbarHeight == 0) {
				int id = (int) ReflectionHelper.getStaticField(ReflectionHelper
						.getClass("com.android.internal.R$dimen"),
						"status_bar_height");
				sStatusbarHeight = activity.getResources()
						.getDimensionPixelSize(id);
				Utils.log("static statusbar height: " + sStatusbarHeight);
			}
			top = sStatusbarHeight;
		}

		int width = bitmap1.getWidth() / 4;
		try {
			Bitmap bitmap = Bitmap.createBitmap(bitmap1, width / 2, top, width,
					Const.OFFEST_FOR_GRADUAL_ACTIVITY);

			return bitmap;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static void setDecorViewBackground(Activity activity,
			Drawable drawable, boolean isCalledOnFocused) {
		View decorView = activity.getWindow().getDecorView();
		if (!isCalledOnFocused)
			decorView = ((ViewGroup) decorView).getChildAt(0);
		if (decorView == null) {
			Utils.log("can not get decorView");
			return;
		}
		Drawable dViewDrawable = decorView.getBackground();
		if (dViewDrawable == null) {
			dViewDrawable = new ColorDrawable(Color.TRANSPARENT);
		}
		Drawable[] drawables = new Drawable[2];
		drawables[0] = drawable;
		drawables[1] = dViewDrawable;
		LayerDrawable ld = new LayerDrawable(drawables);
		if (sDisplayHeight == 0) {
			DisplayMetrics metric = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
			sDisplayHeight = metric.heightPixels;
		}
		if (sStatusbarHeight == 0) {
			int id = (int) ReflectionHelper.getStaticField(
					ReflectionHelper.getClass("com.android.internal.R$dimen"),
					"status_bar_height");
			sStatusbarHeight = activity.getResources()
					.getDimensionPixelSize(id);
			Utils.log("static statusbar height: " + sStatusbarHeight);
		}
		ld.setLayerInset(0, 0, 0, 0, sDisplayHeight - sStatusbarHeight);
		ld.setLayerInset(1, 0, sStatusbarHeight, 0, 0);
		decorView.setBackground(ld);
	}

	public static BitMapColor getBitmapColor(Bitmap bitmap) {
		BitMapColor bmc = new BitMapColor();
		int width = bitmap.getWidth();
		int height = Const.OFFEST_FOR_GRADUAL_ACTIVITY;
		int color1 = bitmap.getPixel(0, 0);
		int color2 = bitmap.getPixel(width - 1, 0);
		int color3 = bitmap.getPixel(0, height - 1);
		int color4 = bitmap.getPixel(width - 1, height - 1);
		Utils.log("color1:" + color1);
		Utils.log("color2:" + color2);
		Utils.log("color3:" + color3);
		Utils.log("color4:" + color4);
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

		ViewGroup rootView = (ViewGroup) activity
				.findViewById(android.R.id.content);
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
		Utils.log("PackageName: " + packageName + "\n" + "ActivityName: "
				+ activityName);
	}

	public static WindowType getWindowType(Activity activity) {
		String pkgName = activity.getPackageName();
		if (pkgName.equals("cn.wps.moffice_eng")
				|| pkgName.equals("com.tencent.mobileqq")) {
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

	public static Bitmap drawableToBitmap(Drawable drawable)
			throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
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
		Log.d(Const.MODULE,
				"\n\n\n"
						+ "-------------------------------------------------------------");
		String pkgName = activity.getPackageName();
		String actName = activity.getLocalClassName();
		Log.d(Const.MODULE, "PackageName:" + pkgName);
		Log.d(Const.MODULE, "ActivityName:" + actName);
		Log.d(Const.MODULE, "Copy the following text and stored as " + pkgName
				+ ".xml file.");
		Log.d(Const.MODULE,
				"*************************************************************");

		Log.d(Const.MODULE, genStandXmls(pkgName, actName));
		Log.d(Const.MODULE,
				"*************************************************************");
	}

	public static boolean exportStandXml(Activity activity) {
		try {
			String path = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/isb/log/profile/";
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
		if (r.length() == 1)
			r = "0" + r;
		String g = Integer.toHexString(Color.green(color));
		if (g.length() == 1)
			g = "0" + g;
		String b = Integer.toHexString(Color.blue(color));
		if (b.length() == 1)
			b = "0" + b;
		return r + g + b;
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

	public static float getColorValue(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		return hsv[2];
	}

	public static int setColorValue(int color, float value) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] = value;
		int alpha = Color.alpha(color);
		return Color.HSVToColor(alpha, hsv);
	}

	public static void outputBitmapToFile(Bitmap bitmap) {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/isb/temp/";
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String fname = path + "screenshot.png";

		FileOutputStream out;
		try {
			out = new FileOutputStream(fname);
			bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
			// bitmap.recycle();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public static void outputBitmapToFile(final Bitmap bitmap,
			final Activity activity) {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/isb/log/img/";
		path = path + activity.getPackageName() + "/";
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		final String fname = path + activity.getLocalClassName() + ".png";
		new Thread() {
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
				} catch (IllegalStateException e) {
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
			// <?xml version="1.0" encoding=¡±UTF-8¡å standalone=¡±yes¡±?>
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
		FileInputStream input = new FileInputStream(sourcefile);
		BufferedInputStream inbuff = new BufferedInputStream(input);

		FileOutputStream out = new FileOutputStream(targetFile);
		BufferedOutputStream outbuff = new BufferedOutputStream(out);

		byte[] b = new byte[1024 * 5];
		int len = 0;
		while ((len = inbuff.read(b)) != -1) {
			outbuff.write(b, 0, len);
		}

		outbuff.flush();

		inbuff.close();
		outbuff.close();
		out.close();
		input.close();

	}

	public static void copyDirectiory(String sourceDir, String targetDir)
			throws IOException {
		(new File(targetDir)).mkdirs();
		File[] file = (new File(sourceDir)).listFiles();

		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()) {
				File sourceFile = file[i];
				File targetFile = new File(
						new File(targetDir).getAbsolutePath() + File.separator
								+ file[i].getName());
				copyFile(sourceFile, targetFile);
			}

			if (file[i].isDirectory()) {
				String dir1 = sourceDir + file[i].getName();
				String dir2 = targetDir + "/" + file[i].getName();
				copyDirectiory(dir1, dir2);
			}
		}

	}

	public static boolean deleteDirectory(String sPath) {
		if (!sPath.endsWith(File.separator)) {
			sPath = sPath + File.separator;
		}
		File dirFile = new File(sPath);
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		boolean flag = false;
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag)
					break;
			} else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag)
			return false;
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

	public static void sendTintStatusBarIntent(Activity activity,
			int backgroundtype, int color, String path, boolean isdark,
			boolean fastTrans) {
		Intent intent = new Intent(Const.INTENT_CHANGE_STATUSBAR_COLOR);
		intent.putExtra(Const.PKG_NAME, activity.getPackageName());
		intent.putExtra(Const.ACT_NAME, activity.getLocalClassName());
		intent.putExtra(Const.STATUSBAR_BACKGROUND_TYPE, backgroundtype);
		intent.putExtra(Const.STATUSBAR_BACKGROUND_COLOR, color);
		intent.putExtra(Const.STATUSBAR_BACKGROUND_PATH, path);
		intent.putExtra(Const.IS_DARKMODE, isdark);
		intent.putExtra(Const.FAST_TRANSITION, fastTrans);

		Utils.log("backgroundtype:" + backgroundtype + ";"
				+ "statusbar_background:" + color + "; " + "path:" + path + ";"
				+ "dark_mode:" + isdark + "; " + "fast_transition:" + fastTrans);

		activity.sendBroadcast(intent);
	}

	public enum WindowType {
		Normal, Float, Translucent, Fullscreen
	}
}