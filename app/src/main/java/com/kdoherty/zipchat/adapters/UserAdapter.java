package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.UserManager;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kdoherty on 8/28/15.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserCellViewHolder> {

    private static final String TAG = UserAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<User> mRoomMembers;
    private Context mContext;
    private int mCellLayoutId;

    public UserAdapter(Context context, int cellLayoutId, List<User> roomMembers) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mCellLayoutId = cellLayoutId;
        mRoomMembers = roomMembers;
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
        FacebookManager.displayProfilePicture(user.getFacebookId(), drawerCellViewHolder.profilePicture);

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

    public void addUser(int position, User user) {
        mRoomMembers.add(position, user);
        notifyItemInserted(position);
    }

    public void addUser(User user) {
        int position = mRoomMembers.size();
        addUser(position, user);
    }

    class UserCellViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView profilePicture;
        private RobotoTextView text;
        private RelativeLayout layout;

        public UserCellViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView;
            profilePicture = (CircleImageView) itemView.findViewById(R.id.drawer_cell_icon);
            text = (RobotoTextView) itemView.findViewById(R.id.drawer_cell_text);
        }
    }
}
