package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;

import java.util.List;

/**
 * Created by kdoherty on 8/28/15.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserCellViewHolder> {

    private static final String TAG = UserAdapter.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    private final LayoutInflater mInflater;
    private final List<User> mRoomMembers;
    private Context mContext;
    private int mCellLayoutId;

    private int mWidth;
    private int mHeight;

    public UserAdapter(Context context, int cellLayoutId, List<User> roomMembers) {
        this(context, cellLayoutId, roomMembers, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public UserAdapter(Context context, int cellLayoutId, List<User> roomMembers, int width, int height) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mCellLayoutId = cellLayoutId;
        mRoomMembers = roomMembers;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public UserCellViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(mCellLayoutId, viewGroup, false);
        return new UserCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserCellViewHolder drawerCellViewHolder, int i) {
        final User user = mRoomMembers.get(i);

        drawerCellViewHolder.text.setText(user.getName());
        FacebookManager.displayProfilePicture(user.getFacebookId(), drawerCellViewHolder.profilePicture, mWidth, mHeight);

        drawerCellViewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(mContext, user);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRoomMembers.size();
    }

    public User getUser(int position) {
        return mRoomMembers.get(position);
    }

    public void removeByUserId(long userId) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getUser(i).getUserId() == userId) {
                mRoomMembers.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    private void addUser(int position, User user) {
        if (!mRoomMembers.contains(user)) {
            mRoomMembers.add(position, user);
        }
        notifyItemInserted(position);
    }

    public void addUser(User user) {
        int position = mRoomMembers.size();
        addUser(position, user);
    }

    class UserCellViewHolder extends RecyclerView.ViewHolder {

        private ImageView profilePicture;
        private TextView text;
        private View layout;

        public UserCellViewHolder(View itemView) {
            super(itemView);
            layout = itemView;
            profilePicture = (ImageView) itemView.findViewById(R.id.user_prof_pic);
            text = (TextView) itemView.findViewById(R.id.user_fb_name);
        }
    }
}
