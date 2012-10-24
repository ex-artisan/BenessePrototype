package com.artisan.apps.blab;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * HomeActivity home screen
 * This is main activity, run first of this app.
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 */
public class HomeActivity extends Activity { 


	//adapter for gallery view
	public static PicAdapter imgAdapt;
	//gallery object
	public static Gallery picGallery;
	//image view for larger display
	private ImageView picView;
	
	//button object, refenrence to new photo feature
	private Button btnNewPhoto;
	
	//button object object, reference to send file selected to server - dropbox
	private Button btnSendFile;
	
	//button object, 
	private Button mSubmit;
	
	//button object
	private Button mDelete;
	
	//flag for load OpenCV lib
	public static boolean loaded = false;
	
	private MenuItem            mItemSetting;
	
	final String folderPath = Environment.getExternalStorageDirectory().getPath() + "/exartisan_pics/";//"/sdcard/exartisan_pics/";
	final String digitZip = "digit.zip"; // Image templates of numbers
	
	File folder = new File(folderPath);
    String[] allFiles;
	
    
    final String TAG  = "HomeActivity";
    
    
    
    DropboxAPI<AndroidAuthSession> mApi;
    int currentPos = -1;
    private boolean mLoggedIn;

    /**
     * 
     * @param srcFile
     * @param des
     */
    private void unzip(String src, String dest, boolean isDel){
    	  
    	  final int BUFFER_SIZE = 4096;
    	  
    	  BufferedOutputStream bufferedOutputStream = null;
    	     FileInputStream fileInputStream;
    	     try {
    	      fileInputStream = new FileInputStream(src);
    	      ZipInputStream zipInputStream 
    	       = new ZipInputStream(new BufferedInputStream(fileInputStream));
    	      ZipEntry zipEntry;
    	      
    	      while ((zipEntry = zipInputStream.getNextEntry()) != null){
    	       
    	       String zipEntryName = zipEntry.getName();
    	       File file = new File(dest + zipEntryName);
    	       
    	       if (file.exists()){
    	        
    	       } else {
    	        if(zipEntry.isDirectory()){
    	         file.mkdirs(); 
    	        }else{
    	         byte buffer[] = new byte[BUFFER_SIZE];
    	         FileOutputStream fileOutputStream = new FileOutputStream(file);
    	         bufferedOutputStream 
    	          = new BufferedOutputStream(fileOutputStream, BUFFER_SIZE);
    	         int count;

    	         while ((count 
    	          = zipInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
    	          bufferedOutputStream.write(buffer, 0, count);
    	         }

    	         bufferedOutputStream.flush();
    	         bufferedOutputStream.close(); 
    	        }
    	       } 
    	      }
    	      zipInputStream.close();
    	      Log.d(TAG,"Unziped");
    	      //Try to delete zip file
    	      if (isDel) {
    	    	  File f = new File(src);
    	    	  f.delete();
    	      }
    	     } catch (FileNotFoundException e) {
    	      // TODO Auto-generated catch block
    	      e.printStackTrace();
    	     }catch (IOException e) {
    	      // TODO Auto-generated catch block
    	      e.printStackTrace();
    	     }
    	 }
    
    /**
     * Copy Create folder if not exists;
     * @param path
     */
    private void createFolderIfNotExists(String path) {
    	try{
    		File f = new File(path);
    		if ( !f.exists() ) {
    			f.mkdirs();
    		}
    	}catch(Exception ex) {
    		ex.printStackTrace();
    	}
    }
    
    /**
     * Copy file from assets folder
     * @param srcFile
     * @param desPath
     * @return
     */
    private boolean copyFileFromAssests(String srcFile, String desPath) {
    	
    	try{
    		createFolderIfNotExists(folderPath);
    		
    		String desFilename = srcFile.substring(srcFile.lastIndexOf("/")+1);
    		Log.d(TAG, "Zip file name " + desFilename);
    		AssetManager am = getAssets();
    		InputStream is = am.open(srcFile);
    		File destFile = new File(folderPath + desFilename);
    		
    		FileOutputStream fos = new FileOutputStream(destFile);
    		byte[] buf = new byte[1024];
    		
    		int rlen = 0;
    		
    		while ((rlen = is.read(buf)) > 0) {
    			fos.write(buf,0, rlen);
    		}
    		fos.close();
    		return true;
    	}catch(Exception ex) {
    		Log.d(TAG,"Copied " + srcFile + "failed");
    		return false;
    	}
    }
    /**
     * This object be called when OpenCV library finish load 
     */
    private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
    	@Override
    	public void onManagerConnected(int status) {
    		
    		Log.i(TAG, "===>callback");
    		
    		switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
					
					// Load native library after(!) OpenCV initialization
					System.loadLibrary("Benesse");
					
					loaded  = true;
					
					// Create and set View
					//mView = new LiveCameraView(mAppContext);
					
					
					/*mView.setOnTouchListener(new OnTouchListener() {
						
						public boolean onTouch(View v, MotionEvent event) {
							
							Bitmap liveImg = mView.setModeToPainting();
							playSound(SOUND_ID_SHUTTER);
							if (liveImg==null  || liveImg.getWidth()==0) {
								Log.i(TAG, "====> ko lay dc anh");
								return false;
							}
							
							
							
							// Compress the wall image into a ByteArray in PNG format
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            liveImg.compress(Bitmap.CompressFormat.PNG, 90, bos);
                            
                            String filename = getFilesDir() + "/pics.png";
                            FileOutputStream fos = null;
                            try {
                                    fos = new FileOutputStream(filename);
                            } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                            }
                            liveImg.compress(Bitmap.CompressFormat.PNG, 90, fos);
							
                            Common.sendFile(filename);
                            
							return false;
						}
					});*/
					
					
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
    	}
	};
    
	
    /**
     * instantiate the interactive main activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	//call superclass method and set main content view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home2);
        
        
        if (folder!=null) allFiles = folder.list();
        
        //Copy digit.zip to exartisan_pics folder
        if (copyFileFromAssests("digit.zip", folderPath)) {
        	unzip(folderPath + digitZip, folderPath, true);
        }
        // Added by Huanvb for create database for ocr
        // eng.traineddata file with the app (in assets folder)
//        copyFileFromAssests("eng.traineddata",folderPath);
        
        //get the large image view
        picView = (ImageView) findViewById(R.id.picture);
        
        //get the gallery view
        picGallery = (Gallery) findViewById(R.id.gallery);
        
        //create a new adapter
        imgAdapt = new PicAdapter(this);
        //set the gallery adapter
        picGallery.setAdapter(imgAdapt);
        

        //set the click listener for each item in the thumbnail gallery
        picGallery.setOnItemClickListener(new OnItemClickListener() {
        	//handle clicks
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	//set the larger image view to display the chosen bitmap calling method of adapter class
                picView.setImageBitmap(imgAdapt.getPic(position));
                currentPos = position;
            }
        });
        
        
        btnNewPhoto = (Button) findViewById(R.id.btnGetNew);
        if (btnNewPhoto!=null)
        	btnNewPhoto.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					// TODO Auto-generated method stub
					Intent myIntent = new Intent(v.getContext(), LiveCameraNative.class);
					startActivity(myIntent);
				}
        		
        	});
        
        
        mSubmit = (Button)findViewById(R.id.btnAuth);

        mSubmit.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // This logs you out if you're logged in, or vice versa
                if (mLoggedIn) {
                    logOut();
                } else {
                    // Start the remote authentication
                    mApi.getSession().startAuthentication(HomeActivity.this);
                }
            }
        });
        
        
        
        mDelete = (Button)findViewById(R.id.btnDel);

        mDelete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
            	AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            	builder.setMessage("Are you sure you want to delete this file?")
            	       .setCancelable(false)
            	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   if (currentPos<0) dialog.cancel();
           						File file = new File (folderPath + allFiles[currentPos]);
            	        	   boolean deleted = file.delete();
            	        	   imgAdapt.refress();
            	        	   dialog.cancel();
            	           }
            	       })
            	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();
            	alert.show();
            	
            }
        });
        
        
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
        
        checkAppKeySetup();
        
        btnSendFile = (Button) findViewById(R.id.btnSend);
        if (btnSendFile!=null)
        	btnSendFile.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					if (currentPos<0) return;
					File file = new File (folderPath + allFiles[currentPos]);
					UploadPicture upload = new UploadPicture(HomeActivity.this, mApi, Common.PHOTO_DIR, file);
                    upload.execute();
				}
        		
        	});
        
        
        setLoggedIn(mApi.getSession().isLinked());
        
//        if (OpenCVLoader.initDebug()) 
//        {
//        	System.loadLibrary("opencv_java");
//			System.loadLibrary("Benesse");
//        } 
//        else
//        {
//            // Report initialization error
//        	Log.e(TAG, "Cannot init static OpenCV library!");
//        }
        
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
        	Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
               
    }
    
        
    public void galeryReset() {
    	if (picGallery==null) return;
    	try {
    	if (picGallery.getAdapter()!=null) ((BaseAdapter) picGallery.getAdapter()).notifyDataSetChanged();
    	}catch (ClassCastException e) {
    		Log.e(TAG, "runtime exception:" + e.getMessage());
    	}
    }
    
    
    /**
     * This function is called when re run (re active) this activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }
    
    
    /**
     * Base Adapter subclass creates Gallery view
     * - provides method to return bitmaps from sdcard
     *
     */
    public class PicAdapter extends BaseAdapter {
    	
    	//use the default gallery background image
        int defaultItemBackground;
        
        //gallery context
        private Context galleryContext;

        //placeholder bitmap for empty spaces in gallery
        Bitmap placeholder;

        /**
         * Constructor
         * @param c
         */
        public PicAdapter(Context c) {
        	
        	//instantiate context
        	galleryContext = c;

            //decode the placeholder image
            placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
            
            
            //get the styling attributes - use default Andorid system resources
            TypedArray styleAttrs = galleryContext.obtainStyledAttributes(R.styleable.PicGallery);
            //get the background resource
            defaultItemBackground = styleAttrs.getResourceId(
            		R.styleable.PicGallery_android_galleryItemBackground, 0);
            //recycle attributes
            styleAttrs.recycle();
        }

        
        /**
         * This function for refress image source for this adapter
         */
        public void refress() {
        	folder = new File(folderPath);
            if (folder != null) allFiles = folder.list();
            HomeActivity.this.galeryReset();
        }
        
        
        //BaseAdapter methods
        
        /**
         * return number of data items i.e. bitmap images
         * @return int, count number of images source
         */
        public int getCount() {
        	if (allFiles==null) return 0;
            return allFiles.length;
        }

        /**
         * return item at specified position
         * @param int, position of item
         * @return object at input position
         */
        public Object getItem(int position) {
            return position;
        }

        //return item ID at specified position
        public long getItemId(int position) {
            return position;
        }

        /**
         * get view specifies layout and display options for each thumbnail in the gallery
         * @param int, position get view
         * @param view object get from cache, conver to new view object
         * @param parent view of object needed get
         * @return view object
         */
        public View getView(int position, View convertView, ViewGroup parent) {
        	
        	//create the view
            ImageView imageView = new ImageView(galleryContext);
            //specify the bitmap at this position in the array
//            imageView.setImageBitmap(imageBitmaps[position]);
            Bitmap bitmapImage = BitmapFactory.decodeFile(folder + "/"
                    + allFiles[position]);
            BitmapDrawable drawableImage = new BitmapDrawable(bitmapImage);
            imageView.setImageDrawable(drawableImage);
            
            
            //set layout options
            imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
            //scale type within view area
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //set default gallery item background
            imageView.setBackgroundResource(defaultItemBackground);
            //return the view
            return imageView;
        }
        
 
        /**
         * This function is used for get picture from position
         * @param position need get picture
         * @return bitmap at specified position for larger display
         */
        public Bitmap getPic(int position)
        {
        	//return bitmap at posn index
        	Bitmap bitmapImage = BitmapFactory.decodeFile(folder + "/"
                    + allFiles[position]);
            BitmapDrawable drawableImage = new BitmapDrawable(bitmapImage);
            
        	return drawableImage.getBitmap();
        }
    }
    
  
    
    

    /**
     * This function is called when this activity create menu 
     * @param menu object for add
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        mItemSetting = menu.add("Settings");     
        return true;
    }

    
    /**
     * This event is called when user click in a menu item
     * @param menuitem clicked
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemSetting) {
        	Log.i(TAG, "Menu Item selected is setting---<");
        	
        	Intent intent = new Intent(HomeActivity.this,
    		      PrefsActivity.class);
    		      startActivity(intent);
    		} 
        return true;
    }
    
    
    
    
    /** Functions for Dropbox upload **/
    
    /**
     * This function is used for check setting status of Dropbox api on this application
     */
    private void checkAppKeySetup() {
        // Check to make sure that we have a valid app key
        if (Common.APP_KEY.startsWith("CHANGE") ||
                Common.APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            finish();
            return;
        }

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + Common.APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
            finish();
        }
    }

    /**
     * This function is used for show a short message on screen in short time
     * @param msg for show
     * @param time for show
     */
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @return Array of [access_key, access_secret], or null if none stored
     */
    private String[] getKeys() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key = prefs.getString(Common.ACCESS_KEY_NAME, null);
        String secret = prefs.getString(Common.ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     * @param String, key of dropbox application
     * @param String, secret string of dropbox application
     */
    private void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(Common.ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(Common.ACCESS_KEY_NAME, key);
        edit.putString(Common.ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    /**
     * This function is used for clear login status of account dropbox on this application 
     */
    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(Common.ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    /**
     * This function is used to create a new session for the agent of this application, use with dropbox account 
     * @return return session of the agent of this application
     */
    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(Common.APP_KEY, Common.APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, Common.ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, Common.ACCESS_TYPE);
        }

        return session;
    }
    
    
    /**
     * This function is called when need logout for the dropbox agent 
     */
    private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     * @param boolean flag for login status
     */
    private void setLoggedIn(boolean loggedIn) {
    	mLoggedIn = loggedIn;
    	if (loggedIn) {
    		mSubmit.setText("Unlink from Dropbox");
    	} else {
    		mSubmit.setText("Link with Dropbox");
    	}
    }
    
    
    
    
    public void onDestroy() {
        super.onDestroy();

        /*
         * Kill application when the root activity is killed.
         */
        Common.killApp(true);
    }


    
    

}
