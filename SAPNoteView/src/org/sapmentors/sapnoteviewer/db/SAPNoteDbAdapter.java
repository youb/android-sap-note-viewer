package org.sapmentors.sapnoteviewer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
public class SAPNoteDbAdapter {

    public static final String KEY_TITLE = "title";
    public static final String KEY_NOTENR = "_id";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table notes (_id integer primary key , "
                    + "title text not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 3;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //Do not delete database unless changes have been made
        	//Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
            //        + newVersion + ", which will destroy all old data");
            //db.execSQL("DROP TABLE IF EXISTS notes");
            //onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SAPNoteDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    private SAPNoteDbAdapter openDB() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    private void closeDB() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createNote(long noteNr, String title) {
        openDB();
    	ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_NOTENR, noteNr);
        long ret= mDb.insert(DATABASE_TABLE, null, initialValues);
        closeDB();
        return ret;
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param noteNr id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(long noteNr) {
    	openDB();
    	boolean bRet = mDb.delete(DATABASE_TABLE, KEY_NOTENR + "=" + noteNr, null) > 0;
    	closeDB();
    	return bRet;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllNotes() {
    	openDB();
        Cursor c= mDb.query(DATABASE_TABLE, new String[] {KEY_NOTENR, KEY_TITLE}, null, null, null, null, null);
      //cursors handle the closing themselves closeDB();
        return c;
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchNote(long noteNr) throws SQLException {
    	openDB();
        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_NOTENR,
                        KEY_TITLE,}, KEY_NOTENR + "=" + noteNr, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        
        //cursors handle the closing themselves closeDB();
        return mCursor;

    }
    
    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public String fetchTitleForNote(long noteNr) throws SQLException {
    	openDB();
        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_NOTENR,
                        KEY_TITLE,}, KEY_NOTENR + "=" + noteNr, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            String title = mCursor.getString(mCursor.getColumnIndex(KEY_TITLE));
            return title;
        }else {
        	return null;
        }
        
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
