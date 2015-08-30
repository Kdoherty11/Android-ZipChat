package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.MessageAdapter;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.fragments.MessageFavoritesFragment;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MessageDetailsActivity extends AppCompatActivity {

    public static final int MESSAGE_FAVORITED_RESULT = 1;
    public static final String RESULT_KEY_MESSAGE_CHANGED = "activities.MessageDetailsActivity.result.MESSAGE_CHANGED";
    public static final String RESULT_KEY_MESSAGE = "activities.MessageDetailsActivity.result.MESSAGE";
    private static final String TAG = MessageDetailsActivity.class.getSimpleName();
    private static final String EXTRA_MESSAGE = "activities.MessageDetailsActivity.extra.MESSAGE";
    private static final String EXTRA_ANON_USER_ID = "activities.MessageDetailsActivity.extra.ANON_USER_ID";
    private MessageFavoritesFragment mMessageFavoritesFragment;

    private Message mMessage;
    private CircleImageView mSenderProfPicCiv;
    private TextView mSenderNameTv;
    private TextView mMsgContentTv;
    private ImageView mFavoriteIv;
    private TextView mFavoriteCountTv;
    private TextView mMsgTimestampTv;
    private ProgressBar mSendingFavoritePb;
    private LinearLayout mFavoriteContainer;
    private TextView mFavoritesTitleTv;
    private View mFavoritesDividerLine;
    private List<User> mInitialFavorites;

    public static Intent getIntent(Context context, Message message, long anonUserId) {
        Intent messageDetail = new Intent(context, MessageDetailsActivity.class);
        messageDetail.putExtra(EXTRA_MESSAGE, message);
        messageDetail.putExtra(EXTRA_ANON_USER_ID, anonUserId);
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

        mMessage = getIntent().getParcelableExtra(EXTRA_MESSAGE);
        long anonUserId = getIntent().getLongExtra(EXTRA_ANON_USER_ID, 0);
        mInitialFavorites = new ArrayList<>(mMessage.getFavorites());

        mSenderProfPicCiv = (CircleImageView) findViewById(R.id.message_picture);
        mSenderNameTv = (TextView) findViewById(R.id.message_sender);
        mMsgContentTv = (TextView) findViewById(R.id.message_text);

        mFavoriteIv = (ImageView) findViewById(R.id.message_favorite);
        mFavoriteCountTv = (TextView) findViewById(R.id.message_favorite_count);
        mMsgTimestampTv = (TextView) findViewById(R.id.message_timestamp);
        mSendingFavoritePb = (ProgressBar) findViewById(R.id.sending_favorite_pb);
        mFavoriteContainer = (LinearLayout) findViewById(R.id.favorite_container);

        mFavoritesDividerLine = findViewById(R.id.divider_line);
        mFavoritesTitleTv = (TextView) findViewById(R.id.favorites_title);

        populateMessageDetails(anonUserId);
    }

    private void populateMessageDetails(final long anonUserId) {
        final User sender = mMessage.getSender();
        final Resources res = getResources();
        if (TextUtils.isEmpty(sender.getFacebookId())) {
            mSenderProfPicCiv.setImageDrawable(res.getDrawable(R.drawable.com_facebook_profile_picture_blank_square));
        } else {
            FacebookManager.displayProfilePicture(sender.getFacebookId(), mSenderProfPicCiv);
        }

        if (sender.getUserId() == UserManager.getId(this) || sender.getUserId() == anonUserId) {
            mSenderProfPicCiv.setBorderColor(res.getColor(R.color.orange));
        } else {
            mSenderProfPicCiv.setBorderColor(res.getColor(R.color.zipchat_blue));
        }

        mSenderProfPicCiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(MessageDetailsActivity.this, sender, anonUserId);
                startActivity(intent);
            }
        });

        mFavoriteIv.setImageDrawable(MessageAdapter.getMessageDrawable(this, mMessage.getFavoriteState()));
        mFavoriteIv.setOnClickListener(new OnFavoriteClickListener());
        mSenderNameTv.setText(sender.getName());
        mMsgContentTv.setText(mMessage.getMessage());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                mMessage.getCreatedAt() * 1000);
        mMsgTimestampTv.setText(timeAgo);

        if (mMessage.getFavoriteCount() > 0) {
            showFavoritesSection();
            mFavoriteCountTv.setText(String.valueOf(mMessage.getFavoriteCount()));
            mFavoriteCountTv.setVisibility(View.VISIBLE);
        } else {
            hideFavoritesSection();
        }
    }

    private void addFavorite() {
        User self = UserManager.getSelf(this);
        mMessage.addFavorite(self, self.getUserId());
        if (mMessageFavoritesFragment != null) {
            mMessageFavoritesFragment.addFavorite(self);
        }
        updateFavoriteUi();
        if (mMessage.getFavoriteCount() == 1) {
            showFavoritesSection();
        }
    }

    private void removeFavorite() {
        User self = UserManager.getSelf(this);
        mMessage.removeFavorite(self, self.getUserId());
        if (mMessageFavoritesFragment != null) {
            mMessageFavoritesFragment.removeFavorite(self);
        }
        updateFavoriteUi();
    }

    private void updateFavoriteUi() {
        mFavoriteIv.setImageDrawable(MessageAdapter.getMessageDrawable(this, mMessage.getFavoriteState()));
        if (mMessage.getFavoriteCount() > 0) {
            showFavoritesSection();
            mFavoriteCountTv.setVisibility(View.VISIBLE);
            mFavoriteCountTv.setText(Integer.toString(mMessage.getFavoriteCount()));
        } else {
            hideFavoritesSection();
        }
    }

    private void showFavoriteLoading() {
        mFavoriteContainer.setVisibility(View.GONE);
        mSendingFavoritePb.setVisibility(View.VISIBLE);
    }

    private void stopFavoriteLoading() {
        mSendingFavoritePb.setVisibility(View.GONE);
        mFavoriteContainer.setVisibility(View.VISIBLE);
    }

    private void hideFavoritesSection() {
        mFavoritesDividerLine.setVisibility(View.GONE);
        mFavoritesTitleTv.setVisibility(View.GONE);
        mFavoriteCountTv.setVisibility(View.GONE);

        if (mMessageFavoritesFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.hide(mMessageFavoritesFragment);
            fragmentTransaction.commit();
        }
    }

    private void showFavoritesSection() {
        mFavoritesDividerLine.setVisibility(View.VISIBLE);
        mFavoritesTitleTv.setVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (mMessageFavoritesFragment == null) {
            mMessageFavoritesFragment = MessageFavoritesFragment.newInstance(mMessage.getFavorites());
            fragmentTransaction.add(R.id.message_favorites_placeholder, mMessageFavoritesFragment);
        } else {
            fragmentTransaction.show(mMessageFavoritesFragment);
        }

        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        setResult();
        super.onBackPressed();
    }

    private void setResult() {
        Intent data = new Intent();
        boolean messageChanged = !mInitialFavorites.equals(mMessage.getFavorites());
        data.putExtra(RESULT_KEY_MESSAGE_CHANGED, messageChanged);
        if (messageChanged) {
            data.putExtra(RESULT_KEY_MESSAGE, mMessage);
        }
        setResult(RESULT_OK, data);
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
            setResult();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MessageDetailsResultHandler {

        private ChatRoomFragment mChatRoomFragment;

        public MessageDetailsResultHandler(ChatRoomFragment chatRoomFragment) {
            mChatRoomFragment = chatRoomFragment;
        }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // Check which request we're responding to
            if (requestCode == MessageDetailsActivity.MESSAGE_FAVORITED_RESULT) {
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    boolean messageChanged = data.getBooleanExtra(MessageDetailsActivity.RESULT_KEY_MESSAGE_CHANGED, false);
                    if (messageChanged) {
                        Message message = data.getParcelableExtra(MessageDetailsActivity.RESULT_KEY_MESSAGE);
                        if (mChatRoomFragment != null) {
                            mChatRoomFragment.updateMessage(message);
                        }
                    }
                }
            }
        }

    }

    private class OnFavoriteClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (!NetworkManager.checkOnline(MessageDetailsActivity.this)) {
                return;
            }
            showFavoriteLoading();
            if (mMessage.getFavoriteState() != Message.FavoriteState.USER_FAVORITED) {
                // favorite message
                ZipChatApi.INSTANCE.favoriteMessage(UserManager.getAuthToken(MessageDetailsActivity.this),
                        mMessage.getMessageId(), UserManager.getId(MessageDetailsActivity.this), new Callback<Response>() {
                            @Override
                            public void success(Response response, Response response2) {
                                stopFavoriteLoading();
                                addFavorite();
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                stopFavoriteLoading();
                                NetworkManager.logErrorResponse(TAG, "Removing a favorite from MessageDetails", error);
                                Toast.makeText(MessageDetailsActivity.this, getResources().getString(R.string.msg_favorite_failed_toast), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                ZipChatApi.INSTANCE.removeFavorite(UserManager.getAuthToken(MessageDetailsActivity.this),
                        mMessage.getMessageId(), UserManager.getId(MessageDetailsActivity.this), new Callback<Response>() {
                            @Override
                            public void success(Response response, Response response2) {
                                stopFavoriteLoading();
                                removeFavorite();
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                stopFavoriteLoading();
                                NetworkManager.logErrorResponse(TAG, "Removing a favorite from MessageDetails", error);
                                Toast.makeText(MessageDetailsActivity.this, getResources().getString(R.string.remove_msg_favorite_failed_toast), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}
