package org.sapmentors.sapnoteviewer;

import org.sapmentors.sapnoteviewer.R;
import org.sapmentors.sapnoteviewer.db.SAPNoteDbAdapter;
import org.sapmentors.sapnoteviewer.db.SAPNoteProvider;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SAPNoteList extends ListActivity {
	

	private static final int ACTION_VIEW = Menu.FIRST;
	private static final int ACTION_SHARE= Menu.FIRST+1;
	private static final int ACTION_DELETE = Menu.FIRST + 2;
	

	private SAPNoteDbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorites);
		UIFrameworkSetup();
		
		//anonymous tracker
		Analytics.trackPageView(this,"/favorites");
		
		
		mDbHelper = new SAPNoteDbAdapter(this);
		//mDbHelper.open();
		fillData();

		registerForContextMenu(getListView());
	}

	private void UIFrameworkSetup(){
		((TextView) findViewById(R.id.title_text)).setText(getTitle());
		
		final Activity thisActivity = this;
		ImageButton bHome = (ImageButton) this.findViewById(R.id.title_home_button);
		bHome.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteHome.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				thisActivity.startActivity(i);
			}
		});
		
		ImageButton bTopSearch = (ImageButton) findViewById(R.id.title_search_button);
		bTopSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSearchRequested();
			}
		});
	}	
	
	private void fillData() {

		Cursor notesCursor = mDbHelper.fetchAllNotes();
		startManagingCursor(notesCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { SAPNoteProvider.KEY_TITLE };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.text1 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.content_favorites_row, notesCursor, from, to);

		setListAdapter(notes);

	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ACTION_VIEW, 0, R.string.contextMenuView);
		menu.add(0, ACTION_SHARE, 0, R.string.contextMenuShare);
		menu.add(0, ACTION_DELETE, 0, R.string.contextMenuDelete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case ACTION_VIEW:
			Intent i1 = new Intent(this, SAPNoteView.class);
			i1.putExtra(SAPNoteView.INTENT_EXTRA_KEY_ID, info.id);
			startActivity(i1);
			return true;
		case  ACTION_SHARE:
			String strNote2 = info.id + "";
			String strNoteTitle = mDbHelper.fetchTitleForNote(info.id);
			String shareTxt= "http://service.sap.com/sap/support/notes/" + strNote2 + " " + strNoteTitle;
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, shareTxt);
			Analytics.trackEvent(this,"Note", "Shared", strNote2);
			
			/* Send it off to the Activity-Chooser */  
			startActivity(Intent.createChooser(shareIntent, "Share note.."));  
			return true;			
		case ACTION_DELETE:
			deleteNote(info.id);

			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void deleteNote(long id) {
		boolean bDeleted = mDbHelper.deleteNote(id);

		if (bDeleted){
		Toast.makeText(SAPNoteList.this,
				"Deleted note " + id + " from favorites", Toast.LENGTH_LONG)
				.show();
		} else{
			Toast.makeText(SAPNoteList.this,
					"Failed to delete note " + id + " from favorites", Toast.LENGTH_LONG)
					.show();			
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, SAPNoteView.class);
		i.putExtra(SAPNoteView.INTENT_EXTRA_KEY_ID, id);
		startActivity(i);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();
	}
}
