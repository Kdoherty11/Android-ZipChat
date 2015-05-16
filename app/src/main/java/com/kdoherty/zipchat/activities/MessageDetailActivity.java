package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.fragments.LoginFragment;
import com.kdoherty.zipchat.fragments.MessageDetailFragment;
import com.kdoherty.zipchat.models.Message;

public class MessageDetailActivity extends AppCompatActivity {

    private static final String KEY_MESSAGE = "MessageDetailMessage";

    public static void startActivity(Context context, Message message) {
        Intent messageDetail = new Intent(context, MessageDetailActivity.class);
        messageDetail.putExtra(KEY_MESSAGE, message);
        context.startActivity(messageDetail);
    }

    private Message mMessage;
    private MessageDetailFragment mMessageDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_message_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.message_detail_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        actionBar.setTitle("Message");

//        if (savedInstanceState == null) {
//            // Add the fragment on initial activity setup
//            mMessageDetailFragment = new MessageDetailFragment();
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(android.R.id.content, mMessageDetailFragment)
//                    .commit();
//        } else {
//            // Or set the fragment from restored state info
//            mMessageDetailFragment = (MessageDetailFragment) getSupportFragmentManager()
//                    .findFragmentById(android.R.id.content);
//        }

        mMessage = getIntent().getParcelableExtra(KEY_MESSAGE);
        //mMessageDetailFragment.displayMessage(mMessage);
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
