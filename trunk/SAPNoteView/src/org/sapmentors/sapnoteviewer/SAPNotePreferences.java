package org.sapmentors.sapnoteviewer;


import java.io.File;
import java.io.IOException;

import org.sapmentors.sapnoteviewer.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;


public class SAPNotePreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	public static final String PREFS_NAME = "SAPNotePrefs";
	public static final String KEY_SAP_USERNAME="sapuser";
	public static final String KEY_SAP_PASSWORD="sappwd";
	public static final String KEY_DO_ANALYTICS="DoAnalytics";
	public static final String KEY_PDF_DOWNLOAD_FOLDER="DownloadFolder";
	public static final String KEY_SEARCH_METHOD="SearchMethod";
	public static final String KEY_CHANGELOG_VERSION_VIEWED="ChangelogVersionViewed";
	
	public static final boolean DEFAULT_VALUE_DO_ANALYTICS=true;
	public static final String DEFAULT_VALUE_PDF_DOWNLOAD_FOLDER="/sapnotes";
	public static final String VALUE_XSEARCH="/xsearch";
	public static final String VALUE_SEARCH="/notes";
	public static final String DEFAULT_VALUE_SEARCH= VALUE_XSEARCH;
	
	protected EditTextPreference editTextUser;
	protected EditTextPreference editTextPassword;
	protected EditTextPreference editTextDownloadFolder;
	protected CheckBoxPreference cbAnalytics ;
	protected ListPreference lpSearchUsed;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPreferenceScreen(createPreferenceHierarchy());
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        setContentView(R.layout.activity_preferences);          
	    
        UIFrameworkSetup();
        
        Analytics.trackPageView(this,"/preferences");
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        root.getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        
        //User preferences
        PreferenceCategory userCat = new PreferenceCategory(this);
        userCat.setTitle(R.string.lblCategoryUser);
        root.addPreference(userCat);

        editTextUser = new EditTextPreference(this);
        editTextUser.setKey(KEY_SAP_USERNAME);
        editTextUser.setDialogTitle(R.string.lblUsernameDialogTitle);
        editTextUser.setTitle(R.string.lblUsernameTitle);
        editTextUser.setSummary(R.string.lblUsernameSummaryBlank);
        userCat.addPreference(editTextUser);
        
        editTextPassword = new EditTextPreference(this);
        editTextPassword.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        editTextPassword.setKey(KEY_SAP_PASSWORD);
        editTextPassword.setDialogTitle(R.string.lblPasswordDialogTitle);
        editTextPassword.setTitle(R.string.lblPasswordTitle);
        editTextPassword.setSummary(R.string.lblPasswordSummaryBlank);
        userCat.addPreference(editTextPassword);
        
        //Other preferences
        PreferenceCategory otherCat = new PreferenceCategory(this);
        otherCat.setTitle(R.string.lblCategoryOther);
        root.addPreference(otherCat);
        
        editTextDownloadFolder = new EditTextPreference(this);
        try {
        	//dynamically find sd card directory
        	 File dir = Environment.getExternalStorageDirectory();
			editTextDownloadFolder.setDefaultValue(dir.getCanonicalPath() + DEFAULT_VALUE_PDF_DOWNLOAD_FOLDER);
		} catch (IOException e) {
			editTextDownloadFolder.setDefaultValue("/mnt/sdcard"+DEFAULT_VALUE_PDF_DOWNLOAD_FOLDER);
		}
        
        editTextDownloadFolder.setKey(KEY_PDF_DOWNLOAD_FOLDER);
        editTextDownloadFolder.setDialogTitle(R.string.lblDownloadFolderDialogTitle);
        editTextDownloadFolder.setTitle(R.string.lblDownloadFolderTitle);
        editTextDownloadFolder.setSummary(R.string.lblDownloadFolderSummaryBlank);
        otherCat.addPreference(editTextDownloadFolder);      

        cbAnalytics = new CheckBoxPreference(this);
        cbAnalytics.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_ANALYTICS));
        cbAnalytics.setKey(KEY_DO_ANALYTICS);
        cbAnalytics.setTitle(R.string.lblAnalyticsTitle);
        cbAnalytics.setSummary(R.string.lblAnalyticsSummary);
        otherCat.addPreference(cbAnalytics);
        
        lpSearchUsed = new ListPreference(this);
        lpSearchUsed.setDefaultValue(DEFAULT_VALUE_SEARCH);
        lpSearchUsed.setKey(KEY_SEARCH_METHOD);
        lpSearchUsed.setTitle(R.string.lblSearchTitle);
        lpSearchUsed.setEntries(R.array.lblsSearch);
        lpSearchUsed.setEntryValues(new String[]{VALUE_XSEARCH,VALUE_SEARCH});
        otherCat.addPreference(lpSearchUsed);
        
        updateSummaryBasedOnValue();
        
        return root;
        
    }
    
    private void updateSummaryBasedOnValue(){
    	if(editTextUser.getText()!=null){
    		editTextUser.setSummary(editTextUser.getText());
    	}else {
    		editTextUser.setSummary(R.string.lblUsernameSummaryBlank);
    	}
    	if(editTextPassword.getText()!=null){
    		editTextPassword.setSummary(R.string.lblPasswordSummary);
    	}else {
    		editTextPassword.setSummary(R.string.lblPasswordSummaryBlank);
    	}
    	
    	if(editTextDownloadFolder.getText()!=null){
    		editTextDownloadFolder.setSummary(editTextDownloadFolder.getText());
    	}else {
    		editTextDownloadFolder.setSummary(R.string.lblDownloadFolderSummaryBlank);
    	}
    	
    	if(lpSearchUsed.getValue().equals(VALUE_XSEARCH)){
    		lpSearchUsed.setSummary(R.string.lblXsearchSummary);
    	}else {
    		lpSearchUsed.setSummary(R.string.lblSearchSummary);
    	}
    	
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updateSummaryBasedOnValue();
		
	}  
    
	private void UIFrameworkSetup(){
		((TextView) findViewById(R.id.title_text)).setText(getTitle());
		
		final Activity thisActivity = this;
		ImageButton bHome = (ImageButton) this.findViewById(R.id.title_home_button);
		bHome.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//saveSettings();
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

  
}