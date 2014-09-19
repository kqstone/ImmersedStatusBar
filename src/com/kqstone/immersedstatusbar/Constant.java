package com.kqstone.immersedstatusbar;

import de.robv.android.xposed.XposedHelpers;
import android.graphics.Color;

public final class Constant {
	public static final boolean DEBUG = false;

	public static final boolean DBG_IMAGE = false;
	
	public static final String MODULE = "ImmersedStatusbar";
	
	public static final String INTENT_CHANGE_STATUSBAR_COLOR = "com.kqstone.immersedstatusbar.CHANGESTATUSBARCOLOR";

	public static final String IS_DARKMODE = "is_darkmode";
	public static final String STATUSBAR_BACKGROUND_COLOR = "background_color";
	public static final String DARKMODE_HANDLE = "dark_handled";
	public static final int COLOR_BLACK = Color.parseColor("#ff000000");
	public static final int COLOR_TRANSPARENT = Color.parseColor("#00000000");
	
	public static final int OFFEST_FOR_GRADUAL_ACTIVITY = 8;
	public static final int DELAY_GET_CACHEDRAWABLE = 150;
	
//	public static final int DISPLAY_HEIGHT = 1280;
//	public static final int DISPLAY_WIDTH = 720;
//	public static final int STATUS_BAR_HEIGHT = 50;

}
