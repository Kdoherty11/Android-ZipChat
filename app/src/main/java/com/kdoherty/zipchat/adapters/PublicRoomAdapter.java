package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.fragments.HomeTabsFragment;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.PublicRoomComparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PublicRoomAdapter extends RecyclerView.Adapter<PublicRoomAdapter.ChatRoomViewHolder> implements Filterable {

    private static final String TAG = PublicRoomAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private List<PublicRoom> mPublicRooms;
    private List<PublicRoom> mFilteredPublicRooms;
    private Context mContext;

    private ChatRoomFilter mFilter = new ChatRoomFilter();

    public PublicRoomAdapter(Context context, List<PublicRoom> publicRooms) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mPublicRooms = publicRooms;
        mFilteredPublicRooms = publicRooms;
    }

    @Override
    public ChatRoomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.cell_public_room, viewGroup, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatRoomViewHolder chatRoomViewHolder, int position) {
        final PublicRoom publicRoom = getPublicRoom(position);

        chatRoomViewHolder.nameTv.setText(publicRoom.getName());
        chatRoomViewHolder.distanceTv.setText(publicRoom.getDistance() + "m");

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                publicRoom.getLastActivity() * 1000);

        chatRoomViewHolder.lastActivityTv.setText(timeAgo);
        chatRoomViewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent publicRoomIntent = PublicRoomActivity.getIntent(mContext, publicRoom);
                mContext.startActivity(publicRoomIntent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mFilteredPublicRooms.size();
    }

    public PublicRoom getPublicRoom(int position) {
        return mFilteredPublicRooms.get(position);
    }

    public void sortRooms(HomeTabsFragment.TabType tab) {
        switch (tab) {
            case DISTANCE:
                Collections.sort(mPublicRooms, PublicRoomComparators.DistanceComparator.ASCENDING);
                notifyDataSetChanged();
                break;
            case ACTIVITY:
                Collections.sort(mPublicRooms, PublicRoomComparators.ActivityComparator.DESCENDING);
                notifyDataSetChanged();
                break;
        }
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    class ChatRoomViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        TextView nameTv;
        TextView distanceTv;
        TextView lastActivityTv;
        RelativeLayout layout;

        public ChatRoomViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView;
            nameTv = (TextView) itemView.findViewById(R.id.chat_room_name_tv);
            distanceTv = (TextView) itemView.findViewById(R.id.chat_room_distance_tv);
            lastActivityTv = (TextView) itemView.findViewById(R.id.chat_room_last_activity_tv);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(Menu.NONE, v.getId(), Menu.NONE, "Subscribe");
            menu.add(Menu.NONE, v.getId(), Menu.NONE, "Unsubscribe");
        }
    }

    public class ChatRoomFilter extends Filter {

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            String filterString = charSequence.toString().trim().toLowerCase();
            if (TextUtils.isEmpty(filterString)) {
                filterResults.count = getItemCount();
                filterResults.values = mPublicRooms;
            } else {
                List<PublicRoom> tempList = new ArrayList<>();

                for (PublicRoom publicRoom : mPublicRooms) {
                    if (publicRoom.getName().toLowerCase().contains(filterString)) {
                        tempList.add(publicRoom);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredPublicRooms = (ArrayList<PublicRoom>) results.values;
            notifyDataSetChanged();
        }
    }
}
