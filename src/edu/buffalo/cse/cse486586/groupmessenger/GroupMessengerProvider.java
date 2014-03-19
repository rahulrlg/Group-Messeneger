package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
	 	private static final String KEY_FIELD = "key";
	    private static final String VALUE_FIELD = "value";
	    private Context context;
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that I used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
    	String filename=(String)values.get(KEY_FIELD);
    	String fileContent=(String)values.get(VALUE_FIELD);
    	FileOutputStream outputstream;
    	//Log.e("ContentProvider", "Reached Provider");
    	try{
    		outputstream= getContext().openFileOutput(filename, Context.MODE_PRIVATE);
    		outputstream.write(fileContent.getBytes());
    		outputstream.close();
    		
    	}
    	catch(Exception e)
    	{
    		Log.v("insert", values.toString());
    	}
        
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
    	context=getContext();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         * 
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         * 
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
    	StringBuilder fileContent=new StringBuilder();
    	String line="";
    	FileInputStream inputStream;
    	String[] columnNames={KEY_FIELD,VALUE_FIELD};
    	String[] columnValues=new String[2];  	
    	MatrixCursor cursor=new MatrixCursor(columnNames);
    	try{
    		inputStream=context.openFileInput(selection);
    		BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
    		while((line=reader.readLine())!=null)
    		{
    			fileContent.append(line.trim());
    		}
    		columnValues[0]=selection;
    		columnValues[1]=fileContent.toString();
    		Log.d("QUERY", "->" + fileContent.toString() );
    		cursor.addRow(columnValues);
    		reader.close();
    	}
    	catch(Exception e)
    	{
    		 Log.e("query", e.getMessage() );
    	}
    	
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }
}
