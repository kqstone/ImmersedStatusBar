package com.kqstone.immersedstatusbar.settings;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.widget.Toast;

import com.kqstone.immersedstatusbar.Const;
import com.kqstone.immersedstatusbar.R;
import com.kqstone.immersedstatusbar.helper.ProfileDownload;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {
	private final static String KEY_PREF_ABOUT = "about";
	private final static String KEY_PREF_PROFILE = "profile";
	private static final String KEY_SWITCH_DEBUG = "switch_debug";
	public static final String KEY_PREF_FILTER_ALPHA = "key_filter_alpha";

	private Context mContext;
	private Preference mPrefAbout;
	private Preference mPrefDownoadProfile;
	private CheckBoxPreference mPrefForceTint;
	private CheckBoxPreference mPreTintNotification;
	private CheckBoxPreference mPreQuickAnimContent;
	private CheckBoxPreference mPreGetUsrColor;
	private PreferenceCategory mDebugprefCategory;
	private Preference mSwitchDebug;
	private CheckBoxPreference mPreExptInform;
	private CheckBoxPreference mPreExptInformToFile;
	private Preference mPreFilterAlpha;
	private Notify mNotify;

	private boolean mDebugMode = false;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mNotify.cancelNotification(0);
			boolean success = msg.what == 1 ? true : false;
			String text;
			if (!success) {
				text = mContext.getResources().getString(R.string.fail)
						+ "!";
				mNotify.setIcon(R.drawable.ic_stat_download_fail);
				mNotify.setEvent(text, text, null);
				mNotify.setFlags(Notification.FLAG_AUTO_CANCEL);
				mNotify.showNotification(0);
			} else {
				text = mContext.getResources().getString(R.string.success)
						+ "!";
			}
			Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();			
		}
	};

	class DownLoadThread implements Runnable {

		@Override
		public void run() {
			boolean success;
			success = ProfileDownload.downloadZip();
			if (success)
				success = ProfileDownload.unzip();
			if (success)
				success = ProfileDownload.copyToDest();
			Message msg = new Message();
			msg.what = success ? 1 : 0;
			handler.sendMessage(msg);
		}

	}

	class Dialog {
		public static final int ALERT_DEBUG = 0;
		private AlertDialog.Builder mBuilder;
		private Context mContext;
		private AlertDialog mDialog;

		public Dialog(Context context, int what) {
			mContext = context;
			mBuilder = new AlertDialog.Builder(context);
			switch (what) {
			case 0:
				mBuilder.setTitle(R.string.title_waring_debug);
				mBuilder.setMessage(R.string.message_waring_debug);
				mBuilder.setNegativeButton(R.string.button_cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								if (mDialog != null)
									mDialog.dismiss();
							}
						});
				mBuilder.setPositiveButton(R.string.button_ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								onDebugSwitchChanged(false);
							}
						});
				break;
			default:
				break;
			}
			mDialog = mBuilder.create();
		}

		public void show() {
			mDialog.show();
		}
	}

	class Notify {
		private Context mContext;
		private NotificationManager mNotificationManager;
		private Notification mNotification;

		public Notify(Context context) {
			mContext = context;
			mNotificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotification = new Notification();
		}

		public Notify(Context context, String tickerText, String contentTitle,
				String contentText, int flags) {
			this(context);
			mNotification.tickerText = tickerText;
			mNotification.flags |= flags;
			Intent notificationIntent = new Intent(mContext,
					SettingsActivity.class);
			PendingIntent contentItent = PendingIntent.getActivity(mContext, 0,
					notificationIntent, 0);
			mNotification.setLatestEventInfo(mContext, contentTitle,
					contentText, contentItent);
		}

		public void setEvent(String tickerText, String contentTitle,
				String contentText) {
			mNotification.tickerText = tickerText;
			Intent notificationIntent = new Intent(mContext,
					SettingsActivity.class);
			PendingIntent contentItent = PendingIntent.getActivity(mContext, 0,
					notificationIntent, 0);
			mNotification.setLatestEventInfo(mContext, contentTitle,
					contentText, contentItent);
		}

		public void setFlags(int flags) {
			mNotification.flags = flags;
		}

		public void setIcon(int iconId) {
			mNotification.icon = iconId;
		}

		public void showNotification(int id) {
			mNotification.when = System.currentTimeMillis();
			mNotificationManager.notify(id, mNotification);
		}

		public void cancelNotification(int id) {
			mNotificationManager.cancel(id);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mNotify = new Notify(mContext);
		this.setTitle(R.string.title_settings);
		this.addPreferencesFromResource(R.xml.settings);
		mPrefAbout = findPreference(KEY_PREF_ABOUT);
		mPrefAbout.setOnPreferenceClickListener(this);
		mPrefDownoadProfile = findPreference(KEY_PREF_PROFILE);
		mPrefDownoadProfile.setOnPreferenceClickListener(this);
		mPrefForceTint = (CheckBoxPreference) findPreference(Const.KEY_PREF_FORCE_TINT);
		mPrefForceTint.setChecked(Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_FORCE_TINT, 0) == 1 ? true : false);
		mPrefForceTint.setOnPreferenceChangeListener(this);
		mPreTintNotification = (CheckBoxPreference) findPreference(Const.KEY_PREF_TINT_NOTIFICATION);
		boolean isTintNoti = Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_TINT_NOTIFICATION, 0) == 1 ? true : false;
		mPreTintNotification.setChecked(isTintNoti);
		mPreTintNotification.setOnPreferenceChangeListener(this);
		mPreFilterAlpha = findPreference(Const.KEY_PREF_FILTER_ALPHA);
		int alpha = Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_FILTER_ALPHA, 100);
		this.mPreFilterAlpha.setSummary(getFilterAlphaValue(String
				.valueOf(alpha)));
		mPreFilterAlpha.setEnabled(isTintNoti);
		mPreFilterAlpha.setOnPreferenceClickListener(this);
		mPreQuickAnimContent = (CheckBoxPreference) findPreference(Const.KEY_PREF_QUICKANIM_CONTENT);
		mPreQuickAnimContent
				.setChecked(Settings.System.getInt(getContentResolver(),
						Const.KEY_PREF_QUICKANIM_CONTENT, 0) == 1 ? true
						: false);
		mPreQuickAnimContent.setOnPreferenceChangeListener(this);
		mPreGetUsrColor = (CheckBoxPreference) findPreference(Const.KEY_PREF_GET_USR_COLOR);
		mPreGetUsrColor.setChecked(Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_GET_USR_COLOR, 0) == 1 ? true : false);
		mPreGetUsrColor.setOnPreferenceChangeListener(this);
		mDebugprefCategory = (PreferenceCategory) findPreference("debug");
		mSwitchDebug = findPreference(KEY_SWITCH_DEBUG);
		mSwitchDebug.setOnPreferenceClickListener(this);
		mPreExptInform = (CheckBoxPreference) findPreference(Const.KEY_PREF_EXPORT_INFORM);
		boolean expInform = Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_EXPORT_INFORM, 0) == 1 ? true : false;
		mPreExptInform.setChecked(expInform);
		mPreExptInform.setOnPreferenceChangeListener(this);
		mPreExptInformToFile = (CheckBoxPreference) findPreference(Const.KEY_PREF_EXPORT_INFORM_TOFILE);
		boolean expInformToFile = Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_EXPORT_INFORM_TOFILE, 0) == 1 ? true : false;
		mPreExptInformToFile.setChecked(expInformToFile);
		mPreExptInformToFile.setOnPreferenceChangeListener(this);
		if (expInform || expInformToFile) {
			new Dialog(mContext, Dialog.ALERT_DEBUG).show();
			mDebugMode = true;
		}
		onDebugSwitchChanged(mDebugMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		int alpha = Settings.System.getInt(getContentResolver(),
				Const.KEY_PREF_FILTER_ALPHA, 100);
		this.mPreFilterAlpha.setSummary(getFilterAlphaValue(String
				.valueOf(alpha)));
	}

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		// TODO Auto-generated method stub
		String key = arg0.getKey();
		if (key.equals(KEY_PREF_ABOUT)) {
			Intent intent = new Intent();
			intent.setClass(this, About.class);
			this.startActivity(intent);
		} else if (key.equals(KEY_PREF_PROFILE)) {
			Toast.makeText(
					mContext,
					mContext.getResources().getString(
							R.string.begin_download_profiles),
					Toast.LENGTH_SHORT).show();
			String text = mContext.getResources().getString(
					R.string.begin_download_profiles);
			String textSummary = mContext.getResources().getString(
					R.string.begin_download_profiles_summary);
			mNotify.setIcon(R.drawable.ic_stat_download);
			mNotify.setEvent(text, text, textSummary);
			mNotify.setFlags(Notification.FLAG_ONGOING_EVENT);
			mNotify.showNotification(0);
			new Thread(new DownLoadThread()).start();
		} else if (key.equals(KEY_SWITCH_DEBUG)) {
			onDebugSwitchChanged(!mDebugMode);
		} else if (key.equals(KEY_PREF_FILTER_ALPHA)) {
			Intent intent = new Intent();
			intent.setClass(this, FilterAlphaSettingActivity.class);
			startActivity(intent);
		}
		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		String key = arg0.getKey();
		if (key.equals(Const.KEY_PREF_FORCE_TINT)) {
			Settings.System.putInt(getContentResolver(),
					Const.KEY_PREF_FORCE_TINT, (Boolean) arg1 ? 1 : 0);
			mPrefForceTint.setChecked((Boolean) arg1);
		} else if (key.equals(Const.KEY_PREF_TINT_NOTIFICATION)) {
			Settings.System.putInt(getContentResolver(),
					Const.KEY_PREF_TINT_NOTIFICATION, (Boolean) arg1 ? 1 : 0);
			this.mPreTintNotification.setChecked((Boolean) arg1);
			this.mPreFilterAlpha.setEnabled((Boolean) arg1);
			sendIntent(Const.INTENT_RESTART_SYSTEMUI);
			// Intent intent = new
			// Intent(Constant.INTENT_UPDATE_NOTIFICATION_ICONS);
			// this.sendBroadcast(intent);
		} else if (key.equals(Const.KEY_PREF_QUICKANIM_CONTENT)) {
			Settings.System.putInt(getContentResolver(),
					Const.KEY_PREF_QUICKANIM_CONTENT, (Boolean) arg1 ? 1 : 0);
			this.mPreQuickAnimContent.setChecked((Boolean) arg1);
		} else if (key.equals(Const.KEY_PREF_GET_USR_COLOR)) {
			Settings.System.putInt(getContentResolver(),
					Const.KEY_PREF_GET_USR_COLOR, (Boolean) arg1 ? 1 : 0);
			this.mPreGetUsrColor.setChecked((Boolean) arg1);
			this.notifyGetUserColor((Boolean) arg1);
		} else if (key.equals(Const.KEY_PREF_EXPORT_INFORM)) {
			Settings.System.putInt(getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM, (Boolean) arg1 ? 1 : 0);
			this.mPreExptInform.setChecked((Boolean) arg1);
		} else if (key.equals(Const.KEY_PREF_EXPORT_INFORM_TOFILE)) {
			Settings.System
					.putInt(getContentResolver(),
							Const.KEY_PREF_EXPORT_INFORM_TOFILE,
							(Boolean) arg1 ? 1 : 0);
			this.mPreExptInformToFile.setChecked((Boolean) arg1);
			// } else if (key.equals(Constant.KEY_PREF_FILTER_ALPHA)) {
			// Settings.System.putInt(getContentResolver(),
			// Constant.KEY_PREF_FILTER_ALPHA, Integer.parseInt((String) arg1));
			// this.mPreFilterAlpha.setValue(getFilterAlphaValue((String)
			// arg1));
			// this.mPreFilterAlpha.setSummary(getFilterAlphaValue((String)
			// arg1));
			// sendIntent(Constant.INTENT_UPDATE_NOTIFICATION_ICONS);
		}

		return false;
	}

	public void onDebugSwitchChanged(boolean switched) {
		mDebugMode = switched;
		if (!switched) {
			mSwitchDebug.setTitle(R.string.show_debug_settings);
			mDebugprefCategory.removePreference(mPreExptInform);
			mDebugprefCategory.removePreference(mPreExptInformToFile);
			Settings.System.putInt(mContext.getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM, 0);
			Settings.System.putInt(mContext.getContentResolver(),
					Const.KEY_PREF_EXPORT_INFORM_TOFILE, 0);
			mPreExptInform.setChecked(false);
			mPreExptInformToFile.setChecked(false);
		} else {
			mSwitchDebug.setTitle(R.string.hide_debug_settings);
			mDebugprefCategory.addPreference(mPreExptInform);
			mDebugprefCategory.addPreference(mPreExptInformToFile);
		}
	}

	private String getFilterAlphaValue(String entry) {
		String preFix = this.getResources().getString(
				R.string.summary_filter_alpha);
		return preFix + entry + "%";
	}

	private void sendIntent(String action) {
		Intent intent = new Intent(action);
		this.sendBroadcast(intent);
	}
	
	private void notifyGetUserColor(boolean show) {
		if (show) {
			String title = mContext.getResources().getString(R.string.text_title_get_user_color_mode);
			String summary = mContext.getResources().getString(R.string.text_content_get_user_color_mode);
			mNotify.setIcon(R.drawable.ic_stat_get_user_color);
			mNotify.setEvent(title, title, summary);
			mNotify.setFlags(Notification.FLAG_ONGOING_EVENT);
			mNotify.showNotification(1);
		} else {
			mNotify.cancelNotification(1);
		}
	}

}
