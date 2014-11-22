package com.kqstone.immersedstatusbar;

public class ActivityProfile {
	private String mActName;
	private Integer mBgType;
	private String mColor;
	private String mBgFile;
	private Integer mOffset;
	
	public ActivityProfile() {
		mActName = null;
		mBgType = null;
		mColor = null;
		mOffset = null;
		mBgFile = null;
	}
	
	public String getActName() {
		return mActName;
	}
	
	public Integer getBgType() {
		return mBgType;
	}
	
	public String getBgColor() {
		return mColor;
	}
	
	public String getBgFile() {
		return mBgFile;
	}
	
	public Integer getOffset() {
		return mOffset;
	}
	
	public void setActName(String actName) {
		mActName = actName;
	}
	
	public void setBgType(int bgType) {
		this.mBgType = bgType;
	}
	
	public void setBgColor(String bgColor) {
		this.mColor = bgColor;
	}
	
	public void setBgFile(String bgFile) {
		this.mBgFile = bgFile;
	}

	public void setOffset(int offset) {
		this.mOffset = offset;
	}
	
	
	public final class BgType {
		public static final int COLOR = 0;
		public static final int PICTURE = 1;
	}

}
