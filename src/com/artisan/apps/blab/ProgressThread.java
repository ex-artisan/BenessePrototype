package com.artisan.apps.blab;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ProgressThread extends Thread {	
	
	
	LiveCameraNative mC;
	String mFilePath;
	
	ProgressThread(LiveCameraNative c, String filePath) {
        mC = c;
        mFilePath = filePath;
    }
	
	@Override
    public void run() {

		//Common.sendFile(mC, mFilePath);
		this.destroy();
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();

	}
}