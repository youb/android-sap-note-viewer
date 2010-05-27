package org.sapmentors.sapnoteview;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

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
import android.widget.Toast;

public class SAPNoteView extends Activity {
	public static final String PREFS_NAME = "SAPNotePrefs";

	public static final String KEY_ID = "SAPNoteNr";

	private WebView webview;
	private Button bView;
	private EditText txtNote;
	
	private boolean bAttemptToSniffNoteFromHTTP=false;
	private String strNoteTitle=null;

	private SAPNoteDbAdapter mDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		bAttemptToSniffNoteFromHTTP=false;

		// if no user is setup, redirect to setup
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sapuser = settings.getString("sapuser", null);
		if (sapuser == null) {
			Toast
					.makeText(
							SAPNoteView.this,
							"Please provide your SAP service marketplace user before continuing",
							Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, SAPNoteSetup.class);
			startActivity(i);
		}

		// open database of favorites
		mDbHelper = new SAPNoteDbAdapter(this);
		//mDbHelper.open();

		// needed in order to get progress bar
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		setContentView(R.layout.view);

		// set up view
		txtNote = (EditText) findViewById(R.id.txtNote);
		webview = (WebView) findViewById(R.id.webview);

		webview.setWebViewClient(new SAPNoteViewClient());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);
		//webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
		// progress bar in title
		webview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				SAPNoteView.this.setProgress(progress * 100);
			}
		});

		webview.setDownloadListener(new DownloadHandler());



		// setup button
		bView = (Button) findViewById(R.id.bView);

		bView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				bAttemptToSniffNoteFromHTTP=false;
				String strNote = ((Editable) txtNote.getText()).toString();
				viewNote(strNote);
			}
		});

		Intent intent = getIntent();
		Uri url = intent.getData();
		Bundle extras = intent.getExtras();
		
		// check if we got an intent parameter from our application
		if (extras != null && extras.containsKey(KEY_ID)) {
			long sapNoteNr = extras.getLong(KEY_ID);
			txtNote.setText(sapNoteNr + "");
			viewNote(sapNoteNr + "");
			
		}else {
			//check if we got called through our url-based intent-filter
			//for example from chrome to phone
			if(url!=null){
				String noteNumber= url.getQueryParameter("numm");
				if(noteNumber!=null){
					txtNote.setText(noteNumber);
					viewNote(noteNumber);
				}else {
					bAttemptToSniffNoteFromHTTP=true;
					viewNoteInternal(url.toString());
				}
			}
		}
		


	}
	
	private void viewNote (String strNote){
		viewNoteInternal("http://service.sap.com/sap/support/notes/" + strNote);
	}

	private void viewNoteInternal(String strURL) {
		hideKeyboard();
		// give a short message to user
		Toast.makeText(SAPNoteView.this, "Loading " + strURL,Toast.LENGTH_LONG).show();
		DefaultHttpClient httpclient=null;
		try {
	        httpclient = new DefaultHttpClient();
	
	        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String sapuser = settings.getString("sapuser", null);
			String sappwd = settings.getString("sappwd", null);
	        
	        httpclient.getCredentialsProvider().setCredentials(
	                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
	                new UsernamePasswordCredentials(sapuser, sappwd));
	 
	        HttpGet httpget = new HttpGet(strURL);
	        
	        System.out.println("executing request" + httpget.getRequestLine());
	        HttpResponse response = httpclient.execute(httpget);
	        HttpEntity entity = response.getEntity();
	
	        InputStream is =entity.getContent();
	        String htmlOrg = convertStreamToString(is);
	        
	        if(bAttemptToSniffNoteFromHTTP){
	        	String sapNoteNr= getNoteNrFromString(htmlOrg);
	        	if(sapNoteNr!=null){
	        		txtNote.setText(sapNoteNr);
	        	}else {
	        		Toast.makeText(SAPNoteView.this, "Could not find SAP Note number from content. Save to favorites will not be possible",Toast.LENGTH_LONG).show();
	        	}
	        	
	        }
	        //try to read the name of the note
	        strNoteTitle= getNoteTitleFromString(htmlOrg);
	        if(strNoteTitle==null){
	        	strNoteTitle = "Note " + ((Editable) txtNote.getText()).toString();
	        }
	        
	        htmlOrg = htmlOrg.replaceAll("\\<div id=\"oc_1\".*?\\>","<div id=\"oc_1\">");
	        
	        
	        webview.loadDataWithBaseURL("https://service.sap.com", htmlOrg, "text/html", "utf-8", null);
	        
	        if (entity != null) {
	            entity.consumeContent();
	        }
		}catch (Exception e){
			Toast.makeText(SAPNoteView.this, "Httpclient failed, attempting backup solution",Toast.LENGTH_LONG).show();
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

 
		
		
		//String strHTML = "<html><body>Summary Symptom<P>If you correct the cumulative received quantity in a delivery schedule,  the system does not always adjust the requirements in the schedule lines  of scheduling agreement. In particular, after you increase the cumulative received quantity, requirements are missing.</P> <b>Additional key words</b><br> <P>VA32, delivery schedule, JIT delivery schedule, fiscal year changes, VBLB-ABEFZ, TVEP-ATTPR MD04, requirement</P> <b>Cause and prerequisites</b><br> <P>The problem is caused by a program error which occurs if you only change  the cumulative received quantity but do not correct any item data or schedule lines.<BR>The error only occurs if the system does not check availability.</P><b>Solution</b><br> <P>First implement the advance correction described in Note 88357 (unless you are already been using Release 4.0B).<BR>Then make the following two changes in the ABAP/4 Dictionary and  implement the attached advance corrections.<BR>Finally, use program SDRQCR21 to correct the existing incorrect requirements.<BR></P><OL>1. Call up Transaction SE11 and create the data element BEDUP  (development class VA). Enter 'XFELD' as the domain name and 'Requirements update indicator' as the short text. Deactivate the  'Maintain field labels' flag. Save and activate.</OL> <OL>2. Call up Transaction SE11 and change structure WVBAP. Choose 'Edit -&gt;  New fields'. Enter 'BEDUP' as a new field name and as the data element. Save and activate.</OL> <P><BR>Also note that if you have already applied Note 73251 (program LV03VF0B)  in your system that you should implement the advance correction as of Release 3.1H.<BR><BR></P>Header Data</body></html>";
		//String strHTML = "<html><body>hello</body></html>";
		//webview.loadData(strHTML, "text/html", "UTF-8");
		
		//change the url
		//webview.loadUrl("http://service.sap.com/sap/support/notes/" + strNote);

	}
	
	private void hideKeyboard(){
		// close soft keyboard
		InputMethodManager inputManager = (InputMethodManager) SAPNoteView.this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(txtNote.getWindowToken(), 0);
		
		/*inputManager.hideSoftInputFromWindow(txtNote.getWindowToken(),
				InputMethodManager.HIDE);
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
			return true;
		case R.id.menuSetup:
			hideKeyboard();
			Intent i = new Intent(this, SAPNoteSetup.class);
			startActivity(i);
			return true;
		case R.id.menuShare:
			String strNote2 = ((Editable) txtNote.getText()).toString();
			String shareTxt= "Note " + strNote2 + " http://service.sap.com/sap/support/notes/" + strNote2;
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, shareTxt);
			
			/* Send it off to the Activity-Chooser */  
			startActivity(Intent.createChooser(shareIntent, "Share note.."));  
			return true;
		case R.id.menuFavorites:
			hideKeyboard();
			Intent i3 = new Intent(this, SAPNoteList.class);
			startActivity(i3);

			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}
	
	private String getNoteNrFromString(String html){
		//find the text in the title element
		Pattern p= Pattern.compile("(?s)<title>(.*)</title>");
		Matcher m= p.matcher(html);
		if (m.find()){
			String title = m.group(1).trim();
			StringTokenizer tokenizer = new StringTokenizer (title," ");
			if(tokenizer.countTokens()>=2){
				tokenizer.nextToken();
				String noteNr= tokenizer.nextToken();
				//System.out.println(noteNr);
				return noteNr;
			}
		}
		return null;
	}
	
	
	private String getNoteTitleFromString(String html){
		//find the text in the title element
		Pattern p= Pattern.compile("<span id=header_data .+?>(.+?)</span>");
		Matcher m= p.matcher(html);
		if (m.find()){
			String title = m.group(1).trim();
			if(title.equals("")){
				return null;
			}else {
				return title;
			}
			
		}
		return null;
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

	private void addNoteToFavorites(String strNote) {
		try {
			long lngNote = Long.parseLong(strNote);
			//strNoteTitle is set during load
			long lngRet =mDbHelper.createNote(lngNote, strNoteTitle);
			if (lngRet!=-1){
				Toast
				.makeText(
						SAPNoteView.this,
						"Added note "+ lngNote + " to favorites",
						Toast.LENGTH_SHORT).show();
			}else {
				Toast
				.makeText(
						SAPNoteView.this,
						"Failed to add " + lngNote + " to favorites",
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
					bAttemptToSniffNoteFromHTTP=true;
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
			Toast.makeText(SAPNoteView.this,
					"An error has occured: " + description, Toast.LENGTH_LONG)
					.show();
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