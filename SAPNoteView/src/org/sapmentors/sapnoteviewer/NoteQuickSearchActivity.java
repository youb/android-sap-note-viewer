package org.sapmentors.sapnoteviewer;


import org.sapmentors.sapnoteviewer.R;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class NoteQuickSearchActivity extends TabActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        final Uri sessionsUri = intent.getData();  
    }

    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class NoteSearchAdapter extends CursorAdapter {
        public NoteSearchAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_note_search, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.note_title)).setText(cursor
                    .getString(0));

            ((TextView) view.findViewById(R.id.note_subtitle)).setText(cursor
                    .getString(1));

        }
    }

    
}
