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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class SAPNoteSetup extends Activity {
	public static final String PREFS_NAME = "SAPNotePrefs";
	private EditText txtUsername;
	private EditText txtPassword;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setup);

		// set up view
		txtUsername = (EditText) findViewById(R.id.txtUsername);
		txtPassword = (EditText) findViewById(R.id.txtPassword);


		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sapuser = settings.getString("sapuser", null);
		if(sapuser!=null){
			txtUsername.setText(sapuser);
		}
		String sappwd = settings.getString("sappwd", null);
		if(sappwd!=null){
			txtPassword.setText(sappwd);
		}
		

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
		case R.id.menuView:
			saveSettings();
			Intent i = new Intent(this, SAPNoteView.class);
			startActivity(i);
			return true;
		case R.id.menuFavorites:
			saveSettings();
			Intent i2 = new Intent(this, SAPNoteList.class);
			startActivity(i2);
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
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sapuser", strUsername);
		editor.putString("sappwd", strPassword);
		editor.commit();
		Toast.makeText(SAPNoteSetup.this, "Saved settings",
				Toast.LENGTH_SHORT).show();	
	}
	
	private void clearSettings(){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("sapuser");
		editor.remove("sappwd");
		editor.commit();
		
		txtUsername.setText("");
		txtPassword.setText("");
		
		Toast.makeText(SAPNoteSetup.this, "Cleared settings",
				Toast.LENGTH_SHORT).show();	
	}
	


}