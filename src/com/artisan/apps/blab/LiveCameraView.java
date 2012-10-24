package com.artisan.apps.blab;

import java.io.IOException;

//import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;

import android.media.ExifInterface;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;


/**
 * 
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 *
 */
class LiveCameraView extends LiveCameraViewBase  {
	
	private int mFrameSize;
	private Bitmap mBitmap;
	private Bitmap mBitmap2;
	private int[] mRGBA;
	private int[] mRGBA2;

	public static final int     VIEW_MODE_IDLE              = 0;
    public static final int     VIEW_MODE_VIEWING               = 1;
    public static final int     VIEW_MODE_PAINTING              = 2;
        
    public static final int     MAX_BAD_MATHES_ALLOWED  = 5;
    public static final long    LOGO_DISPLAY_PERIOD_MS  = 4000;

    private static final int    TRACKING_IDLE           = 1;
    private static final int    TRACKING_STARTED        = 2;
    private static final int    TRACKING_GOOD           = 3;
    private static final int    TRACKING_LOST           = 4;

    private static int           mViewMode;
        
    private static long prevFTime = 0, startTime;
    private Mat mYuv;
    private Mat mRgba;
    private Mat mGraySubmat;
    private Mat mTrackOffset;
    private Mat mWarpImg;
    private Mat mWarpedImg;
    private boolean mMatchNWarpRunning=false;
    private Thread mThread;
    private int mMatchret=9;
    private int mNumMatches=0;
    private float offsetX, offsetY;
    private float offsetMatchX, offsetMatchY;
    private Bitmap mWarpedGraffiti;
    private boolean mGoodMatch = false;
    private int mNumBadMatches = 0;
    private int mTrackingStatus = TRACKING_IDLE;
	
    
    
	
    LiveCameraNative myMainActivity;
    SharedPreferences prefs = null;
	
    /**
     * Constructor
     * @param context
     */
    public LiveCameraView(Context context) {
        super(context);
        
        this.setOnTouchListener(new  OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction()==MotionEvent.ACTION_DOWN) {
//					runFocus();
				}
				return false;
			}
			
		});
        
    }
    
    
    /**
     * Constructor
     * @param context
     * @param attrs
     */
	public LiveCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isInEditMode()) {
			return;
		}
		mViewMode = VIEW_MODE_IDLE;
		startTime = SystemClock.uptimeMillis();
		Log.d("LiveCameraView", "on craete");
		
		this.setOnTouchListener(new  OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction()==MotionEvent.ACTION_DOWN) {
					runFocus();
				}
				return false;
			}
			
		});
	} 
	
	/**
	 * This function is used for set main activity of this camera
	 * @param activity context
	 */
	public void setMainActivity(LiveCameraNative c){
		myMainActivity = c;
		if (c!=null) prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		// Added by Huanvb for training database 
        byte[] yuv = new byte[1]; int[] rgba = new int[1]; int[] rgba2=new int[1]; boolean flag=true;
        FindFeatures(0, 0, yuv, rgba, rgba2, flag, flag);
        // end Huanvb
		
	}
    
    /**
     * manual run forcus
     */
	protected void runFocus() {
		AutoFocusCallBackImpl autoFocusCallBack = new AutoFocusCallBackImpl();
		mCamera.autoFocus(autoFocusCallBack);
	}
	
	/**
	 * This function is called when camera started preview
	 * @param int, width of camera
	 * @param int, height of camera
	 */
	@Override
	protected void onPreviewStarted(int previewWidtd, int previewHeight) {
		mFrameSize = previewWidtd * previewHeight;
		mRGBA = new int[mFrameSize];
		mRGBA2 = new int[mFrameSize];
		mBitmap = Bitmap.createBitmap(previewWidtd, previewHeight, Bitmap.Config.ARGB_8888);
		mBitmap2 = Bitmap.createBitmap(previewWidtd, previewHeight, Bitmap.Config.ARGB_8888);
	}

	/**
	 * This function called when camera stoped preview 
	 */
	@Override
	protected void onPreviewStopped() {
		if(mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
			
			mBitmap2.recycle();
			mBitmap2 = null;
		}
		mRGBA = null;
		mRGBA2 = null;
	}

	
	
	
	/**
	 * This function is called when surface of camera - application changed
	 * @param holder object of this surface
	 * @param int, format type
	 * @param int, width of this surface
	 * @param int, height of this surface
	 */
	@Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        
		if (HomeActivity.loaded == false) return;
		
		super.surfaceChanged(_holder, format, width, height);
                Log.d("LiveCamera", "surface changed");
                
                if (myMainActivity==null) return;
        mWarpedGraffiti=Bitmap.createBitmap(getFrameWidth(), getFrameHeight(), Bitmap.Config.ARGB_8888);
        
                Log.d("LiveCamera", "surface changed before synch");
        
        synchronized (this) {
            // initialize Mats before usage
	
            mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
            mGraySubmat = mYuv.submat(0, getFrameHeight(), 0, getFrameWidth());

            mRgba = new Mat();
            mWarpedImg = new Mat();
            mTrackOffset = new Mat(1,2,CvType.CV_32FC1);
            }
        
                Log.d("LiveCamera", "surface changed done");
    }
	
	// Added by Huanvb to test tessesart ocr
//	protected void onOCR() {
//		
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inSampleSize = 4;
//		
//		String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/exartisan_pics/";		
//		String _path = DATA_PATH + "/answer_number_code.jpg";
//
//		Bitmap bitmap = BitmapFactory.decodeFile(_path, options);
//
//		try {
//			ExifInterface exif = new ExifInterface(_path);
//			int exifOrientation = exif.getAttributeInt(
//					ExifInterface.TAG_ORIENTATION,
//					ExifInterface.ORIENTATION_NORMAL);
//
//			Log.v("LiveCamera", "Orient: " + exifOrientation);
//
//			int rotate = 0;
//
//			switch (exifOrientation) {
//			case ExifInterface.ORIENTATION_ROTATE_90:
//				rotate = 90;
//				break;
//			case ExifInterface.ORIENTATION_ROTATE_180:
//				rotate = 180;
//				break;
//			case ExifInterface.ORIENTATION_ROTATE_270:
//				rotate = 270;
//				break;
//			}
//
//			Log.v("LiveCamera", "Rotation: " + rotate);
//
//			if (rotate != 0) {
//
//				// Getting width & height of the given image.
//				int w = bitmap.getWidth();
//				int h = bitmap.getHeight();
//
//				// Setting pre rotate
//				Matrix mtx = new Matrix();
//				mtx.preRotate(rotate);
//
//				// Rotating Bitmap
//				bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
//			}
//
//			// Convert to ARGB_8888, required by tess
//			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//
//		} catch (IOException e) {
//			Log.e("LiveCamera", "Couldn't correct orientation: " + e.toString());
//		}
//
//		// _image.setImageBitmap( bitmap );
//		
//		Log.v("LiveCamera", "Before baseApi");
//		
//		String lang = "eng";
//
//		TessBaseAPI baseApi = new TessBaseAPI();
//		baseApi.setDebug(true);
//		baseApi.init(DATA_PATH, lang);
//		baseApi.setVariable("tessedit_char_whitelist", "0123456789");
//		baseApi.setImage(bitmap);
//		String recognizedText = baseApi.getUTF8Text();
//		baseApi.end();
//		
//		// You now have the text in recognizedText var, you can do anything with it.
//		// We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)
//		// so that garbage doesn't make it to the display.
//
//		Log.e("LiveCamera", "OCR called Huanvb =================>");
//		Log.v("LiveCamera", "OCRED TEXT: " + recognizedText);
//
//		if ( lang.equalsIgnoreCase("eng") ) {
//			recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
//		}
//		
//		
//		recognizedText = recognizedText.trim();
//				
////		if ( recognizedText.length() != 0 ) {
////			_field.setText(_field.getText().toString().length() == 0 ? recognizedText : _field.getText() + " " + recognizedText);
////			_field.setSelection(_field.getText().toString().length());
////		}
//		
//		// Cycle done.
//	}
	// end Huanvb
	
	boolean autofocus_running = false;
	/**
	 * This function is auto called in preview line of camera
	 * @param byte array of pixel input
	 * @return bitmap of camera view, at this time of line process
	 */
    @Override
    protected Bitmap processFrame(byte[] data) {
        int[] rgba = mRGBA;
        int[] rgba2 = mRGBA2;
        int mSnap = 0;
        boolean grayFlag = false;
        boolean ocrFlag = false;
        if (prefs!=null){
        	grayFlag = prefs.getBoolean("checkBoxGray", false);

        }

        
        mSnap = FindFeatures(getFrameWidth(), getFrameHeight(), data, rgba, rgba2, grayFlag, ocrFlag);
        
     // Added by Huanvb to test tessesart ocr
//        Log.e("LiveCamera", "FindFeatures called Huanvb =================>");//onOCR();
//        if (mSnap==2)
//        {	
//        	onOCR();
//        	Log.e("LiveCamera", "OCR called Huanvb =================>");
//        }

        Bitmap bmp = mBitmap; 
        mBitmap2.setPixels(rgba2, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
        
        
        if (mSnap==0) {
//        	Message msg
        	myMainActivity.uiHandler.post(new Runnable() 
            {
                public void run() 
                {
                	myMainActivity.unEnableShot();
                }
            });
        	
//        	runFocus();
        }
        
        if (mSnap == 3)
        {
        	Log.e("LiveCamera", "Run Focus Huanvb =================>");
        	if (!autofocus_running)
        	{
        		autofocus_running = true;
        		runFocus();
        	}
        }
        
        if (mSnap>0) {
        	myMainActivity.uiHandler.post(new Runnable() 
            {
                public void run() 
                {
                	myMainActivity.enableShot();
       
                }
            });
        	
        }
        
        return bmp;
    }

    
    /**
     * native function
     * @param width
     * @param height
     * @param yuv
     * @param rgba
     * @param rgba2
     * @param grayFlag
     * @return
     */
    public native int FindFeatures(int width, int height, byte yuv[], int[] rgba, int[] rgba2, boolean grayFlag, boolean ocrFlag);
    
    
    
    /**
     * Object callback when shoted one picture
     */
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] imageData, Camera c) {

			if (imageData != null) {
				//TODO - goi ham native xu ly, lay ve mot doi tuong Mat hoac anh luon
				Log.i("LiveCameraView", "===> lay dc du lieu chup anh, do dai: "+imageData.length);
				int[] rgba = mRGBA;
				int[] rgba2 = mRGBA2;
		        int mSnap =0;
		        boolean grayFlag = false;
		        boolean ocrFlag = false;
		        if (prefs!=null){
		        	grayFlag = prefs.getBoolean("checkBoxGray", false);

		        }

		        mSnap = FindFeatures(getFrameWidth(), getFrameHeight(), imageData, rgba, rgba2, grayFlag, ocrFlag);
				Bitmap bmp = mBitmap; 
		        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
		        mBitmap2.setPixels(rgba2, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());

			}
		}
	};
	
	/**
	 * Shot one photo. Simple, only get current frame of video line
	 * @return
	 */
	public Bitmap takePhoto() {
		mCamera.takePicture(null, mPictureCallback, mPictureCallback);
		mCamera.startPreview();
		return mBitmap2;
	}
    
    
	/**
	 * Set the working mode to Viewing mode - view a single live graffiti
	 * @param wallImage
	 * @param graffitiImage
	 */
    public void setModeToViewing(Bitmap wallImage, Bitmap graffitiImage)
    {
                Log.d("LiveCamera", "setModeToViewing");
        Bitmap tmpBitmap;

        // Save graffitiImage as a OpenCV Mat object
        tmpBitmap = graffitiImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(tmpBitmap, mWarpImg);
        if(!mWarpImg.empty())
                Imgproc.resize(mWarpImg, mWarpImg, new Size(getFrameWidth(), getFrameHeight()));
        
        // Get wallImage, convert to gray-scale and process for interest points
        tmpBitmap = wallImage.copy(Bitmap.Config.ARGB_8888, true);
        Mat wallImg = new Mat();
        Utils.bitmapToMat(tmpBitmap, wallImg);
        if(wallImg.empty())
                Log.d("LiveCamera", "Error - empty wall image");
        else
        {
                Imgproc.cvtColor(wallImg, wallImg, Imgproc.COLOR_BGRA2GRAY, 1);
                //ProcessRefFrame(wallImg.getNativeObjAddr());
        }
        tmpBitmap.recycle();
        //wallImg.dispose();
                
        mTrackingStatus = TRACKING_IDLE;
        mGoodMatch = false;
                offsetX = 0;
        offsetY = 0;
        offsetMatchX = 0;
        offsetMatchY = 0;
                
        mViewMode = VIEW_MODE_VIEWING;
    }
    
    
    /**
     * Set the working mode to Painting mode - get last frame and stop camera preview
     * @return
     */
    public Bitmap setModeToPainting()
    {
        Log.d("LiveCamera", "setModeToPainting");
        //mViewMode = VIEW_MODE_PAINTING;
        return mBitmap2;
    }
    
    /**
     * Get mode of camera
     * @return
     */
    public int getMode()
    {
        return mViewMode;
    }
    
    
    
    /**
     * 
     */
    boolean bIsAutoFocused;
    
    /**
     * The define of object callback of forcus action
     * @author Nguyen Quang Huy<nowayforback@gmail.com>
     *
     */
    private class AutoFocusCallBackImpl implements Camera.AutoFocusCallback {
        public void onAutoFocus(boolean success, Camera camera) {
            bIsAutoFocused = success; //update the flag used in onKeyDown()
            Log.i("LiveCameraView", "Inside autofocus callback. autofocused="+success);
            autofocus_running = false;
            //play the autofocus sound
            //MediaPlayer.create(myMainActivity, R.raw.spraying).start();
//            myMainActivity.sprayingSound();
        }
    }
    
    
}
