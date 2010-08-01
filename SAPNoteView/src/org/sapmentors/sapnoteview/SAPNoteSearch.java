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
		viewSearch();
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
	private void viewSearch(){
		viewNoteInternal("https://websmp204.sap-ag.de/xsearch");
	}

	private void viewNoteInternal(String strURL) {
		// give a short message to user
		Toast.makeText(SAPNoteSearch.this, "Loading " + strURL,Toast.LENGTH_LONG).show();
		DefaultHttpClient httpclient=null;
		try {
	        httpclient = new DefaultHttpClient();
	
	        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String sapuser = settings.getString("sapuser", null);
			String sappwd = settings.getString("sappwd", null);
	        
	        httpclient.getCredentialsProvider().setCredentials(
	                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
	                new UsernamePasswordCredentials(sapuser, sappwd));
	 
	        CustomRedirectHandler redirectHandler =  new CustomRedirectHandler();
	        httpclient.setRedirectHandler(redirectHandler);
	        
	        HttpGet httpget = new HttpGet(strURL);
	        
	        
	        HttpResponse response = httpclient.execute(httpget);
	        HttpEntity entity = response.getEntity();
	        String contentType= entity.getContentType().getValue();
	        Log.d(this.getClass().getName(), "Response has contentType "+contentType);
	        
	        //handle different content types
	        if(contentType==null || !contentType.startsWith("text")){
	        	Toast.makeText(SAPNoteSearch.this, "Downloads are not supported in current version. Content-type:" + contentType,Toast.LENGTH_LONG).show();
	        	return;
	        }
	        
	        InputStream is =entity.getContent();
	        String htmlOrg = convertStreamToString(is);
	        
	        
	        
	        //background-color tricks seems to work in chrome, but not on android
	        //htmlOrg = htmlOrg.replaceAll("\\<div id=\"oc_1\".*?\\>","<div id=\"oc_1\" ct=\"SC\" class=\"urScrl\" style=\"background-color:#FFFFFF;\" >");
	        //replace for knowledge base articles
	        //htmlOrg = htmlOrg.replaceAll("\\<div id=\"Display_Container\".*?\\>","<div id=\"Display_Container\" ct=\"SC\" class=\"urScrl\" style=\"background-color:#FFFFFF;\" >");
	        
	        
	        
	        String baseURL = "https://" + redirectHandler.getLastServerHost();
	        webview.loadDataWithBaseURL(baseURL, htmlOrg, "text/html", "utf-8", null);
	        //webview.loadDataWithBaseURL("https://service.sap.com", htmlOrg, "text/html", "utf-8", null);
	        
	        if (entity != null) {
	            entity.consumeContent();
	        }
		}catch (Exception e){
			Toast.makeText(SAPNoteSearch.this, "Httpclient failed, attempting backup solution",Toast.LENGTH_LONG).show();
			webview.loadUrl(strURL);
			Log.e(this.getClass().getName(),"Error during httpclient",e);
		}finally{
		       // When HttpClient instance is no longer needed, 
	        // shut down the connection manager to ensure
	        // immediate deallocation of all system resources
	        if(httpclient!=null){
	        	httpclient.getConnectionManager().shutdown();
	        }
		}

	}
	


	
	private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
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

	private class SAPNoteViewClient extends WebViewClient {
		private boolean bHaveTriedManual=false;
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i(this.getClass().getName(), "Loading URL:" + url);
			//
			if (url != null && url.contains("display_pdf")) {
				return false;
			} else {
				//trying to make sure links to notes are loaded through our code
				//this so that we can fix the scroll problems and get the note title
				if(bHaveTriedManual==false){
					bHaveTriedManual=true;
					viewNoteInternal(url);
					return true;
				}
				
				view.loadUrl(url);
				return true;
			}

		}
		
		public void onLoadResource(WebView view, String url){
			String s=null;
			
		}
		
		

		@Override
		public void onPageFinished(WebView view, String url) {
			int contentHeight= webview.getContentHeight();
			super.onPageFinished(view, url);
			bHaveTriedManual=false;
		}



		/**
		 * Handle authentication request by trying the already stored sap
		 * username and password
		 */
		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String sapuser = settings.getString("sapuser", null);
			String sappwd = settings.getString("sappwd", null);

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
			bHaveTriedManual=false;
			Toast.makeText(SAPNoteSearch.this,
					"An error has occured: " + description, Toast.LENGTH_LONG)
					.show();
		}

	}
	
	private class CustomRedirectHandler extends DefaultRedirectHandler {
		//default is service.sap.com
		String lastServerHost="service.sap.com";
		
		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context)
				throws ProtocolException {
		
			//get the location header to find out where to redirect to
	        Header locationHeader = response.getFirstHeader("location");
	        String locationValue= locationHeader.getValue();
	        URL url;
			try {
				url = new URL(locationValue);
				lastServerHost=url.getHost();
			} catch (MalformedURLException e) {
				//Ignore
				//e.printStackTrace();
			}
	        
	      			
			return super.getLocationURI(response, context);
		}
		
		public String getLastServerHost(){
			return lastServerHost;
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