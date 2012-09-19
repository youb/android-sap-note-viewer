package org.sapmentors.sapnoteviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class QuickSearchActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        final String sessionsUri = intent.getDataString();
    }

}
