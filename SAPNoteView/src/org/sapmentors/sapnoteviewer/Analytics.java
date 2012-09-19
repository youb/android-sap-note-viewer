package org.sapmentors.sapnoteviewer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Analytics {
	public final static String ANALYTICS_ID="UA-17758208-1";
	private static GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
	private static boolean isStarted=false;
	
	public static void trackPageView(Context context, String pageName){
		if(isEnabled(context)){
			if(!isStarted){
				tracker.start(Analytics.ANALYTICS_ID, 15, context);
				isStarted=true;
			}
			tracker.trackPageView(pageName);
		}
	}
	
	public static void trackEvent (Context context, String category, String name, String value){
		if(isEnabled(context)){
			if(!isStarted){
				tracker.start(Analytics.ANALYTICS_ID, 15,context);
				isStarted=true;
			}
			//analytics don't like whitespace
			name = name.replaceAll(" ", "");
			value= value.replaceAll(" ","");
			tracker.trackEvent(category, name, value,0 );
		}
		
	}
	
	public static boolean isEnabled(Context context){
		SharedPreferences settings = context.getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
		return settings.getBoolean(SAPNotePreferences.KEY_DO_ANALYTICS, false);
	}
}
