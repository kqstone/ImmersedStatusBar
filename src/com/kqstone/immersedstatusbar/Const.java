package com.kqstone.immersedstatusbar;

import android.content.Intent;

public final class Const {
	public static final boolean DEBUG = true;

	public static final String MODULE = "ISB";
	public static final String PKG_NAME_SELF = "com.kqstone.immersedstatusbar";
	public final static int UNKNOW_COLOR = 16777216;

	public static final String INTENT_CHANGE_STATUSBAR_COLOR = "com.kqstone.immersedstatusbar.CHANGESTATUSBARCOLOR";
	public static final String INTENT_CHANGE_STATUSBAR_DARKMODE = "com.kqstone.immersedstatusbar.CHANGESTATUSBARDARKMODE";;
	public static final String INTENT_UPDATE_NOTIFICATION_ICONS = "com.kqstone.immersedstatusbar.UPDATENOTIFICATIONICONS";
	public static final String INTENT_UPDATE_SETTINGS = "com.kqstone.immersedstatusbar.UPDATESETTINGS";
	public static final String INTENT_UPDATE_TRANSANIMASCALE = "com.kqstone.immersedstatusbar.UPDATETRANSANIMSCALE";
	public static final String INTENT_RESTART_SYSTEMUI = "com.kqstone.immersedstatusbar.RESTARTSYSTEMUI";
	public static final String INTENT_GET_USER_COLOR = "com.kqstone.immersedstatusbar.GETUSERCOLOR";
	public static final String INTENT_SET_LOCK_WALLPAPER = "com.miui.keyguard.setwallpaper";
	public static final String INTENT_SET_WALLPAPER = "com.kqstone.immersedstatusbar.SETWALLPAPER";
	public static final String INTENT_KEYGUARD_STATE_CHANGED = "com.kqstone.immersedstatusbar.KEYGUARDSTATECHANGED";

	public static final String PKG_NAME = "package_name";
	public static final String ACT_NAME = "activity_name";
	public static final String IS_DARKMODE = "is_darkmode";
	public static final String STATUSBAR_BACKGROUND_COLOR = "background_color";
	public static final String STATUSBAR_BACKGROUND_TYPE = "background_type";
	public static final String STATUSBAR_BACKGROUND_PATH = "background_path";
	public static final String ACTIVITY_OFFSET = "activity_offset";
	public static final String FAST_TRANSITION = "fast_transition";
	public static final String TRANS_ANIM_SCALE = "trans_anim_scale";
	public static final String IS_LOCKED = "is_locked";

	public static final int OFFEST_FOR_GRADUAL_ACTIVITY = 15;
	public static final int DELAY_GET_CACHEDRAWABLE = 0;

	public static final long TIME_FOR_STATUSBAR_BACKGROUND_TRANSITION = 100;

	// preference const
	public static final String PREF_SHOW_SET_PER_ACT = "show_pref_per_act";

	public final static String KEY_PREF_FORCE_TINT = "key_force_tint";
	public final static String KEY_PREF_TINT_NOTIFICATION = "key_tint_notification";
	public static final String KEY_PREF_QUICKANIM_CONTENT = "key_quickanim_content";
	public static final String KEY_PREF_EXPORT_INFORM = "key_export_inform";
	public static final String KEY_PREF_EXPORT_INFORM_TOFILE = "key_export_inform_tofile";
	public static final String KEY_PREF_FILTER_ALPHA = "key_filter_alpha";
	public static final String KEY_PREF_GET_USR_COLOR = "key_get_usr_color";

	public static final String PROFILE_URL = "http://github.com/kqstone/ISBpreferences/archive/master.zip";

}
