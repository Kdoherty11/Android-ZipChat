package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.MessageDetailsActivity;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.activities.ZipChatApplication;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.AnimateFirstDisplayListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by kdoherty on 12/26/14.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageCellViewHolder> {

    public interface MessageFavoriteListener {
        void sendFavoriteEvent(long messageId, boolean isFavorite);
    }

    private static final String TAG = MessageAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<Message> mMessages;
    private MessageFavoriteListener mMessageFavListener;
    private final Handler mFavoriteEventHandler = new Handler();
    private boolean mHasPendingFavorite;
    private SendFavoriteEventRunnable mSendFavoriteEvent;
    private Message.FavoriteState mInitialFavoriteState;

    private static final Semaphore sendFavoriteLock = new Semaphore(1);
    private static final int SEND_FAVORITE_LOCK_TIMEOUT_SECONDS = 1;

    private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();

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
                    Log.d(TAG, "Timed out while aquiring send favorite lock in runnable");
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while aquiring the send favorite lock in runnable: " + e);
                return;
            }

            boolean userFavorited = mInitialFavoriteState == Message.FavoriteState.USER_FAVORITED;
            if (isAddFavorite != userFavorited) {
                mMessageFavListener.sendFavoriteEvent(messageId, isAddFavorite);
            }

            mInitialFavoriteState = null;
            mHasPendingFavorite = false;
            sendFavoriteLock.release();
        }
    }

    private Context mContext;

    private static final long SEND_FAVORITE_EVENT_DELAY = 1000; // MS

    public MessageAdapter(Context context, List<Message> messages, MessageFavoriteListener messageFavoriteListener) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }
        if (messages == null) {
            throw new IllegalArgumentException("messages is null");
        }
        mInflater = LayoutInflater.from(context);
        mMessages = messages;
        mContext = context;
        mMessageFavListener = messageFavoriteListener;
        ZipChatApplication.initImageLoader(mContext);
    }

    @Override
    public MessageCellViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        View view = mInflater.inflate(R.layout.cell_message, viewGroup, false);
        return new MessageCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MessageCellViewHolder messageCellViewHolder, int i) {
        final Message message = mMessages.get(i);
        final User sender = message.getSender();

        if (TextUtils.isEmpty(sender.getFacebookId())) {
            messageCellViewHolder.profilePicture.setImageDrawable(mContext.getResources().getDrawable(R.drawable.com_facebook_profile_picture_blank_square));
        } else {
            ImageLoader.getInstance().displayImage(FacebookManager.getProfilePicUrl(sender.getFacebookId()), messageCellViewHolder.profilePicture,
                    FacebookManager.displayProfPicOpts, mAnimateFirstListener);
        }

        messageCellViewHolder.name.setText(sender.getName());

        messageCellViewHolder.profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(mContext, sender);
                mContext.startActivity(intent);
            }
        });

        messageCellViewHolder.message.setText(message.getMessage());

        long userId = UserManager.getId(mContext);
        if (message.isConfirmed()) {
            messageCellViewHolder.unconfirmedMsgPb.setVisibility(View.GONE);
            messageCellViewHolder.favoriteLayout.setVisibility(View.VISIBLE);

            messageCellViewHolder.favorite.setOnClickListener(new FavoriteClickListener(message, userId));

            Message.FavoriteState favoriteState = message.getFavoriteState(userId);

            Drawable favoriteDrawable = getMessageDrawable(favoriteState);
            messageCellViewHolder.favorite.setImageDrawable(favoriteDrawable);

            int favoriteCount = message.getFavoriteCount();

            if (favoriteCount > 0) {
                messageCellViewHolder.favoriteCount.setVisibility(View.VISIBLE);
            } else if (messageCellViewHolder.favoriteCount.getVisibility() == View.VISIBLE) {
                messageCellViewHolder.favoriteCount.setVisibility(View.GONE);
            }

            messageCellViewHolder.favoriteCount.setText(String.valueOf(message.getFavoriteCount()));

            messageCellViewHolder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = MessageDetailsActivity.getIntent(mContext, message);
                    mContext.startActivity(intent);
                }
            });
        } else {
            // not yet confirmed
            messageCellViewHolder.unconfirmedMsgPb.setVisibility(View.VISIBLE);
            messageCellViewHolder.favoriteLayout.setVisibility(View.GONE);
        }

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                message.getCreatedAt() * 1000);
        messageCellViewHolder.timestamp.setText(timeAgo);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
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

    private static int getMessageDrawableId(Message.FavoriteState state) {
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

    public Message getMessage(int position) {
        return mMessages.get(position);
    }

    private Drawable getMessageDrawable(Message.FavoriteState state) {
        return mContext.getResources().getDrawable(getMessageDrawableId(state));
    }

    public void confirmMessage(String uuid, Message msg) {
        int msgIndex = findUnconfirmedMsgIndex(uuid);
        if (msgIndex > -1) {
            Log.d(TAG, "Confirming message at index: " + msgIndex);
            mMessages.set(msgIndex, msg);
            notifyItemChanged(msgIndex);
        } else {
            Log.w(TAG, "Message " + msg + " couldn't be found to be confirmed." +
                    " uuid=" + uuid + " messages= " + mMessages);
            Utils.debugToast(mContext, "Message: " + msg + " couldn't be found to be confirmed");
        }
    }

    private int findUnconfirmedMsgIndex(String uuid) {
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
                    Log.d(TAG, "Timed out while aquiring send favorite lock on click");
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while aquiring the send favorite lock in on click: " + e);
                return;
            }

            final Message.FavoriteState favoriteState = message.getFavoriteState(userId);
            final boolean isAddFavorite = favoriteState != Message.FavoriteState.USER_FAVORITED;

            if (isAddFavorite) {
                favoriteMessage(UserManager.getSelf(mContext), message.getMessageId(), userId);
            } else {
                removeFavorite(UserManager.getSelf(mContext), message.getMessageId(), userId);
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

    public void sendPendingEvents() {
        if (mHasPendingFavorite) {
            mFavoriteEventHandler.removeCallbacks(mSendFavoriteEvent);
            mSendFavoriteEvent.run();
        }
    }

    public class MessageCellViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout layout;
        private ImageView profilePicture;
        private TextView name;
        private TextView message;
        private ImageView favorite;
        private TextView favoriteCount;
        private TextView timestamp;
        private LinearLayout favoriteLayout;
        private ProgressBar unconfirmedMsgPb;

        public MessageCellViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView;
            profilePicture = (ImageView) itemView.findViewById(R.id.message_picture);
            name = (TextView) itemView.findViewById(R.id.message_sender);
            message = (TextView) itemView.findViewById(R.id.message_text);
            favoriteLayout = (LinearLayout) itemView.findViewById(R.id.favorite_container);
            favorite = (ImageView) favoriteLayout.findViewById(R.id.message_favorite);
            favoriteCount = (TextView) favoriteLayout.findViewById(R.id.message_favorite_count);
            timestamp = (TextView) itemView.findViewById(R.id.message_timestamp);
            unconfirmedMsgPb = (ProgressBar) itemView.findViewById(R.id.unconfirmed_msg_pb);
        }
    }
}