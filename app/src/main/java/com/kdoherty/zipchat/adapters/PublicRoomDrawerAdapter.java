package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookUtils;
import com.kdoherty.zipchat.utils.UserUtils;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kdoherty on 12/26/14.
 */
public class PublicRoomDrawerAdapter extends RecyclerView.Adapter<PublicRoomDrawerAdapter.UserCellViewHolder> {

    private static final String TAG = PublicRoomDrawerAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<User> mRoomMembers;

    public PublicRoomDrawerAdapter(Context context, List<User> roomMembers) {
        mInflater = LayoutInflater.from(context);
        mRoomMembers = roomMembers;
        addUser(0, UserUtils.getSelf(context));
    }

    @Override
    public UserCellViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.cell_user, viewGroup, false);
        return new UserCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserCellViewHolder drawerCellViewHolder, int i) {

        User user = mRoomMembers.get(i);

        drawerCellViewHolder.text.setText(user.getName());
        FacebookUtils.displayProfilePicture(user.getFacebookId(), drawerCellViewHolder.profilePicture);
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

        public UserCellViewHolder(View itemView) {
            super(itemView);
            profilePicture = (CircleImageView) itemView.findViewById(R.id.drawer_cell_icon);
            text = (RobotoTextView) itemView.findViewById(R.id.drawer_cell_text);
        }
    }
}
