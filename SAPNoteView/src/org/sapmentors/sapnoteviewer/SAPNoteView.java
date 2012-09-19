package org.sapmentors.sapnoteviewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;
import org.sapmentors.sapnoteviewer.R;
import org.sapmentors.sapnoteviewer.db.SAPNoteDbAdapter;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SAPNoteView extends Activity {
	public static final String INTENT_EXTRA_KEY_ID = "SAPNoteNr";

	private WebView webview;
	private Button bView;
	private EditText txtNote;

	private boolean bAttemptToSniffNoteFromHTTP = false;
	private String strNoteTitle = null;

	private SAPNoteDbAdapter mDbHelper;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bAttemptToSniffNoteFromHTTP = false;

		// if no user is setup, redirect to setup
		SharedPreferences settings = getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
		String sapuser = settings.getString(SAPNotePreferences.KEY_SAP_USERNAME, null);
		if (sapuser == null) {
			Toast
					.makeText(
							SAPNoteView.this,
							"Please provide your SAP service marketplace user before continuing",
							Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, SAPNotePreferences.class);
			startActivity(i);
		}

		// open database of favorites
		mDbHelper = new SAPNoteDbAdapter(this);
		// mDbHelper.open();


		setContentView(R.layout.activity_view);
		UIFrameworkSetup();

		// anonymous tracker
		Analytics.trackPageView(this,"/view");

		// set up view
		txtNote = (EditText) findViewById(R.id.txtNote);
		webview = (WebView) findViewById(R.id.webview);

		webview.setWebViewClient(new SAPNoteViewClient());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);

		webview.setDownloadListener(new DownloadHandler());
		
        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                    if(progress==100){
                    	updateLoading(false);
                    }else {
                    	updateLoading(true);
                    }
            }
        });
		

		// View note button
		bView = (Button) findViewById(R.id.bView);
		bView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bAttemptToSniffNoteFromHTTP = false;
				String strNote = ((Editable) txtNote.getText()).toString();
				viewNote(strNote);
			}
		});
		

		//Check if this intent was called with parameters
		Intent intent = getIntent();
		String dataString = intent.getDataString();

		Uri url = intent.getData();
		Bundle extras = intent.getExtras();

		// check if we got an intent parameter from our application
		if (extras != null && extras.containsKey(INTENT_EXTRA_KEY_ID)) {
			long sapNoteNr = extras.getLong(INTENT_EXTRA_KEY_ID);
			txtNote.setText(sapNoteNr + "");
			viewNote(sapNoteNr + "");
			// hack to check if the dataString is pure numeric
		} else if (dataString != null) {
			try {
				long sapNoteNr = Long.parseLong(dataString);
				txtNote.setText(sapNoteNr + "");
				viewNote(sapNoteNr + "");
			} catch (NumberFormatException e) {
				// if it is not a number from the quick search it is a url
				// check if we got called through our url-based intent-filter
				// for example from chrome to phone
				// TODO: Test if chrome to phone works
				if (url != null) {
					String noteNumber = url.getQueryParameter("numm");
					if (noteNumber != null) {
						txtNote.setText(noteNumber);
						viewNote(noteNumber);
					} else {
						bAttemptToSniffNoteFromHTTP = true;
						viewNoteFromURL(url.toString());
					}
				}
			}
		}

	}
	/**
	 * Setup the UI framework consisting of Home and search button
	 */
	private void UIFrameworkSetup() {
		((TextView) findViewById(R.id.title_text)).setText(getTitle());

		final Activity thisActivity = this;
		ImageButton bHome = (ImageButton) this
				.findViewById(R.id.title_home_button);
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

	/**
	 * Primary method used for viewing a note
	 * 
	 * @param strNote The notenumber as a string
	 */
	private void viewNote(String strNote) {
		String strURL = "https://service.sap.com/sap/support/notes/" + strNote;
		Analytics.trackEvent(this,"Note", "View", strNote);
		viewNoteFromURL(strURL);
	}

	/**
	 * Normally this method should not be called directly, but only via
	 * viewNote. But for some notes we will sniff the note number after the html
	 * is downloaded
	 * 
	 * @param strURL
	 */
	private void viewNoteFromURL(String strURL) {
		hideKeyboard();
		updateLoading(true);
		Toast
				.makeText(SAPNoteView.this, "Loading " + strURL,
						Toast.LENGTH_LONG).show();
		//Create Async task for loading the URL
		//This task will also update the UI once finished
		new DownloadNoteTask().execute(strURL);

	}
	
	private void downloadPDF(String strNote){
		updateLoading(true);
		new DownloadFileTask().execute(strNote);
	}
	
	
	private void updateLoading(boolean isLoading){
		ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.title_loading);
		if(isLoading){
			loadingIndicator.setVisibility(View.VISIBLE);
		}else {
			loadingIndicator.setVisibility(View.GONE);
		}
		
	}
//
	private class DownloadNoteTask extends AsyncTask<String, String, String> {
		CustomRedirectHandler redirectHandler;
		String downloadUrl;
		private int responseCode;
		
		protected String doInBackground(String... urls) {
			redirectHandler = new CustomRedirectHandler();
			downloadUrl=urls[0];
			return downloadNoteInternal(downloadUrl);
		}

		/**
		 * Progress messages to be displayed in UI thread
		 * but originting from asynctask 
		 */
		protected void onProgressUpdate(String... status) {
			Toast.makeText(SAPNoteView.this, status[0],
					Toast.LENGTH_LONG).show();
		}

		/**
		 * The main async method.
		 * Needed since we need to adjust the HTML 
		 * and sniff out the note name
		 * 
		 * @param strURL
		 * @return
		 */
		private String downloadNoteInternal(String strURL) {

			DefaultHttpClient httpclient = null;
			try {
				httpclient = new DefaultHttpClient();

				SharedPreferences settings = getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
				String sapuser = settings.getString(SAPNotePreferences.KEY_SAP_USERNAME, null);
				String sappwd = settings.getString(SAPNotePreferences.KEY_SAP_PASSWORD, null);

				//setup authentication
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
						new UsernamePasswordCredentials(sapuser, sappwd));

				//we store redirect in order to get javascript domain problem sorted
				httpclient.setRedirectHandler(redirectHandler);

				HttpGet httpget = new HttpGet(strURL);

				//Do the actual http call
				HttpResponse response = httpclient.execute(httpget);
				
				responseCode = response.getStatusLine().getStatusCode();
				
				HttpEntity entity = response.getEntity();
				String contentType = entity.getContentType().getValue();
				Log.d(this.getClass().getName(), "Response code " + responseCode + "Response has contentType "
						+ contentType);
				
				if(responseCode==401){
					//we don't report this as the backup solution will provide the message
					//publishProgress(getString(R.string.HTTPAuthenticationFailed));
					
					return null;
				}


				// handle different content types
				if (contentType == null || !contentType.startsWith("text")) {
					publishProgress("Downloads are not supported in current version. Content-type:"
									+ contentType);
					return null;
				}

				BasicResponseHandler responseHandler= new BasicResponseHandler();
				String htmlOrg = responseHandler.handleResponse(response);
	
				if (entity != null) {
					entity.consumeContent();
				}
				return htmlOrg;
				
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "Error during httpclient", e);
				return null;
			} finally {
				// When HttpClient instance is no longer needed,
				// shut down the connection manager to ensure
				// immediate deallocation of all system resources
				if (httpclient != null) {
					httpclient.getConnectionManager().shutdown();
				}
			}

		}
		
		
		protected void onPostExecute(String htmlOrg) {
			
			
			if(htmlOrg==null || htmlOrg.equals("")){
				if (responseCode==401){
					//we've attempted authentication but it has failed
					Toast.makeText(SAPNoteView.this, getString(R.string.HTTPAuthenticationFailed),
							Toast.LENGTH_LONG).show();
					Intent i = new Intent(SAPNoteView.this, SAPNotePreferences.class);
					startActivity(i);
					return;
				}else {
					Toast.makeText(SAPNoteView.this,
							"Problems with download. Attempting backup solution",
							Toast.LENGTH_LONG).show();
					webview.loadUrl(downloadUrl);
					return;
				}
			}
			publishProgress("Note loaded, now adjusting HTML to fix problems");
			// if we followed a link, we need to sniff the note number from
			// the html
			if (bAttemptToSniffNoteFromHTTP) {
				String sapNoteNr = getNoteNrFromString(htmlOrg);
				if (sapNoteNr != null) {
					txtNote.setText(sapNoteNr);
				} else {
					Toast
							.makeText(
									SAPNoteView.this,
									"Could not find SAP Note number from content. Save to favorites will not be possible",
									Toast.LENGTH_LONG).show();
				}

			}

			// try to read the name of the note
			strNoteTitle = getNoteTitleFromString(htmlOrg);
			if (strNoteTitle == null) {
				strNoteTitle = "Note "
						+ ((Editable) txtNote.getText()).toString();
			}
			strNoteTitle.replaceAll("&amp;", "&");

			// background-color tricks seems to work in chrome, but not on
			// android
			htmlOrg = htmlOrg
					.replaceAll(
							"\\<div id=\"oc_1\".*?\\>",
							"<div id=\"oc_1\" ct=\"SC\" class=\"urScrl\" style=\"background-color:#FFFFFF;\" >");
			
			String baseURL = "https://"+ redirectHandler.getLastServerHost();
			
			Log.d(this.getClass().getName(), "HTML adjusted. Base url is "+baseURL);
			webview.loadDataWithBaseURL(baseURL, htmlOrg, "text/html",
					"utf-8", null);

		}
	}

	
	private class DownloadFileTask extends AsyncTask<String, String, File> {
		private int responseCode;
		private String downloadDirectory;
		
		protected File doInBackground(String... noteNr) {
			String strNote=noteNr[0];
			return downloadFileInternal(strNote);
		}

		/**
		 * Progress messages to be displayed in UI thread
		 * but originating from asynctask 
		 */
		protected void onProgressUpdate(String... status) {
			Toast.makeText(SAPNoteView.this, status[0],
					Toast.LENGTH_LONG).show();
		}

		/**
		 * The main async method.
		 * Needed since we need to adjust the HTML 
		 * and sniff out the note name
		 * 
		 * @param strURL
		 * @return
		 */
		private File downloadFileInternal(String strNote) {

			DefaultHttpClient httpclient = null;
			try {
				String url = "https://service.sap.com/sap/bc/bsp/spn/no_display_pdf/sapnote.htm?numm="+strNote+"&vers=0000000000&language=E&sap-language=E";
				httpclient = new DefaultHttpClient();

				SharedPreferences settings = getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
				String sapuser = settings.getString(SAPNotePreferences.KEY_SAP_USERNAME, null);
				String sappwd = settings.getString(SAPNotePreferences.KEY_SAP_PASSWORD, null);
				downloadDirectory = settings.getString(SAPNotePreferences.KEY_PDF_DOWNLOAD_FOLDER, null);

				//setup authentication
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
						new UsernamePasswordCredentials(sapuser, sappwd));

				HttpGet httpget = new HttpGet(url);
			   
				//Do the actual http call
				HttpResponse response = httpclient.execute(httpget);
				
				responseCode = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
				String contentType = entity.getContentType().getValue();
				
				Log.d(this.getClass().getName(), "Response code " + responseCode + " Response has contentType "
						+ contentType);
				
				if(responseCode==401){
					//we don't report this as the backup solution will provide the message
					return null;
				}

				InputStream in = entity.getContent();
				
				
				File dir = new File(downloadDirectory);
				
				if(!dir.exists()){
					dir.mkdir();
				}
				
				File file = new File(downloadDirectory,strNote+".pdf");

				
				FileOutputStream fOut = new FileOutputStream(file);
	
				
				publishProgress("Downloading pdf for note " + strNote + " to "+ file.getCanonicalPath()); 
				
				byte[] buffer = new byte[1024];
				int len1 = 0;
				while ( (len1 = in.read(buffer)) != -1 ) {
					fOut.write(buffer,0, len1);
				}
				
				if (entity != null) {
					entity.consumeContent();
				}
				return file;
				
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "Error during httpclient", e);
				publishProgress("Got exception when downloading to "+ downloadDirectory + " " + e.getMessage() + "\nPlease check dowload folder settings"); 
				
				return null;
			} finally {
				// When HttpClient instance is no longer needed,
				// shut down the connection manager to ensure
				// immediate deallocation of all system resources
				if (httpclient != null) {
					httpclient.getConnectionManager().shutdown();
				}
			}

		}
		
		
		protected void onPostExecute(File file) {
			updateLoading(false);
			if(file!=null && file.exists()){
				Uri path = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                try {
                    startActivity(intent);
                } 
                catch (ActivityNotFoundException e) {
                    Toast.makeText(SAPNoteView.this, 
                        "No Application Available to View PDF", 
                        Toast.LENGTH_SHORT).show();
                }
				
				
			}else {
                Toast.makeText(SAPNoteView.this, 
                        "Download of PDF failed", 
                        Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void hideKeyboard() {
		// close soft keyboard
		InputMethodManager inputManager = (InputMethodManager) SAPNoteView.this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(txtNote.getWindowToken(), 0);

		/*
		 * inputManager.hideSoftInputFromWindow(txtNote.getWindowToken(),
		 * InputMethodManager.HIDE);
		 */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_view, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAdd:
			String strNote = ((Editable) txtNote.getText()).toString();
			addNoteToFavorites(strNote);
			Analytics.trackEvent(this,"Note", "Favorite", strNote);
			
			return true;
		case R.id.menuShare:
			String strNote2 = ((Editable) txtNote.getText()).toString();
			String shareTxt = "http://service.sap.com/sap/support/notes/"
					+ strNote2 + " " + strNoteTitle;
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, shareTxt);

			/* Send it off to the Activity-Chooser */
			startActivity(Intent.createChooser(shareIntent, "Share note.."));
			Analytics.trackEvent(this,"Note", "Shared", strNote2);
			return true;
		case R.id.menuDownloadPdf:
			String strNote3 = ((Editable) txtNote.getText()).toString();
			Analytics.trackEvent(this,"Note", "DownloadPDF", strNote3);
			downloadPDF(strNote3);
			
			return true;	
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}

	private String getNoteNrFromString(String html) {
		// find the text in the title element
		Pattern p = Pattern.compile("(?s)<title>(.*)</title>");
		Matcher m = p.matcher(html);
		if (m.find()) {
			String title = m.group(1).trim();
			StringTokenizer tokenizer = new StringTokenizer(title, " ");
			if (tokenizer.countTokens() >= 2) {
				tokenizer.nextToken();
				String noteNr = tokenizer.nextToken();
				// System.out.println(noteNr);
				return noteNr;
			}
		}
		return null;
	}

	private String getNoteTitleFromString(String html) {
		// find the text in the title element
		Pattern p = Pattern.compile("<span id=header_data .+?>(.+?)</span>");
		Matcher m = p.matcher(html);
		if (m.find()) {
			String title = m.group(1).trim();
			if (title.equals("")) {
				return null;
			} else {
				return title;
			}

		}
		return null;
	}


	/**
	 * Override method in order to avoid webview reload on changed orientation.
	 * 
	 * Also requires modification in AndroidManifest.xml See
	 * http://stackoverflow
	 * .com/questions/1002085/android-webview-handling-orientation-changes
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	private void addNoteToFavorites(String strNote) {
		try {
			long lngNote = Long.parseLong(strNote);
			// strNoteTitle is set during load
			long lngRet = mDbHelper.createNote(lngNote, strNoteTitle);
			if (lngRet != -1) {
				Toast.makeText(SAPNoteView.this,
						"Added note " + lngNote + " to favorites",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(SAPNoteView.this,
						"Note " + lngNote + " is already a favorites",
						Toast.LENGTH_SHORT).show();
			}

		} catch (NumberFormatException e) {
			Toast
					.makeText(
							SAPNoteView.this,
							"Text field must contain a note number before it is added to favorites",
							Toast.LENGTH_LONG).show();
		}

	}

	private class SAPNoteViewClient extends WebViewClient {
		private boolean bHaveTriedManual = false;

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i(this.getClass().getName(), "Loading URL:" + url);
			updateLoading(true);
			if (url != null && url.contains("display_pdf")) {
				String strNote = ((Editable) txtNote.getText()).toString();
				downloadPDF(strNote);
				return true;
			} else {
				// trying to make sure links to notes are loaded through our
				// code
				// this so that we can fix the scroll problems and get the note
				// title
				if (bHaveTriedManual == false) {
					Log.i(this.getClass().getName(), "Will try to sniff note number from the HTML content");
					bHaveTriedManual = true;
					bAttemptToSniffNoteFromHTTP = true;
					viewNoteFromURL(url);
					//TODO: This could actually allow the webview to load the url and only parse the title
					//but currently we overwrite
					
					return true;
				}
				return false;
			}

		}

		public void onLoadResource(WebView view, String url) {
			Log.d(this.getClass().getName(), "onLoadResource:" + url);
			updateLoading(true);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Log.d(this.getClass().getName(), "onPageFinished:" + url);
			super.onPageFinished(view, url);
			bHaveTriedManual = false;
			updateLoading(false);
		}

		/**
		 * Handle authentication request by trying the already stored sap
		 * username and password
		 */
		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {

			Log.d(this.getClass().getName(), "onReceivedHttpAuthRequest:" + host);
			SharedPreferences settings = getSharedPreferences(SAPNotePreferences.PREFS_NAME, 0);
			String sapuser = settings.getString(SAPNotePreferences.KEY_SAP_USERNAME, null);
			String sappwd = settings.getString(SAPNotePreferences.KEY_SAP_PASSWORD, null);

			handler.proceed(sapuser, sappwd);
			
		}

		/**
		 * If an error occurs during loading of URL
		 * 
		 */
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			Log.w(this.getClass().getName(), "onReceivedError:" +errorCode + " "+ failingUrl + "\n"+description);
			bHaveTriedManual = false;
			Toast.makeText(SAPNoteView.this,
					"An error has occured: " + description, Toast.LENGTH_LONG)
					.show();		
			updateLoading(false);
		}

	}

	private class CustomRedirectHandler extends DefaultRedirectHandler {
		// default is service.sap.com
		String lastServerHost = "service.sap.com";

		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context)
				throws ProtocolException {

			// get the location header to find out where to redirect to
			Header locationHeader = response.getFirstHeader("location");
			String locationValue = locationHeader.getValue();
			URL url;
			try {
				url = new URL(locationValue);
				lastServerHost = url.getHost();
			} catch (MalformedURLException e) {
				// Ignore
				// e.printStackTrace();
			}

			return super.getLocationURI(response, context);
		}

		public String getLastServerHost() {
			return lastServerHost;
		}

	}

	/**
	 * Code taken from android browser
	 * http://android.git.kernel.org/?p=platform/
	 * packages/apps/Browser.git;a=tree
	 * ;f=src/com/android/browser;h=a9a204e54222376c5b5159023d06cb7238c738de
	 * ;hb=HEAD
	 * 
	 * @author dagfinn.parnas
	 * 
	 */
	private class DownloadHandler implements DownloadListener {

		/**
		 * Notify the host application a download should be done, or that the
		 * data should be streamed if a streaming viewer is available.
		 * 
		 * @param url
		 *            The full url to the content that should be downloaded
		 * @param contentDisposition
		 *            Content-disposition http header, if present.
		 * @param mimetype
		 *            The mimetype of the content reported by the server
		 * @param contentLength
		 *            The file size reported by the server
		 */
		public void onDownloadStart(String url, String userAgent,
				String contentDisposition, String mimetype, long contentLength) {
			Log.i(this.getClass().getName(), "Download to URL:" + url);
			// if we're dealing wih A/V content that's not explicitly marked
			// for download, check if it's streamable.
			if (contentDisposition == null
					|| !contentDisposition.regionMatches(true, 0, "attachment",
							0, 10)) {
				// query the package manager to see if there's a registered
				// handler
				// that matches.
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(url), mimetype);
				ResolveInfo info = getPackageManager().resolveActivity(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
				if (info != null) {
					ComponentName myName = getComponentName();
					// If we resolved to ourselves, we don't want to attempt to
					// load the url only to try and download it again.
					if (!myName.getPackageName().equals(
							info.activityInfo.packageName)
							|| !myName.getClassName().equals(
									info.activityInfo.name)) {
						// someone (other than us) knows how to handle this mime
						// type with this scheme, don't download.
						try {
							startActivity(intent);
							return;
						} catch (ActivityNotFoundException ex) {
							Log.w(this.getClass().getName(),
									"activity not found for " + mimetype
											+ " over "
											+ Uri.parse(url).getScheme(), ex);
						}
					}
				}
			}
			// failed to start activity for file
			Toast.makeText(
					SAPNoteView.this,
					"Found no activities that could handle streaming files with mime type "
							+ mimetype, Toast.LENGTH_LONG).show();

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