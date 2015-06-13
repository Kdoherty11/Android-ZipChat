package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kevin on 5/16/15.
 */
public class MessageFavoritorAdapter extends RecyclerView.Adapter<MessageFavoritorAdapter.UserCellViewHolder> {

    private static final String TAG = MessageFavoritorAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<User> mMessageFavoritors;

    public MessageFavoritorAdapter(Context context, List<User> messageFavoritors) {
        mInflater = LayoutInflater.from(context);
        mMessageFavoritors = messageFavoritors;
    }

    @Override
    public UserCellViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.cell_user, viewGroup, false);
        return new UserCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserCellViewHolder messageFavoritorViewHolder, int i) {
        User user = mMessageFavoritors.get(i);

        messageFavoritorViewHolder.text.setText(user.getName());
        FacebookManager.displayProfilePicture(user.getFacebookId(), messageFavoritorViewHolder.profilePicture);
    }

    @Override
    public int getItemCount() {
        return mMessageFavoritors.size();
    }

    public User getUser(int position) {
        return mMessageFavoritors.get(position);
    }

    public void removeByUserId(long userId) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getUser(i).getUserId() == userId) {
                mMessageFavoritors.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void addUser(int position, User user) {
        mMessageFavoritors.add(position, user);
        notifyItemInserted(position);
    }

    public void addUser(User user) {
        int position = mMessageFavoritors.size();
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
