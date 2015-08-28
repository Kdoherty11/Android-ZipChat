package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.fragments.MessageFavoritesFragment;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.utils.Utils;

public class MessageDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_MESSAGE = "MessageDetailMessage";
    private MessageFavoritesFragment mMessageFavoritesFragment;

    public static Intent getIntent(Context context, Message message) {
        Intent messageDetail = new Intent(context, MessageDetailsActivity.class);
        messageDetail.putExtra(EXTRA_MESSAGE, message);
        return messageDetail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_message_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.message_detail_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("Message");
        }


        //if (mMessageFavoritesFragment == null) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Message message = getIntent().getParcelableExtra(EXTRA_MESSAGE);
        Utils.debugToast(this, "Message: " + message.getMessage() + " Favoritors: " + message.getFavorites());
        mMessageFavoritesFragment = MessageFavoritesFragment.newInstance(message.getFavorites());
        fragmentTransaction.add(R.id.message_favorites_placeholder, mMessageFavoritesFragment);
        fragmentTransaction.commit();
        //}
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

        return super.onOptionsItemSelected(item);
    }
}
