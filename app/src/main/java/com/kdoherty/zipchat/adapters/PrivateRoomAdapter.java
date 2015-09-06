package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.activities.ZipChatApplication;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kdoherty on 12/26/14.
 */
public class PrivateRoomAdapter extends RecyclerView.Adapter<PrivateRoomAdapter.PrivateChatViewHolder> implements Filterable {

    private static final String TAG = PrivateRoomAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<PrivateRoom> mPrivateRooms;
    private List<PrivateRoom> mFilteredPrivateRooms;
    private Context mContext;

    private PrivateChatFilter mFilter = new PrivateChatFilter();

    public PrivateRoomAdapter(Context context, List<PrivateRoom> privateRooms) {
        if (context == null || privateRooms == null) {
            throw new NullPointerException();
        }
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mPrivateRooms = privateRooms;
        mFilteredPrivateRooms = privateRooms;
        ZipChatApplication.initImageLoader(mContext);
    }

    @Override
    public PrivateChatViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.cell_private_room, viewGroup, false);
        return new PrivateChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PrivateChatViewHolder roomCellViewHolder, int position) {
        final PrivateRoom privateRoom = getPrivateChat(position);

        User other = privateRoom.getOther();
        if (other == null) {
            other = privateRoom.getAndSetOther(UserManager.getId(mContext));
        }

        final User finalOther = other;

        FacebookManager.displayProfilePicture(finalOther.getFacebookId(), roomCellViewHolder.circleProfilePictureView);
        roomCellViewHolder.title.setText(finalOther.getName());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                privateRoom.getLastActivity() * 1000);
        roomCellViewHolder.lastActivity.setText(timeAgo);

        roomCellViewHolder.circleProfilePictureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(mContext, finalOther);
                mContext.startActivity(intent);
            }
        });

        roomCellViewHolder.layout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = PrivateRoomActivity.getIntent(mContext, privateRoom);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilteredPrivateRooms.size();
    }

    public PrivateRoom getPrivateChat(int position) {
        return mFilteredPrivateRooms.get(position);
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    static class PrivateChatViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView circleProfilePictureView;
        private TextView title;
        private TextView lastActivity;
        private RelativeLayout layout;

        public PrivateChatViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView;
            title = (TextView) itemView.findViewById(R.id.private_chat_title);
            lastActivity = (TextView) itemView.findViewById(R.id.private_chat_last_activity);
            circleProfilePictureView = (CircleImageView) itemView.findViewById(R.id.private_chat_picture);
        }

    }

    private class PrivateChatFilter extends Filter {

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            String filterString = charSequence.toString().trim().toLowerCase();
            if (TextUtils.isEmpty(filterString)) {
                filterResults.count = getItemCount();
                filterResults.values = mPrivateRooms;
            } else {
                List<PrivateRoom> tempList = new ArrayList<>();

                for (PrivateRoom privateRoom : mPrivateRooms) {
                    if (privateRoom.getOther().getName().toLowerCase().contains(filterString)) {
                        tempList.add(privateRoom);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredPrivateRooms = (ArrayList<PrivateRoom>) results.values;
            notifyDataSetChanged();
        }
    }
}
