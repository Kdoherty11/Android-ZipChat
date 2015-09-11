package com.kdoherty.zipchat.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.activities.ZipChatApplication;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.views.AnimateFirstDisplayListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kdoherty on 12/26/14.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageCellViewHolder> {

    private static final String TAG = MessageAdapter.class.getSimpleName();
    private static final Semaphore sendFavoriteLock = new Semaphore(1);
    private static final int SEND_FAVORITE_LOCK_TIMEOUT_SECONDS = 1;
    private static final long SEND_FAVORITE_EVENT_DELAY = 1000; // MS
    private final LayoutInflater mInflater;
    private final List<Message> mMessages;
    private final Handler mFavoriteEventHandler = new Handler();
    private MessageCellClickListener mClickCallbacks;
    private boolean mHasPendingFavorite;
    private SendFavoriteEventRunnable mSendFavoriteEvent;
    private Message.FavoriteState mInitialFavoriteState;
    private long mSelfUserId;
    private int mTimestampConfirmedStartMargin;
    private int mTimestampPendingStartMargin;

    private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();
    private Activity mActivity;
    private int mSelfBorderColorId;
    private int mOtherBoarderColor;
    private long mAnonUserId;

    public MessageAdapter(Activity activity, List<Message> messages, MessageCellClickListener messageCellClickListener) {
        this(activity, messages, 0, messageCellClickListener);
    }

    public MessageAdapter(Activity activity, List<Message> messages, long anonUserId, MessageCellClickListener messageCellClickListener) {
        if (activity == null) {
            throw new IllegalArgumentException("Context is null");
        }
        if (messages == null) {
            throw new IllegalArgumentException("messages is null");
        }
        mInflater = LayoutInflater.from(activity);
        mMessages = messages;
        mActivity = activity;
        mAnonUserId = anonUserId;
        mClickCallbacks = messageCellClickListener;
        ZipChatApplication.initImageLoader(mActivity);
        mSelfUserId = UserManager.getId(mActivity);
        mSelfBorderColorId = mActivity.getResources().getColor(R.color.orange);
        mOtherBoarderColor = mActivity.getResources().getColor(R.color.zipchat_blue);
        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
        mTimestampConfirmedStartMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, displayMetrics);
        mTimestampPendingStartMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, displayMetrics);
    }

    public static int getMessageDrawableId(Message.FavoriteState state) {
        switch (state) {
            case FAVORITED:
                return R.drawable.ic_favorite_grey600_24dp;
            case USER_FAVORITED:
                return R.drawable.ic_favorite_red600_24dp;
            case UNFAVORITED:
                return R.drawable.ic_favorite_outline_grey600_24dp;
            default:
                throw new AssertionError("Default switch case on Message.FavoriteState");
        }
    }

    public static Drawable getMessageDrawable(Context context, Message.FavoriteState state) {
        return context.getResources().getDrawable(getMessageDrawableId(state));
    }

    @Override
    public MessageCellViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        View view = mInflater.inflate(R.layout.cell_message, viewGroup, false);
        return new MessageCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MessageCellViewHolder messageCellViewHolder, final int position) {
        final Message message = mMessages.get(position);
        final User sender = message.getSender();
        final boolean isAnon = TextUtils.isEmpty(sender.getFacebookId());

        if (isAnon) {
            messageCellViewHolder.profilePictureCiv.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.com_facebook_profile_picture_blank_square));
        } else {
            ImageLoader.getInstance().displayImage(FacebookManager.getProfilePicUrl(sender.getFacebookId()), messageCellViewHolder.profilePictureCiv,
                    FacebookManager.DISPLAY_PROF_PIC_OPTS, mAnimateFirstListener);
        }

        if (sender.getUserId() == mSelfUserId || sender.getUserId() == mAnonUserId) {
            messageCellViewHolder.profilePictureCiv.setBorderColor(mSelfBorderColorId);
        } else {
            messageCellViewHolder.profilePictureCiv.setBorderColor(mOtherBoarderColor);
        }

        messageCellViewHolder.nameTv.setText(sender.getName());

        messageCellViewHolder.profilePictureCiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(mActivity, sender, mAnonUserId);
                mActivity.startActivity(intent);
            }
        });

        messageCellViewHolder.messageTv.setText(message.getMessage());

        long userId = UserManager.getId(mActivity);
        if (message.isConfirmed()) {
            messageCellViewHolder.unconfirmedMsgPb.setVisibility(View.GONE);
            messageCellViewHolder.failedMsgLayout.setVisibility(View.GONE);

            messageCellViewHolder.favoriteLayout.setVisibility(View.VISIBLE);
            messageCellViewHolder.favoriteLayout.setOnClickListener(new FavoriteClickListener(message, userId));

            Message.FavoriteState favoriteState = message.getFavoriteState(userId);

            Drawable favoriteDrawable = getMessageDrawable(mActivity, favoriteState);
            messageCellViewHolder.favoriteIv.setImageDrawable(favoriteDrawable);

            int favoriteCount = message.getFavoriteCount();

            if (favoriteCount > 0) {
                messageCellViewHolder.favoriteCountTv.setVisibility(View.VISIBLE);
            } else if (messageCellViewHolder.favoriteCountTv.getVisibility() == View.VISIBLE) {
                messageCellViewHolder.favoriteCountTv.setVisibility(View.GONE);
            }

            messageCellViewHolder.favoriteCountTv.setText(String.valueOf(message.getFavoriteCount()));

            messageCellViewHolder.cellLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickCallbacks.onMessageClick(message);
                }
            });
        } else if (message.didTimeout()) {
            messageCellViewHolder.favoriteLayout.setVisibility(View.GONE);
            messageCellViewHolder.unconfirmedMsgPb.setVisibility(View.GONE);
            messageCellViewHolder.failedMsgLayout.setVisibility(View.VISIBLE);

            messageCellViewHolder.retrySendMsgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    message.setDidTimeout(false);
                    notifyItemChanged(position);
                    mClickCallbacks.onResendMessageClick(message);
                }
            });
            messageCellViewHolder.deleteUnsentMsgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeMessage(position);
                }
            });
        } else {
            // not yet confirmed
            messageCellViewHolder.failedMsgLayout.setVisibility(View.GONE);
            messageCellViewHolder.favoriteLayout.setVisibility(View.GONE);
            messageCellViewHolder.unconfirmedMsgPb.setVisibility(View.VISIBLE);
        }
        displayTimestamp(message, messageCellViewHolder);
    }

    private void displayTimestamp(Message message, MessageCellViewHolder viewHolder) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.timestampTv.getLayoutParams();
        if (message.isConfirmed()) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    message.getCreatedAt() * 1000);
            viewHolder.timestampTv.setText(timeAgo);
            layoutParams.setMarginStart(mTimestampConfirmedStartMargin);
        } else {
            viewHolder.timestampTv.setText(mActivity.getResources().getString(R.string.pending));
            layoutParams.setMarginStart(mTimestampPendingStartMargin);
        }

        viewHolder.timestampTv.setLayoutParams(layoutParams);
    }

    private void removeMessage(int position) {
        mMessages.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public void setAnonUserId(long anonUserId) {
        this.mAnonUserId = anonUserId;
    }

    public void addMessageToEnd(Message message) {
        int position = mMessages.size();
        mMessages.add(message);
        notifyItemInserted(position);
    }

    public void addMessagesToStart(List<Message> messageList) {
        mMessages.addAll(0, messageList);
        notifyItemRangeInserted(0, messageList.size());
    }

    public Message getMessage(int position) {
        return mMessages.get(position);
    }

    public void confirmMessage(String uuid, Message msg) {
        int msgIndex = findUnsentMessageByUuid(uuid);
        if (msgIndex > -1) {
            mMessages.set(msgIndex, msg);
            notifyItemChanged(msgIndex);
        } else {
            Log.w(TAG, "Message " + msg + " couldn't be found to be confirmed." +
                    " uuid=" + uuid + " messages= " + mMessages);
            //Utils.debugToast(mActivity, "Message: " + msg + " couldn't be found to be confirmed");
        }
    }

    private int findUnsentMessageByUuid(String uuid) {
        int numMessages = mMessages.size();
        for (int i = 0; i < numMessages; i++) {
            Message message = mMessages.get(i);
            if (!message.isConfirmed() && uuid.equals(message.getUuid())) {
                return i;
            }
        }

        return -1;
    }

    public void favoriteMessage(User user, long messageId, long selfId) {
        favoriteHelper(user, messageId, true, selfId);
    }

    public void removeFavorite(User user, long messageId, long selfId) {
        favoriteHelper(user, messageId, false, selfId);
    }

    private void favoriteHelper(User user, long messageId, boolean isAdd, long selfId) {
        int messageIndex = indexOfMessageById(messageId);

        if (messageIndex != -1) {
            Message message = getMessage(indexOfMessageById(messageId));

            if (isAdd) {
                message.addFavorite(user, selfId);
            } else {
                message.removeFavorite(user, selfId);
            }
            notifyItemChanged(messageIndex);
        }
    }

    public int indexOfMessageById(long messageId) {
        int size = mMessages.size();
        for (int i = 0; i < size; i++) {
            if (messageId == mMessages.get(i).getMessageId()) {
                return i;
            }
        }
        return -1;
    }

    public void sendPendingEvents() {
        if (mHasPendingFavorite) {
            mFavoriteEventHandler.removeCallbacks(mSendFavoriteEvent);
            mSendFavoriteEvent.run();
        }
    }

    public void updateMessage(Message message) {
        int messageIndex = indexOfMessageById(message.getMessageId());
        mMessages.set(messageIndex, message);
        notifyItemChanged(messageIndex);
    }

    public void notifyItemsChanged(List<Integer> changedIndices) {
        for (Integer index : changedIndices) {
            notifyItemChanged(index);
        }
    }

    public void timeoutUnconfirmedMessages() {
        List<Integer> timedOutMessageIndices = new ArrayList<>();
        for (int i = 0; i < mMessages.size(); i++) {
            Message msg = mMessages.get(i);
            if (!msg.isConfirmed()) {
                timedOutMessageIndices.add(i);
                msg.setDidTimeout(true);
            }
        }

        notifyItemsChanged(timedOutMessageIndices);
    }

    public interface MessageCellClickListener {
        void onFavoriteClick(long messageId, boolean isFavorite);

        void onResendMessageClick(Message message);

        void onMessageClick(Message message);
    }

    private class SendFavoriteEventRunnable implements Runnable {

        private long messageId;
        private boolean isAddFavorite;

        private SendFavoriteEventRunnable(long messageId, boolean isAddFavorite) {
            this.messageId = messageId;
            this.isAddFavorite = isAddFavorite;
        }

        @Override
        public void run() {
            try {
                boolean lockAquired = sendFavoriteLock.tryAcquire(SEND_FAVORITE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!lockAquired) {
                    Log.d(TAG, "Timed out while acquiring send favorite lock in runnable");
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while acquiring the send favorite lock in runnable: " + e);
                return;
            }

            boolean userFavorited = mInitialFavoriteState == Message.FavoriteState.USER_FAVORITED;
            if (isAddFavorite != userFavorited) {
                mClickCallbacks.onFavoriteClick(messageId, isAddFavorite);
            }

            mInitialFavoriteState = null;
            mHasPendingFavorite = false;
            sendFavoriteLock.release();
        }
    }

    private class FavoriteClickListener implements View.OnClickListener {
        private final Message message;
        private final long userId;

        public FavoriteClickListener(Message message, long userId) {
            this.message = message;
            this.userId = userId;
        }

        @Override
        public void onClick(View v) {
            try {
                boolean lockAquired = sendFavoriteLock.tryAcquire(SEND_FAVORITE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!lockAquired) {
                    Log.d(TAG, "Timed out while aquiring send favoriteIv lock on click");
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while aquiring the send favoriteIv lock in on click: " + e);
                return;
            }

            final Message.FavoriteState favoriteState = message.getFavoriteState(userId);
            final boolean isAddFavorite = favoriteState != Message.FavoriteState.USER_FAVORITED;

            if (isAddFavorite) {
                favoriteMessage(UserManager.getSelf(mActivity), message.getMessageId(), userId);
            } else {
                removeFavorite(UserManager.getSelf(mActivity), message.getMessageId(), userId);
            }

            if (mHasPendingFavorite) {
                mFavoriteEventHandler.removeCallbacks(mSendFavoriteEvent);
                mSendFavoriteEvent = new SendFavoriteEventRunnable(message.getMessageId(), isAddFavorite);
                mFavoriteEventHandler.postDelayed(mSendFavoriteEvent, SEND_FAVORITE_EVENT_DELAY);
            } else {
                mHasPendingFavorite = true;
                mInitialFavoriteState = favoriteState;
                mSendFavoriteEvent = new SendFavoriteEventRunnable(message.getMessageId(), isAddFavorite);
                mFavoriteEventHandler.postDelayed(mSendFavoriteEvent, SEND_FAVORITE_EVENT_DELAY);
            }

            sendFavoriteLock.release();
        }
    }

    public class MessageCellViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout cellLayout;
        private CircleImageView profilePictureCiv;
        private TextView nameTv;
        private TextView messageTv;
        private ImageView favoriteIv;
        private TextView favoriteCountTv;
        private TextView timestampTv;
        private LinearLayout favoriteLayout;
        private ProgressBar unconfirmedMsgPb;
        private LinearLayout failedMsgLayout;
        private Button retrySendMsgBtn;
        private Button deleteUnsentMsgBtn;

        public MessageCellViewHolder(View itemView) {
            super(itemView);
            cellLayout = (RelativeLayout) itemView;
            profilePictureCiv = (CircleImageView) itemView.findViewById(R.id.message_picture_civ);
            nameTv = (TextView) itemView.findViewById(R.id.message_sender_tv);
            messageTv = (TextView) itemView.findViewById(R.id.message_text_tv);
            favoriteLayout = (LinearLayout) itemView.findViewById(R.id.favorite_container);
            favoriteIv = (ImageView) favoriteLayout.findViewById(R.id.message_favorite_iv);
            favoriteCountTv = (TextView) favoriteLayout.findViewById(R.id.message_favorite_count_tv);
            timestampTv = (TextView) itemView.findViewById(R.id.message_timestamp_tv);
            unconfirmedMsgPb = (ProgressBar) itemView.findViewById(R.id.unconfirmed_msg_pb);
            failedMsgLayout = (LinearLayout) itemView.findViewById(R.id.message_timeout_layout);
            retrySendMsgBtn = (Button) failedMsgLayout.findViewById(R.id.retry_send_msg_btn);
            deleteUnsentMsgBtn = (Button) failedMsgLayout.findViewById(R.id.delete_unsent_msg_btn);
        }
    }
}