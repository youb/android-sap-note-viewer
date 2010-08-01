package org.sapmentors.sapnoteview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class SAPNoteHome extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home);

		final Activity thisActivity=this;
		
		// setup button
		Button bView = (Button) findViewById(R.id.home_btn_notes);
		bView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteView.class);
				startActivity(i);
			}
		});	
		
		Button bFavorites = (Button) findViewById(R.id.home_btn_starred);
		bFavorites.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteList.class);
				startActivity(i);
			}
		});	
		
		Button bSetup = (Button) findViewById(R.id.home_btn_vendors);
		bSetup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteSetup.class);
				startActivity(i);
			}
		});			
		
	}
	
	public void sayHello(ImageButton b)	{
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_setup, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuView:
			Intent i = new Intent(this, SAPNoteView.class);
			startActivity(i);
			return true;
		case R.id.menuFavorites:
			Intent i2 = new Intent(this, SAPNoteList.class);
			startActivity(i2);
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}	

}