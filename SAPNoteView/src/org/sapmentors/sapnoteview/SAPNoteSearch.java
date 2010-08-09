package org.sapmentors.sapnoteview;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class SAPNoteSearch extends Activity {
	private final String SEARCH_PRIMARY_URL="https://service.sap.com/xsearch";
	private final String SEARCH_SECONDARY_URL="https://service.sap.com/notes";
	


	public static final String PREFS_NAME = "SAPNotePrefs";

	private WebView webview;

	private GoogleAnalyticsTracker tracker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// if no user is setup, redirect to setup
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sapuser = settings.getString("sapuser", null);
		if (sapuser == null) {
			Toast
					.makeText(
							SAPNoteSearch.this,
							"Please provide your SAP service marketplace user before continuing",
							Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, SAPNoteSetup.class);
			startActivity(i);
		}

		// needed in order to get progress bar
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		
		setContentView(R.layout.activity_search);		
		UIFrameworkSetup();
		
		//anonymous tracker
		tracker = GoogleAnalyticsTracker.getInstance();
	    tracker.start(Analytics.ANALYTICS_ID, 60,this);
	    tracker.trackPageView("/search");

		webview = (WebView) findViewById(R.id.webview);
		webview.setWebViewClient(new SAPNoteViewClient());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);
		//webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
		// progress bar in title
		webview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				SAPNoteSearch.this.setProgress(progress * 100);
			}
		});
		viewSearch(SEARCH_PRIMARY_URL);
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
		
		ImageButton bSearch = (ImageButton) thisActivity.findViewById(R.id.title_search_button);
		bSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(thisActivity, SAPNoteSearch.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				thisActivity.startActivity(i);
			}
		});
	}	
	private void viewSearch(String searchURL){
		
		webview.loadUrl(searchURL);
		Toast.makeText(SAPNoteSearch.this,
				"Loading search interface from " + searchURL + "\nIt may be a bit slow to load", Toast.LENGTH_LONG)
				.show();
	}




	/**
	 * Override method in order to avoid 
	 * webview reload on changed orientation.
	 * 
	 * Also requires modification in AndroidManifest.xml
	 * See http://stackoverflow.com/questions/1002085/android-webview-handling-orientation-changes
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Class which picks up events indicating that the user 
	 * has navigated to an SAP note
	 * 
	 * @author dapa
	 *
	 */
	private class SAPNoteViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i(this.getClass().getName(), "Search loading URL:" + url);
			//
			if (url != null && url.contains("/sap/support/notes/")) {
				return sendToNoteView(url);	
			} else {
				//allow current webview to handle
				return false;
			}

		}
		
		public void onLoadResource(WebView view, String url){
			Log.d(this.getClass().getName(), "onLoadResource:" + url);
			if (url != null && url.contains("/sap/support/notes/")) {
				//webview.goBack();
				sendToNoteView(url);
				
			}
			
		}
		
		private boolean sendToNoteView(String url){
			//user has clicked an SAP note.. Pass to view note activity
			try {
				//we get the last token of the url
				StringTokenizer tokenizer = new StringTokenizer(url,"/");
				String lastToken = null;
				while (tokenizer.hasMoreTokens()){
					lastToken = tokenizer.nextToken();
				}
				String strSAPNote = lastToken;
				long sapNoteNr = Long.parseLong(strSAPNote);
				Intent i1 = new Intent(getApplicationContext(), SAPNoteView.class);
				i1.putExtra(SAPNoteView.INTENT_EXTRA_KEY_ID, sapNoteNr);
				startActivity(i1);
				return true;
			}catch (Exception e) {
				Log.w(this.getClass().getName(), "Exception from search", e);
				return false;
			}	
		}



		/**
		 * Handle authentication request by trying the already stored sap
		 * username and password
		 */
		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {

			SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
			String sapuser = settings.getString(Preferences.KEY_SAP_USERNAME, null);
			String sappwd = settings.getString(Preferences.KEY_SAP_PASSWORD, null);

			assert (sapuser != null);

			handler.proceed(sapuser, sappwd);
			// super.onReceivedHttpAuthRequest(view, handler, host, realm);
		}

		/**
		 * If an error occurs during loading of URL
		 * 
		 */
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			Toast.makeText(SAPNoteSearch.this,
					"An error has occured: " + description, Toast.LENGTH_LONG)
					.show();
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_search, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		//always save settings
		switch (item.getItemId()) {
		case R.id.menuSearchPrimary:
			viewSearch(SEARCH_PRIMARY_URL);
			return true;
		case R.id.menuSearchSecondary:
			viewSearch(SEARCH_SECONDARY_URL);
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}	
	
	/**
	 * Handle the back key specifically
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
			webview.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	

}