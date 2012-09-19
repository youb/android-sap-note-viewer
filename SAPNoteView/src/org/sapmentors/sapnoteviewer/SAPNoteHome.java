package org.sapmentors.sapnoteviewer;

import org.sapmentors.sapnoteviewer.R;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

public class SAPNoteHome extends Activity {
	GoogleAnalyticsTracker tracker;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home);

		// anonymous tracker
		Analytics.trackPageView(this,"/home");
		Analytics.trackEvent(this,"System", "AndroidOS", Build.VERSION.RELEASE);
		Analytics.trackEvent(this,"System", "Device", Build.MODEL);  
		
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
		
		Button bSearch = (Button) findViewById(R.id.home_btn_search);
		bSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteSearch.class);
				startActivity(i);
			}
		});			
		
		Button bSetup = (Button) findViewById(R.id.home_btn_setup);
		bSetup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNotePreferences.class);
				startActivity(i);
			}
		});	
		
		ImageButton bTopSearch = (ImageButton) findViewById(R.id.title_search_button);
		bTopSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSearchRequested();
			}
		});
		
		// SAP Mentors
		ImageView bMentors = (ImageView) findViewById(R.id.footer_logo);
		bMentors.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://sapmentors.sap.com"));
				Analytics.trackPageView(SAPNoteHome.this,"/sapmentors");
				startActivity(i);
			}
		});	
		
		//evaluate if we will show changelog
		try {
			//current version
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			int versionCode = packageInfo.versionCode; 
			
			//version where changelog has been viewed
			SharedPreferences settings = getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
			int viewedChangelogVersion = settings.getInt(SAPNotePreferences.KEY_CHANGELOG_VERSION_VIEWED, 0);
			
			if(viewedChangelogVersion<versionCode) {
				Editor editor=settings.edit();
				editor.putInt(SAPNotePreferences.KEY_CHANGELOG_VERSION_VIEWED, versionCode);
				editor.commit();
				displayChangeLog();
			}
		} catch (NameNotFoundException e) {
			Log.w("Unable to get version code. Will not show changelog", e);
		}	
	}

	private void displayChangeLog(){
		
		//load some kind of a view
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.changelog_view, null);
	    
        new AlertDialog.Builder(this)
        .setTitle("Changelog")
        .setIcon(android.R.drawable.ic_menu_info_details)
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
              //
          }
        }).show();
        
	}

}