package org.sapmentors.sapnoteview;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

		
	}


}