package com.artisan.apps.blab;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Common function of this application
 * @author Nguyen Quang Huy<nowayforback@gmail.com>
 *
 */
public class Common {

	//Dropbox key of exartisan@gmx.com account
	final static public String APP_KEY = "0mzyefcz6hvdowv"; //"sssv7bmbf3k8nc4";
	final static public String APP_SECRET = "bxs7zurc5n7vpeq"; //"nbakeicx5s5jhow";
	
    final static public AccessType ACCESS_TYPE = AccessType.DROPBOX;
    
    final static public String ACCOUNT_PREFS_NAME = "prefs";
    final static public String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static public String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    
    final static public String PHOTO_DIR = "/Photos/";
    
    public static boolean mLoggedInDropbox = false;
    
    public static DropboxAPI<AndroidAuthSession> mApi = null;
    
    
    public static File fileUp = null;
    public static boolean flagInUpload = false;
    
    
	
    /**
     * this function used for send file to eReceipt server
     * @param c
     * @param pathToOurFile
     * @throws Exception
     */
	public static void sendFile(Activity c, String pathToOurFile) throws Exception {
		
		Log.i("LiveCamera","==>vao den sendFile");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		String rawUrl = prefs.getString("serverurl", "http://192.168.1.43:8000/eReceipt/default/uploadFile");
		String rawUploadUser = prefs.getString("username", "user03");
		
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;

		//String pathToOurFile = getFilesDir() + "/pics.png";
		String urlServer = rawUrl + "?userName=" + rawUploadUser; //"http://192.168.1.43:8000/eReceipt/default/uploadFile?userName=user03";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1*1024*1024;

		try
		{
		FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );

		URL url = new URL(urlServer);
		Log.i("LiveCamera","==>vao den sendFile1111 url = "+urlServer);
		connection = (HttpURLConnection) url.openConnection();
		Log.i("LiveCamera","==>vao den sendFile2222");
		// Allow Inputs & Outputs
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		//connection.setConnectTimeout(30);
		
		connection.setReadTimeout(30000);
		
		// Enable POST method
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
		
		connection.setRequestProperty("userName", rawUploadUser);

		Log.i("LiveCamera","==>vao den sendFile3333");
		
		outputStream = new DataOutputStream( connection.getOutputStream() );
		
		
		Log.i("LiveCamera","==>vao den sendFile444");
		
		
		outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + pathToOurFile +"\"" + lineEnd);
		outputStream.writeBytes(lineEnd);

		bytesAvailable = fileInputStream.available();
		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		buffer = new byte[bufferSize];

		// Read file
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		
		Log.i("LiveCamera","==>vao den giua sendFile, doc duoc "+bytesRead + " bytes" );

		//outputStream.wait(80000);
		while (bytesRead > 0)
		{
		outputStream.write(buffer, 0, bufferSize);
		bytesAvailable = fileInputStream.available();
		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		}

		outputStream.writeBytes(lineEnd);
		outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		// Responses from the server (code and message)
		int serverResponseCode = connection.getResponseCode();
		String serverResponseMessage = connection.getResponseMessage();
		
		Log.i("LiveCamera","===>file da upload len la: "+serverResponseMessage+" , ma ket qua la: "+serverResponseCode);

		fileInputStream.close();
		outputStream.flush();
		outputStream.close();
		}
		catch (Exception ex)
		{
			Log.e("LiveCamera", " ===> catch duoc loi: "+ex.getMessage());
			throw ex;
		}
	}
	
		
	
	
	static long secondInMillis = 1000;
	static long minuteInMillis = secondInMillis * 60;
	static long hourInMillis = minuteInMillis * 60;
	static long dayInMillis = hourInMillis * 24;
	

	/**
	 * Delete one folder on external store
	 * @param dir
	 * @return
	 */
	public static boolean deleteFolder(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteFolder(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    } 
	
	
	public static boolean testVersion(Context context) {
		
		return true;

	}

	
	/**
	 * create a new photo file on store card
	 * @param subFolder
	 * @param ext
	 * @return
	 */
	public static File getNewFile(File subFolder, String ext) {
		int idx = 1;
		File file;
		do {
			file = new File(subFolder.getAbsolutePath()
					+ String.format("/pics_%d"+ext, idx++));
		} while (file.exists());
		return file;
	}
	
	
	/**
	 * Copy one file from internal to external card
	 * @param context
	 * @param internalFileName
	 * @param target
	 * @return
	 */
	public static boolean saveInternalFileToExternalStorage(Context context,
			String internalFileName, File target) {
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {

			fis = new FileInputStream(internalFileName);
			fos = new FileOutputStream(target);
			int BUFF_SIZE = 1024;
			byte[] buff = new byte[BUFF_SIZE];
			int byteCount = 0;
			while ((byteCount = fis.read(buff, 0, BUFF_SIZE)) > 0) {
				fos.write(buff, 0, byteCount);

			}
			return true;
		} catch (FileNotFoundException e) {
			Common.manageException(e, context);
		} catch (IOException e) {
			Common.manageException(e, context);
		} finally {
			try {
				if (fos != null)
					fos.close();
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				Common.manageException(e, context);
			}
		}
		return false;
	}

	
	/**
	 * Test and get/create new folder on external card
	 * @param context
	 * @param subFolderName
	 * @return
	 */
	public static File getExternalFolder(Context context, String subFolderName) {
		File folder = getExternalFolder(context);
		if (folder == null)
			return null;

		File subFolder = new File(folder.getAbsolutePath() + "/"
				+ subFolderName);
		if (!subFolder.exists())
			subFolder.mkdir();
		return subFolder;
	}

	/**
	 * Get external folder
	 * @param context
	 * @return
	 */
	private static File getExternalFolder(Context context) {
		if (!Common.isExternalStorageWritable()) {
			Common.doMessageDialog(context, "ERROR","No SDCard");
			return null;
		}
		return Environment.getExternalStorageDirectory();
	}
	
	/**
	 * Create and show a meassage dialog
	 * @param context
	 * @param title
	 * @param message
	 */
	public static void doMessageDialog(Context context, String title,
			String message) {
		doMessageDialog(context, title, message, null);
	}
	
	/**
	 * Create and show a message dialog
	 * @param context
	 * @param title
	 * @param message
	 * @param onClick
	 */
	public static void doMessageDialog(Context context, String title,
			String message, OnClickListener onClick) {
		Thread contextThread = context. getMainLooper(). getThread();
		if (contextThread != Thread.currentThread())
			return;
		
		AlertDialog dialog = new AlertDialog.Builder(context).setTitle(title)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, onClick).create();
		dialog.show();

	}

	
	/**
	 * Get bitmap from file
	 * @param context
	 * @param file
	 * @return
	 */
	public static Bitmap getBitmap(Context context, String file) {
		FileInputStream fis;
		Bitmap bmp = null;
		try {
			fis = context.openFileInput(file);
			bmp = BitmapFactory.decodeStream(fis);
			fis.close();

		} catch (FileNotFoundException e) {
			manageException(e, context);
		} catch (IOException e) {
			manageException(e, context);
		}
		return bmp;
	}

	
	/**
	 * Get data from file
	 * @param context
	 * @param file
	 * @return
	 */
	public static byte[] getData(Context context, String file) {
		FileInputStream fis;
		int BUFF_SIZE = 1024;

		try {
			fis = context.openFileInput(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buff = new byte[BUFF_SIZE];
			int byteCount = 0;
			while ((byteCount = fis.read(buff, 0, BUFF_SIZE)) > 0) {
				out.write(buff, 0, byteCount);
			}
			fis.close();
			return out.toByteArray();
		} catch (FileNotFoundException e) {
			manageException(e, context);
		} catch (IOException e) {
			manageException(e, context);
		}
		return new byte[0];
	}

	/**
	 * get time
	 * @param duration
	 * @return
	 */
	public static String getElapsedTime(long duration) {

		long elapsedDays = duration / dayInMillis;
		duration = duration % dayInMillis;

		long elapsedHours = duration / hourInMillis;
		duration = duration % hourInMillis;

		long elapsedMinutes = duration / minuteInMillis;
		duration = duration % minuteInMillis;

		long elapsedSeconds = duration / secondInMillis;

		if (elapsedDays == 0 && elapsedHours == 0)
			return String.format("%dm - %ds", elapsedMinutes, elapsedSeconds);
		if (elapsedDays == 0)
			return String.format("%dh - %dm - %ds", elapsedHours,
					elapsedMinutes, elapsedSeconds);

		return String.format("%d - %dh - %dm - %ds", elapsedDays, elapsedHours,
				elapsedMinutes, elapsedSeconds);
	}

	
	/**
	 * Check status of write rule
	 * @return
	 */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();

		return Environment.MEDIA_MOUNTED.equals(state);
	}

	/**
	 * wrap for exception
	 * @param e
	 * @param context
	 */
	public static void manageException(Exception e, Context context) {
		doMessageDialog(context, "ERROR",
				e.getLocalizedMessage());

	}

	

	/**
	 * Copy a file
	 * @param in
	 * @param out
	 * @throws Exception
	 */
	public static void copyFile(File in, File out) throws Exception {
		FileOutputStream fos = new FileOutputStream(out);
		copyFile(in, fos);
		fos.close();
	}
	
	/**
	 * Copy a file
	 * @param in
	 * @param fos
	 * @throws Exception
	 */
	public static void copyFile(File in, FileOutputStream fos) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		try {
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * Delete a file
	 * @param context
	 * @param file
	 */
	public static void safeDeleteFile(Context context, String file) {
		try {
			context.deleteFile(file);
		} catch (Exception e) {
		}

	}
	
	
	
	
	
	
	public static boolean homeKeyPressed;
    private static boolean justLaunched = true;

    /**
     * Check if the app was just launched. If the app was just launched then
     * assume that the HOME key will be pressed next unless a navigation event
     * by the user or the app occurs. Otherwise the user or the app navigated to
     * the activity so the HOME key was not pressed.
     */
    public static void checkJustLaunced() {
        if (justLaunched) {
            homeKeyPressed = true;
            justLaunched = false;
        } else {
            homeKeyPressed = false;
        }
    }

    /**
     * Check if the HOME key was pressed. If the HOME key was pressed then the
     * app will be killed either safely or quickly. Otherwise the user or the
     * app is navigating away from the activity so assume that the HOME key will
     * be pressed next unless a navigation event by the user or the app occurs.
     * 
     * @param killSafely
     *            Primitive boolean which indicates whether the app should be
     *            killed safely or quickly when the HOME key is pressed.
     * 
     * @see {@link UIHelper.killApp}
     */
    public static void checkHomeKeyPressed(boolean killSafely) {
        if (homeKeyPressed) {
            killApp(true);
        } else {
            homeKeyPressed = true;
        }
    }

    /**
     * Kill the app either safely or quickly. The app is killed safely by
     * killing the virtual machine that the app runs in after finalizing all
     * {@link Object}s created by the app. The app is killed quickly by abruptly
     * killing the process that the virtual machine that runs the app runs in
     * without finalizing all {@link Object}s created by the app. Whether the
     * app is killed safely or quickly the app will be completely created as a
     * new app in a new virtual machine running in a new process if the user
     * starts the app again.
     * 
     * <P>
     * <B>NOTE:</B> The app will not be killed until all of its threads have
     * closed if it is killed safely.
     * </P>
     * 
     * <P>
     * <B>NOTE:</B> All threads running under the process will be abruptly
     * killed when the app is killed quickly. This can lead to various issues
     * related to threading. For example, if one of those threads was making
     * multiple related changes to the database, then it may have committed some
     * of those changes but not all of those changes when it was abruptly
     * killed.
     * </P>
     * 
     * @param killSafely
     *            Primitive boolean which indicates whether the app should be
     *            killed safely or quickly. If true then the app will be killed
     *            safely. Otherwise it will be killed quickly.
     */
    public static void killApp(boolean killSafely) {
        if (killSafely) {
            /*
             * Notify the system to finalize and collect all objects of the app
             * on exit so that the virtual machine running the app can be killed
             * by the system without causing issues. NOTE: If this is set to
             * true then the virtual machine will not be killed until all of its
             * threads have closed.
             */
            System.runFinalizersOnExit(true);

            /*
             * Force the system to close the app down completely instead of
             * retaining it in the background. The virtual machine that runs the
             * app will be killed. The app will be completely created as a new
             * app in a new virtual machine running in a new process if the user
             * starts the app again.
             */
            System.exit(0);
        } else {
            /*
             * Alternatively the process that runs the virtual machine could be
             * abruptly killed. This is the quickest way to remove the app from
             * the device but it could cause problems since resources will not
             * be finalized first. For example, all threads running under the
             * process will be abruptly killed when the process is abruptly
             * killed. If one of those threads was making multiple related
             * changes to the database, then it may have committed some of those
             * changes but not all of those changes when it was abruptly killed.
             */
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }
	
	
	
}
