package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.views.CircleProfilePictureView;

import java.util.ArrayList;
import java.util.List;

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
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mPrivateRooms = privateRooms;
        mFilteredPrivateRooms = privateRooms;
    }

    @Override
    public PrivateChatViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.cell_private_room, viewGroup, false);
        return new PrivateChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PrivateChatViewHolder roomCellViewHolder, int position) {
        PrivateRoom privateRoom = getPrivateChat(position);

        User other = privateRoom.getOther();
        if (other == null) {
            other = privateRoom.getAndSetOther(UserUtils.getId(mContext));
        }

        roomCellViewHolder.circleProfilePictureView.setProfileId(other.getFacebookId());
        roomCellViewHolder.title.setText(other.getName());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                privateRoom.getLastActivity() * 1000);
        roomCellViewHolder.lastActivity.setText(timeAgo);
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

    static class PrivateChatViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private CircleProfilePictureView circleProfilePictureView;
        private TextView title;
        private TextView lastActivity;

        public PrivateChatViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.private_chat_title);
            lastActivity = (TextView) itemView.findViewById(R.id.private_chat_last_activity);
            circleProfilePictureView = (CircleProfilePictureView) itemView.findViewById(R.id.private_chat_picture);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(Menu.NONE, v.getId(), Menu.NONE, "Leave");
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
