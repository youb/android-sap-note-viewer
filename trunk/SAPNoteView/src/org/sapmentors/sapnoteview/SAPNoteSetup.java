package org.sapmentors.sapnoteview;


import com.google.android.apps.analytics.GoogleAnalyticsTracker;

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
import android.widget.TextView;
import android.widget.Toast;

public class SAPNoteSetup extends Activity {
	private EditText txtUsername;
	private EditText txtPassword;
	private GoogleAnalyticsTracker tracker;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_setup);
		UIFrameworkSetup();
		//anonymous tracker
		tracker = GoogleAnalyticsTracker.getInstance();
	    tracker.start(Analytics.ANALYTICS_ID, 60,this);
	    tracker.trackPageView("/setup");
	
		// set up view
		txtUsername = (EditText) findViewById(R.id.txtUsername);
		txtPassword = (EditText) findViewById(R.id.txtPassword);


		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
		String sapuser = settings.getString(Preferences.KEY_SAP_USERNAME, null);
		if(sapuser!=null){
			txtUsername.setText(sapuser);
		}
		String sappwd = settings.getString(Preferences.KEY_SAP_PASSWORD, null);
		if(sappwd!=null){
			txtPassword.setText(sappwd);
		}
	}

	private void UIFrameworkSetup(){
		((TextView) findViewById(R.id.title_text)).setText(getTitle());
		
		final Activity thisActivity = this;
		ImageButton bHome = (ImageButton) this.findViewById(R.id.title_home_button);
		bHome.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				saveSettings();
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
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_setup, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		//always save settings
		switch (item.getItemId()) {
		case R.id.menuSave:
			saveSettings();
			Intent i = new Intent(this, SAPNoteHome.class);
			startActivity(i);
			return true;
		case R.id.menuClear:
			clearSettings();
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}	
	
	private void saveSettings(){
		String strUsername = ((Editable) txtUsername.getText()).toString();
		String strPassword = ((Editable) txtPassword.getText()).toString();
		
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Preferences.KEY_SAP_USERNAME, strUsername);
		editor.putString(Preferences.KEY_SAP_PASSWORD, strPassword);
		editor.commit();
		Toast.makeText(SAPNoteSetup.this, "Saved settings",
				Toast.LENGTH_SHORT).show();	
	}
	
	private void clearSettings(){
		SharedPreferences settings = getSharedPreferences(Preferences.KEY_SAP_USERNAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(Preferences.KEY_SAP_USERNAME);
		editor.remove(Preferences.KEY_SAP_PASSWORD);
		editor.commit();
		
		txtUsername.setText("");
		txtPassword.setText("");
		
		Toast.makeText(SAPNoteSetup.this, "Cleared settings",
				Toast.LENGTH_SHORT).show();	
	}
	


}