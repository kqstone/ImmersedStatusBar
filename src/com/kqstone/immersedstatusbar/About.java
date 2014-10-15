package com.kqstone.immersedstatusbar;

import android.app.Activity;
import android.os.Bundle;

public class About extends Activity {
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		this.setContentView(R.layout.about);
		this.setTitle(R.string.about);
	}

}
