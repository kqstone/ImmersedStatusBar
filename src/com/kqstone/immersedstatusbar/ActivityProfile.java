package com.kqstone.immersedstatusbar;

public class ActivityProfile {
	private String mActName;
	private int mBgType;
	private String mColor;
	private String mBgPath;
	private int mOffset;
	
	public String getActName() {
		return mActName;
	}
	
	public int getBgType() {
		return mBgType;
	}
	
	public String getBgColor() {
		return mColor;
	}
	
	public String getBgPath() {
		return mBgPath;
	}
	
	public int getOffset() {
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
	
	public void setBgPath(String bgPath) {
		this.mBgPath = bgPath;
	}
	
	public void setOffset(int offset) {
		this.mOffset = offset;
	}
	
	
	public final class BgType {
		public static final int COLOR = 0;
		public static final int PICTURE = 1;
	}

}
