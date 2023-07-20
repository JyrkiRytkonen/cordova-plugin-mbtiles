package com.ginasystem.plugins.mbtiles;

import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import java.io.File;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplification of com.makina.offline.mbtiles
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MBTilesActions
{
    static final String KEY_TILE_DATA = "tile_data";
	static final String KEY_SD_PATH_DATA = "sdCard_path";

    protected String mDirectory;
    protected Context mContext;
	private SQLiteDatabase db = null;

    public MBTilesActions(Context context, CordovaResourceApi resourceApi, String url, String name, boolean empty) throws SQLiteException {

        this.mContext = context;
        this.mDirectory = null;


        if (url == null || url.length() < 0) {
            url = "cdvfile://localhost/persistent/OfflineMaps/";
        }
		if(!empty)
		{
				

        Uri fileURL = resourceApi.remapUri(Uri.parse(url));
        mDirectory = fileURL.getPath() + "/";

 		//__cdvfile_$STORAGE__ is not a valid path. Need to parse the storage from the cdvfile
		// This plugin can't handle http(s)://localhost/__cdvfile_$STORAGE__ paths, so we need to map those to correct Android paths
        if(mDirectory.startsWith("/__cdvfile_")) {
            try {
                Pattern pattern = Pattern.compile("__cdvfile_(.*?)__\\/");
                Matcher matcher = pattern.matcher(mDirectory);
                if (matcher.find()) {
                    String storage = matcher.group(1);

                    if(!storage.isEmpty()) {
                        // This might indicate that file is saved into external sdcard, so we need to point to the sd card storage
                        if(storage.equals("root")) {
                            Pattern externalPattern = Pattern.compile("__cdvfile_root__\\/storage\\/.*\\/Android");
                            Matcher externalMatcher = externalPattern.matcher(mDirectory);
                            if (externalMatcher.find()) {
                                mDirectory = mDirectory.replaceAll("\\/__cdvfile_.*.__\\/", "/");
                            }
                        } else {
                            mDirectory = mDirectory.replaceAll("\\/__cdvfile_.*.__\\/", String.format("/%s/",storage));
                        }
                    }
                }
            }catch (Exception e) {
                Log.d(getClass().getName(), "Failed to replace cordova storage path : " + mDirectory + " ex=" + e.getMessage());
            }
        }

        if (getDirectory() != null) {
            String path = getDirectory() + name;
            try {
                this.db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
                Log.d(getClass().getName(), "openDatabase : " + this.db.getPath());
            } catch (SQLiteCantOpenDatabaseException e) {
                close();
                Log.e(getClass().getName(), "can't open database :" + e.getMessage());
            } catch (SQLiteException e) {
                Log.e(getClass().getName(), "openDatabase : " + this.db.getPath());
            }
        } else {
            close();
        }
				}
	}

    /**
     * return the Current Directory
     * @return directory working
     */
    protected String getDirectory() {

			 if(checkExternalStorageState())
			{
				return mDirectory;
			}
				return null;
		}

	public boolean isOpen()
	{
		return (this.db != null) && this.db.isOpen();
	}

	public void close()
	{
		if (isOpen())
		{
			Log.d(getClass().getName(), "close '" + db.getPath() + "'");
			
			this.db.close();
			this.db = null;
		}
	}

	protected JSONObject getExtDatabaseAbsoluteSdCardPath(boolean useSdCardRoot)
	{
		String path =  getDbAbsoluteSdCardPath(useSdCardRoot);
		JSONObject pathData = new JSONObject();
			try
			{
				pathData.put(KEY_SD_PATH_DATA,path);
				if(path != null)
				Log.d("SDpath", path);
			}
			catch (JSONException je)
			{
				Log.e(getClass().getName(), je.getMessage(), je);
			}
		return pathData;
	}

	protected String getDbAbsoluteSdCardPath(boolean useSdCardRoot)
	{
	if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
			{
				File[] externalCacheDirs = mContext.getExternalFilesDirs(null);
				for(File file: externalCacheDirs)
				{
					if(Environment.isExternalStorageRemovable(file))
					{
						// return  file.getAbsolutePath().split("/Android")[0] + "/";
						Log.w("cardPath777", file.getAbsolutePath());
					//	break;	
					if(useSdCardRoot)	
					{
						return file.getAbsolutePath().split("/Android")[0]+"/";//.split("/Android")[0] + "/";
					}
					else
					{
						return  file.getAbsolutePath().split("/files")[0]+"/";//.split("/Android")[0] + "/";	
					}			
					}
				}
			}
			return null;
	}

    /**
     * Retrieves the tile as <code>JSONObject</code> into a Base64 representation according to given parameters.
     * <p>
     * One key is <strong>required</strong> :
     * <p>
     * <ul>
     * <li>tile_data : the tile data into a Base64 representation</li>
     * </ul>
     * @param zoomLevel the current zoom level
     * @param column column index
     * @param row row index
     * @return the tile as <code>JSONObject</code>
     */
	public JSONObject getTile(int zoomLevel, int column, int row)
	{
		Log.d(getClass().getName(), "getTile [" + zoomLevel + ", " + column + ", " + row + "]");
		
		int currentZoomLevel = zoomLevel;
		
		Cursor cursor = db.query("tiles",
				new String[]{"tile_data"},
				"zoom_level = ? AND tile_column = ? AND tile_row = ?",
				new String[]{String.valueOf(currentZoomLevel), String.valueOf(column), String.valueOf(row)},
				null, null, null);
		
		JSONObject tileData = new JSONObject();
		
		// we should have only one result
		if (cursor.moveToFirst())
		{
			try
			{
                byte[] bytes = cursor.getBlob(cursor.getColumnIndex("tile_data"));
                if (bytes == null || bytes.length == 0)
                    return null;
				tileData.put(KEY_TILE_DATA, Base64.encodeToString(bytes, Base64.DEFAULT));
			}
			catch (JSONException je)
			{
				Log.e(getClass().getName(), je.getMessage(), je);
			}
		}
		
		cursor.close();
		
		return tileData;
	}

		protected boolean checkExternalStorageState()
		{
			String state = Environment.getExternalStorageState();		
			return  (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
		}

}
