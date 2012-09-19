package org.sapmentors.sapnoteviewer;

import org.sapmentors.sapnoteviewer.R;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UIFrameworkUtils {

	/**
	 * Register event listeners for framework elements. 
	 * Needed since xml onClick for elements not part of android 1.5
	 * @param activity
	 */
	public static void registerFrameworkElements(final Activity activity){
		registerHomeButton(activity);
		registerSearchButton(activity);
	}
	
	private static void registerHomeButton(final Activity activity){
		// setup button
		Button bView = (Button) activity.findViewById(R.id.title_home_button);
		bView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(activity, SAPNoteView.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(i);
			}
		});
	}
	
	private static void registerSearchButton(final Activity activity){
		// setup button
		Button bView = (Button) activity.findViewById(R.id.title_search_button);
		bView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(activity, SAPNoteView.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(i);
			}
		});
	}
	
	
	                                        
	
}
