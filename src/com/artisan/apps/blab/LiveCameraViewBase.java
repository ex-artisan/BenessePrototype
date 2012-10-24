package com.artisan.apps.blab;

import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
//import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * 
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 *
 */
public abstract class LiveCameraViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	
	//TAG is used for debug
    private static final String TAG = "LiveCameraViewBase";

    //main camera
    protected Camera              mCamera;
    
    //surface holder
    private SurfaceHolder       mHolder;
    
    //canvas width, used for photo
    private int                 mFrameWidth;
    
    //canvas height, used for photo
    private int                 mFrameHeight;
    
    //pixel for current frame
    private byte[]              mFrame;
    
    
    private boolean             mThreadRun;
    private byte[]              mBuffer;


    /**
     * Constructor function
     * @param context
     * @param attrs
     */
	public LiveCameraViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isInEditMode()) {
			return;
		}
		mHolder = getHolder();
		mHolder.addCallback(this);
		Log.i(TAG, "Instantiated new " + this.getClass());
	}
    
    
	/**
	 * Constructor function
	 * @param context
	 */
    public LiveCameraViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    
    /**
     * This function is used for get frame width
     * @return
     */
    public int getFrameWidth() {
        return mFrameWidth;
    }

    
    /**
     * This function is used for get height of frame
     * @return int, height of camera canvas
     */
    public int getFrameHeight() {
        return mFrameHeight;
    }

	/**
	 * This function is called preview line of camera 
	 * @throws IOException
	 */
//	@TargetApi(11)
	public void setPreview() throws IOException {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//            mCamera.setPreviewTexture( new SurfaceTexture(10) );
//        else
        	mCamera.setPreviewDisplay(null);
	}

	/**
	 * This function is used for set camera to work status
	 * @return staus of camera
	 */
    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        releaseCamera();
        mCamera = Camera.open();
        if(mCamera == null) {
        	Log.e(TAG, "Can't open camera!");
        	return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (LiveCameraViewBase.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    LiveCameraViewBase.this.notify(); 
                }
                camera.addCallbackBuffer(mBuffer);
            }
        });
        return true;
    }
    
    /**
     * This function is used when need stop camera for photo
     */
    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        mThreadRun = false;
        synchronized (this) {
	        if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewStopped();
    }
    
    
    /**
     * set-up camera for work
     * @param width of camera
     * @param height of camera
     */
    public void setupCamera(int width, int height) {
        Log.i(TAG, "setupCamera");
        synchronized (this) {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                mFrameWidth = width;
                mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    int  minDiff = Integer.MAX_VALUE;
                    for (Camera.Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = size.width;
                            mFrameHeight = size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }

                params.setPreviewSize(getFrameWidth(), getFrameHeight());
                
                List<String> FocusModes = params.getSupportedFocusModes();

//                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
//                {
                	params.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
//                }            
                
                mCamera.setParameters(params);
                
                /* Now allocate the buffer */
                params = mCamera.getParameters();
                int size = params.getPreviewSize().width * params.getPreviewSize().height;
                size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
                mFrame = new byte [size];
                mCamera.addCallbackBuffer(mBuffer);

    			try {
    				setPreview();
    			} catch (IOException e) {
    				Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
    			}

                /* Notify that the preview is about to be started and deliver preview size */
                onPreviewStarted(params.getPreviewSize().width, params.getPreviewSize().height);

                /* Now we can start a preview */
                mCamera.startPreview();
            }
        }
    }
    
    /**
     * This event is called when surface of camera change status
     * @param holder object of this surface
     * @param int, format type
     * @param int, width of this surface
     * @param int, width of this surface
     */
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        setupCamera(width, height);
    }

    
    /**
     * This event is called when surface of camera created
     * @param holder object of this surface
     */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        (new Thread(this)).start();
    }

    
    /**
     * This event is called when surface of camera destroy
     * @param holder object of this surface
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }

    
    
    
    
    
    

    /* The bitmap returned by this method shall be owned by the child and released in onPreviewStopped() */
    protected abstract Bitmap processFrame(byte[] data);

    /**
     * This method is called when the preview process is being started. It is called before the first frame delivered and processFrame is called
     * It is called with the width and height parameters of the preview process. It can be used to prepare the data needed during the frame processing.
     * @param previewWidth - the width of the preview frames that will be delivered via processFrame
     * @param previewHeight - the height of the preview frames that will be delivered via processFrame
     */
    protected abstract void onPreviewStarted(int previewWidtd, int previewHeight);

    /**
     * This method is called when preview is stopped. When this method is called the preview stopped and all the processing of frames already completed.
     * If the Bitmap object returned via processFrame is cached - it is a good time to recycle it.
     * Any other resources used during the preview can be released.
     */
    protected abstract void onPreviewStopped();

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {
            Bitmap bmp = null;

            synchronized (this) {
                try {
                    this.wait();
                    bmp = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}