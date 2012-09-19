package org.sapmentors.sapnoteviewer.db;


import java.util.HashMap;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class SAPNoteProvider extends ContentProvider {

    public static final String KEY_TITLE = "title";
    public static final String KEY_NOTENR = "_id";

    private DatabaseHelper dbHelper;

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 3;
    
    /*Mapping to the fields the SearchManager needs in the cursor for quicksearch*/
    private static final HashMap<String, String> NOTE_PROJECTION_MAP;
    static {
    	NOTE_PROJECTION_MAP = new HashMap<String, String>();
    	NOTE_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
                KEY_TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
    	NOTE_PROJECTION_MAP.put(KEY_NOTENR,KEY_NOTENR);
    	NOTE_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                KEY_NOTENR + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
    }	    

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SAPNoteProvider() {
     //   this.mCtx = ctx;
    	dbHelper = new DatabaseHelper(getContext());
    }

	@Override
	public int delete(Uri uri, String s, String[] as) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.dir/vnd.sapmentors.sapnote";
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;

	}


	
    /**
     * Method called when the user has entered text into 
     * 
     */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(DATABASE_TABLE);
		//if search is empty add a wildcard, it has content add wildcard before and after
		if(selectionArgs!=null && selectionArgs[0].length()==0){
			selectionArgs[0] = "%";
		}else if (selectionArgs!=null && selectionArgs[0].length()>0){
			selectionArgs[0] = "%" +selectionArgs[0]+ "%";
		}
		//map from internal fields to fields SearchManager understands
		qBuilder.setProjectionMap(NOTE_PROJECTION_MAP);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		//String sql = qBuilder.buildQuery(projection, selection, selectionArgs, null, null,null,null);
        //do the query
		Cursor c = qBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		//c.setNotificationUri(getContext().getContentResolver(), uri);	
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s,
			String[] as) {
		// TODO Auto-generated method stub
		return 0;
	}

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
/*    public boolean updateNote(long rowId, String title, String body) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_BODY, body);

        return mDb.update(DATABASE_TABLE, args, KEY_NOTENR + "=" + rowId, null) > 0;
    }
    */
}
