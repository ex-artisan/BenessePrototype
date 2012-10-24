package com.artisan.apps.blab;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;




import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;


/**
 * This activity is used for shot a new photo
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 *
 */
public class LiveCameraNative extends Activity {
	
	//TAG is used when debug
    private static final String TAG = "LiveCameraNative";
    
    //It is main camera
    private LiveCameraView mView;
    
    //Index of source resources
    public static final int SOUND_ID_SHUTTER        = 1;
    public static final int SOUND_ID_TING           = 2;
    public static final int SOUND_ID_SPRAYING           = 3;
    
    //Sound play object
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundPoolMap;
    
    //Preference reference
    SharedPreferences prefs;
    
    //Dialogs for show information
    ProgressDialog dialog, serverErr, sendCompleted;
    
    //Button object, show when ready for shot photo
    Button shotButton;
    
    //This butoon show norman, when not ready for shotting
    Button disShotButton;
    
	
	
    /**
     * This function is used for play a sound resource
     * @param soundID
     */
	private void playSound(int soundID) {
        AudioManager mgr = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING);
        float volume = streamVolumeMax;
        soundPool.play(soundID, volume, volume, 1, 1, 1f);
}
	
	/**
	 * Constructor funtion
	 */
    public LiveCameraNative() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    
    /**
     * This function is auto called when activity be paused
     */
    @Override
	protected void onPause() {
        Log.i(TAG, "onPause");
		super.onPause();
		if (null != mView)
			mView.releaseCamera();
	}

    
    /**
     * This function is called when re run (re active) this activity
     */
	@Override
	protected void onResume() {
        Log.i(TAG, "onResume");
		super.onResume();
		if((null != mView) && !mView.openCamera() ) {
			AlertDialog ad = new AlertDialog.Builder(this).create();  
			ad.setCancelable(false); // This blocks the 'BACK' button  
			ad.setMessage("Fatal error: can't open camera!");  
			ad.setButton("OK", new DialogInterface.OnClickListener() {  
			    public void onClick(DialogInterface dialog, int which) {  
				dialog.dismiss();
				finish();
			    }  
			});  
			ad.show();
		}
	}

    /** 
     * Called when the activity is first created. 
     * @param bundle object
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        
        if (HomeActivity.loaded == false) {
        	AlertDialog ad = new AlertDialog.Builder(LiveCameraNative.this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't load OpenCV library!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			    }
			});
			ad.show();
        }
        
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
 
        setContentView(R.layout.main);
        
        mView = (LiveCameraView) findViewById(R.id.cameraLiveView);

		// Check native OpenCV camera
		if( !mView.openCamera() ) {
			AlertDialog ad = new AlertDialog.Builder(LiveCameraNative.this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			    }
			});
			ad.show();
		}
        
        
        shotButton = (Button) findViewById(R.id.addNewPhotoButton);
        if (shotButton!=null) 
        shotButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	//LiveCameraNative.this.showUploadMsg();
                	
                    Log.i("LiveCamera", "====> click");
                    
                    Bitmap liveImg = mView.setModeToPainting();
                    
                    
                    int exportWidth = 0, exportHeight = 0;
                    
                    if (prefs != null) {
            			String prefWidth = prefs.getString("picWidth", "0");
            			if (prefWidth!=null && prefWidth!="") {
            				exportWidth = Integer.getInteger(prefWidth, 0); // .parseInt(prefWidth);
            			}
            			
            			
            			String prefHeight = prefs.getString("picHeight", "0");
            			if (prefHeight!=null && prefHeight!="") {
            				exportHeight = Integer.getInteger(prefHeight, 0);  // .parseInt(prefHeight);
            			}
            		}
                    
                    if (exportWidth > 0 && exportHeight > 0) {
                    	liveImg = Bitmap.createScaledBitmap(liveImg, exportWidth, exportHeight, true);
                    }
                    
                    //Bitmap liveImg = mView.takePhoto();
                    
					playSound(SOUND_ID_SHUTTER);
					if (liveImg==null  || liveImg.getWidth()==0) {
						Log.i(TAG, "====> ko lay dc anh");
						return;
					}
					
					Log.i(TAG, "====> lay dc anh");
					
					Bitmap.CompressFormat mComp = Bitmap.CompressFormat.PNG;
					String mExt = ".png";
					String myCompSetting = prefs.getString("listpref", "1");
					Log.d(TAG,"======>compress setting ="+myCompSetting);
					if (myCompSetting.equals("2")) {
						mComp = Bitmap.CompressFormat.JPEG;
						mExt = ".jpg";
					}
					
					// Compress the wall image into a ByteArray in PNG or JPEG format
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    liveImg.compress(mComp, 70, bos);
                    
                    String filename = getFilesDir() + "/pics"+mExt;
                    FileOutputStream fos = null;
                    try {
                            fos = new FileOutputStream(filename);
                            liveImg.compress(mComp, 70, fos);
                            
                            File subFolder = Common.getExternalFolder(LiveCameraNative.this, "exartisan_pics");
            				if (subFolder != null){
            				
            					File exFile = Common.getNewFile(subFolder, mExt);

            					Common.saveInternalFileToExternalStorage(LiveCameraNative.this, filename, exFile);
            				}
            				
            				
            				HomeActivity.imgAdapt.refress();
            				
            				
                            
                    } catch (FileNotFoundException e) {
                            e.printStackTrace();
                    }
                    
                    
                }
        });
        
        
        disShotButton = (Button) findViewById(R.id.addNewPhotoButtonDis);
        
     // Prepare some sound effects
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_ID_SHUTTER, soundPool.load(this, R.raw.shutter, 1));
        soundPoolMap.put(SOUND_ID_TING, soundPool.load(this, R.raw.ting, 1));
        soundPoolMap.put(SOUND_ID_SPRAYING, soundPool.load(this, R.raw.spraying, 1));
        
        
        
        dialog = new ProgressDialog(LiveCameraNative.this);
        dialog.setMessage("Uploading. Please wait...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        
        serverErr = new ProgressDialog(LiveCameraNative.this);
        serverErr.setMessage("Have error when send file to server.\nYou can check image file at: /sdcard/exartisan_pics/pics_MAXNUM.png");
        serverErr.setCancelable(true);
        
        
        sendCompleted = new ProgressDialog(LiveCameraNative.this);
        sendCompleted.setMessage("Send file to server be completed.");
        sendCompleted.setCancelable(true);
        
        mView.setMainActivity(LiveCameraNative.this);
        
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        
    }
    

    
    
    /**
     * This function is used for show shot photo button
     */
    public void enableShot(){
    	if (shotButton!=null) shotButton.setVisibility(View.VISIBLE);
    	if (disShotButton!=null) disShotButton.setVisibility(View.GONE);
    }
    
    
    /**
     * This function is used for un-show shot photo button
     */
    public void unEnableShot(){
    	if (shotButton!=null) shotButton.setVisibility(View.GONE);
    	if (disShotButton!=null) disShotButton.setVisibility(View.VISIBLE);
    }
    
    //handler is used for change the ui of this activity from other thread
    final public Handler uiHandler = new Handler();
    
    
    /**
     * Show private dialog
     */
    public void showUploadMsg() {
    	Log.i(TAG, "===>show dialog");
    	dialog.show();
    }
    
    
    /**
     * Hide private dialog
     */
    public void hideUploadMsg() {
    	Log.i(TAG, "===>hide dialog");
    	if (dialog!=null && dialog.isShowing()) dialog.dismiss();
    }
    
    
    /**
     * Play private sound resource
     */
    public void sprayingSound(){
    	playSound(SOUND_ID_SPRAYING);
    }
    
    
    
    
    
}
