package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.fragments.MessageDetailsFragment;
import com.kdoherty.zipchat.models.Message;

public class MessageDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_MESSAGE = "MessageDetailMessage";

    public static Intent getIntent(Context context, Message message) {
        Intent messageDetail = new Intent(context, MessageDetailsActivity.class);
        messageDetail.putExtra(EXTRA_MESSAGE, message);
        return messageDetail;
    }

    private Message mMessage;
    private MessageDetailsFragment mMessageDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_message_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.message_detail_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        actionBar.setTitle("Message");

//        if (savedInstanceState == null) {
//            // Add the fragment on initial activity setup
//            mMessageDetailsFragment = new MessageDetailsFragment();
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(android.R.id.content, mMessageDetailsFragment)
//                    .commit();
//        } else {
//            // Or set the fragment from restored state info
//            mMessageDetailsFragment = (MessageDetailsFragment) getSupportFragmentManager()
//                    .findFragmentById(android.R.id.content);
//        }

        mMessage = getIntent().getParcelableExtra(EXTRA_MESSAGE);
        //mMessageDetailsFragment.displayMessage(mMessage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
