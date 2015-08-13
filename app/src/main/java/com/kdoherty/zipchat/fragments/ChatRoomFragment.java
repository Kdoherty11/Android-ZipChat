package com.kdoherty.zipchat.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.ZipChatApplication;
import com.kdoherty.zipchat.adapters.MessageAdapter;
import com.kdoherty.zipchat.events.IsSubscribedEvent;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.ReceivedRoomMembersEvent;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ChatService;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.AnimateFirstDisplayListener;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 2/2/15.
 */
public class ChatRoomFragment extends Fragment implements AsyncHttpClient.WebSocketConnectCallback, View.OnClickListener, WebSocket.StringCallback, MessageAdapter.MessageFavoriteListener {

    private static final String TAG = ChatRoomFragment.class.getSimpleName();

    private static final String HEARTBEAT_MESSAGE = "Beat";

    private static final int MESSAGE_LIMIT = 25;
    private static final int ITEM_VIEW_CACHE_SIZE = 25;

    private static final String ARG_ROOM_ID = "ChatRoomFragmentRoomIdArg";
    private static final String ARG_IS_PUBLIC_ROOM = "ChatRoomFragmentIsPublicRoom";

    private MessageAdapter mMessageAdapter;
    private RecyclerView mMessagesRv;
    private EditText mMessageBoxEt;
    private long mRoomId;

    private Queue<JSONObject> mSocketEventQueue = new ArrayDeque<>();
    private WebSocket mWebSocket;

    private int mMessageOffset = 0;
    private long mSelfId;

    private boolean mMessagesLoading = true;
    private boolean mLoadedAllMessages = false;

    private CircleImageView mAnonToggleCv;
    private ImageLoadingListener mAnimateFirstListener = new AnimateFirstDisplayListener();
    private DisplayImageOptions options;

    private boolean mIsAnon;
    private String mProfilePicUrl;

    private boolean mIsPublicRoom;

    private ChatService mChatService;

    private Callback<List<Message>> mGetMessagesCallback = new Callback<List<Message>>() {
        @Override
        public void success(List<Message> messages, Response response) {
            populateMessageList(messages);
            int numMessagesLoaded = messages.size();
            mMessageOffset += messages.size();
            mMessagesLoading = true;

            if (numMessagesLoaded < MESSAGE_LIMIT) {
                mLoadedAllMessages = true;
            }
        }

        @Override
        public void failure(RetrofitError error) {
            String roomType = mIsPublicRoom ? "public" : "private";
            NetworkManager.logErrorResponse(TAG, "Getting " + roomType + " room chat messages", error);
            mMessagesLoading = true;
        }
    };

    public static ChatRoomFragment newInstance(long roomId, boolean isPublicRoom) {
        Bundle args = new Bundle();
        args.putLong(ARG_ROOM_ID, roomId);
        args.putBoolean(ARG_IS_PUBLIC_ROOM, isPublicRoom);

        ChatRoomFragment fragment = new ChatRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ChatRoomFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mProfilePicUrl = "http://graph.facebook.com/" + FacebookManager.getFacebookId(getActivity()) + "/picture?type=square";
        ZipChatApplication.initImageLoader(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelfId = UserManager.getId(getActivity());
        Bundle args = getArguments();
        mRoomId = args.getLong(ARG_ROOM_ID);
        mIsPublicRoom = args.getBoolean(ARG_IS_PUBLIC_ROOM);
        ChatService.RoomType roomType = mIsPublicRoom ? ChatService.RoomType.PUBLIC :
                ChatService.RoomType.PRIVATE;

        mChatService = new ChatService(mSelfId, mRoomId, roomType, UserManager.getAuthToken(getActivity()), this);

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.com_facebook_profile_picture_blank_portrait)
                .showImageForEmptyUri(R.drawable.com_facebook_profile_picture_blank_portrait)
                .showImageOnFail(R.drawable.com_facebook_profile_picture_blank_portrait)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_chat_room, container, false);

        mMessagesRv = (RecyclerView) rootView.findViewById(R.id.chat_room_activity_messages);
        mMessageBoxEt = (EditText) rootView.findViewById(R.id.chat_room_activity_message_box);
        mAnonToggleCv = (CircleImageView) rootView.findViewById(R.id.anon_toggle);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMessagesRv.setItemViewCacheSize(ITEM_VIEW_CACHE_SIZE);
        mMessagesRv.setItemAnimator(new DefaultItemAnimator());
        mMessagesRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mMessagesRv.setLayoutManager(layoutManager);
        mMessagesRv.setOnScrollListener(new MessagesScrollListener(layoutManager));

        populateMessageList();

        mMessageBoxEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.chat_room_activity_send_button).setOnClickListener(this);
        if (mIsPublicRoom) {
            mAnonToggleCv.setOnClickListener(this);
            ImageLoader.getInstance().displayImage(mProfilePicUrl, mAnonToggleCv,
                    options, mAnimateFirstListener);
        } else {
            mAnonToggleCv.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (socketIsAvailable()) {
            if (mMessageAdapter != null) {
                mMessageAdapter.sendPendingEvents();
            }
            Log.i(TAG, "Pausing socket!");
            Utils.debugToast(getActivity(), "Pausing socket in on pause", Toast.LENGTH_SHORT);
            mWebSocket.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebSocket != null && mWebSocket.isPaused()) {
            mWebSocket.resume();
            Utils.debugToast(getActivity(), "Resuming socket in on resume", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AnimateFirstDisplayListener.clearImages();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.chat_room_activity_send_button:
                sendMessage();
                break;
            case R.id.anon_toggle:
                mIsAnon = !mIsAnon;
                if (mIsAnon) {
                    mAnonToggleCv.setImageDrawable(getResources().getDrawable(R.drawable.com_facebook_profile_picture_blank_square));
                } else {
                    ImageLoader.getInstance().displayImage(mProfilePicUrl, mAnonToggleCv,
                            options, mAnimateFirstListener);
                }
                break;
        }
    }

    private void populateMessageList(List<Message> messageList) {
        if (mMessageAdapter == null) {
            Activity activity = getActivity();
            if (activity != null) {
                mMessageAdapter = new MessageAdapter(activity, messageList, this);
                mMessagesRv.setAdapter(mMessageAdapter);
                mMessagesRv.scrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
        } else {
            mMessageAdapter.addMessagesToStart(messageList);
        }
    }

    private void populateMessageList() {
        if (!NetworkManager.checkOnline(getActivity())) {
            return;
        }

        if (mIsPublicRoom) {
            ZipChatApi.INSTANCE.getPublicRoomMessages(UserManager.getAuthToken(getActivity()), mRoomId,
                    MESSAGE_LIMIT, mMessageOffset, mGetMessagesCallback);
        } else {
            ZipChatApi.INSTANCE.getPrivateRoomMessages(UserManager.getAuthToken(getActivity()), mRoomId,
                    MESSAGE_LIMIT, mMessageOffset, mGetMessagesCallback);
        }
    }

    private class MessagesScrollListener extends RecyclerView.OnScrollListener {

        private LinearLayoutManager mLayoutManager;
        private int pastVisiblesItems;
        private int visibleItemCount;
        private int totalItemCount;

        private MessagesScrollListener(LinearLayoutManager mLayoutManager) {
            this.mLayoutManager = mLayoutManager;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            visibleItemCount = mLayoutManager.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();
            pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

            if (mMessagesLoading) {
                if (!mLoadedAllMessages && (visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                    mMessagesLoading = false;
                    populateMessageList();
                }
            }
        }
    }

    private void sendMessage() {
        String messageContent = mMessageBoxEt.getText().toString().trim();
        if (!messageContent.isEmpty()) {
            sendMessage(messageContent);
            Utils.hideKeyboard(getActivity(), mMessageBoxEt);
            mMessageBoxEt.setText("");
        }
    }

    private void favoriteMessage(final User user, final long messageId) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageAdapter != null) {
                    mMessageAdapter.favoriteMessage(user, messageId, mSelfId);
                } else {
                    Log.w(TAG, "mMessageAdapter was null in favoriteMessage");
                }
            }
        });
    }

    private void removeFavorite(final User user, final long messageId) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageAdapter != null) {
                    mMessageAdapter.removeFavorite(user, messageId, mSelfId);
                } else {
                    Log.w(TAG, "mMessageAdapter was null in removeFavorite");
                }
            }
        });
    }

    private void addMessage(final Message message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageAdapter != null) {
                    mMessageAdapter.addMessageToEnd(message);
                    mMessagesRv.scrollToPosition(mMessageAdapter.getItemCount() - 1);
                    mMessageOffset++;
                } else {
                    Log.w(TAG, "mMessageAdapter was null in addMessage");
                }

            }
        });
    }

    @Override
    public void sendFavoriteEvent(long messageId, boolean isFavorite) {
        Log.i(TAG, "Sending a favorite event to message " + messageId + " is favorite: " + isFavorite);

        JSONObject json = new JSONObject();
        try {
            json.put("event", "FavoriteNotification");
            json.put("messageId", messageId);
            String action = isFavorite ? "add" : "remove";
            json.put("action", action);
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the favorite event JSON: " + e.getMessage());
            return;
        }

        sendEventOverSocket(json);
    }

    private void sendEventOverSocket(JSONObject event) {
        if (socketIsAvailable()) {
            mWebSocket.send(event.toString());
        } else {
            sendEventSocketNotAvailable(event);
        }
    }

    private void sendQueuedEvents() {
        while (!mSocketEventQueue.isEmpty()) {
            JSONObject event = mSocketEventQueue.poll();
            Utils.debugToast(getActivity(), "Sending message from mSocketEventQueue: " + event);
            Log.w(TAG, "Sending message from mSocketEventQueue: " + event);
            sendEventOverSocket(event);
        }
    }

    private void sendMessage(String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("event", "talk");
            json.put("message", message);
            if (mIsPublicRoom) {
                json.put("isAnon", mIsAnon);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the chat message JSON: " + e.getMessage());
            return;
        }

        sendEventOverSocket(json);
    }

    private void sendEventSocketNotAvailable(JSONObject event) {
        String err = "WebSocket is closed when trying to send "
                + event.toString() + "... Adding event to queue";
        Utils.debugToast(getActivity(), err);
        Log.w(TAG, err);
        mSocketEventQueue.add(event);

        reconnect();
        Utils.debugToast(getActivity(), "ChatService is not currently connecting... Attempting to reconnect");
    }

    @Override
    public void onCompleted(Exception exception, WebSocket webSocket) {
        if (exception != null) {
            Log.e(TAG, "Problem connecting to the web socket: " + exception.getMessage());
            return;
        }
        Utils.debugToast(getActivity(), "Connected to the socket!");
        mWebSocket = webSocket;
        mWebSocket.setStringCallback(this);

        CompletedCallback completedCallback = new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Utils.debugToast(getActivity(), "Closed websocket. Exception: " + ex);
                if (ex != null) {
                    Log.w(TAG, "Attempting to recover from " + ex.getMessage());
                    Utils.debugToast(getActivity(), "Attempting to recover from " + ex.getMessage());
                    reconnect();
                }
            }
        };

        mWebSocket.setClosedCallback(completedCallback);
        sendQueuedEvents();
    }

    private void reconnect() {
        mChatService.cancel();
        mChatService = new ChatService(mChatService);
    }

    private boolean socketIsAvailable() {
        return mWebSocket != null && mWebSocket.isOpen();
    }

    @Override
    public void onStringAvailable(final String s) {
        try {
            Gson gson = new Gson();
            JSONObject stringJson = new JSONObject(s);

            String event = stringJson.getString("event");
            final String message = stringJson.getString("message");
            User user;

            switch (event) {
                case "talk":
                    if (!HEARTBEAT_MESSAGE.equals(message)) {
                        addMessage(gson.fromJson(message, Message.class));
                    } else {
                        Log.d(TAG, "Received heartbeat from socket...");
                    }
                    break;
                case "join":
                    Log.d(TAG, "Received room members join from socket");
                    if (stringJson.has("user")) {
                        user = gson.fromJson(stringJson.getString("user"), User.class);
                        if (mSelfId != user.getUserId()) {
                            BusProvider.getInstance().post(new MemberJoinEvent(user));
                        }
                    }
                    break;
                case "quit":
                    Log.d(TAG, "Received room members quit from socket");
                    if (stringJson.has("user")) {
                        user = gson.fromJson(stringJson.getString("user"), User.class);
                        if (mSelfId != user.getUserId()) {
                            BusProvider.getInstance().post(new MemberLeaveEvent(user));
                        }
                    }
                    break;
                case "joinSuccess":
                    Log.d(TAG, "Received join success event from socket");
                    JSONObject messageJson = new JSONObject(message);
                    User[] users = gson.fromJson(messageJson.getString("roomMembers"), User[].class);
                    if (messageJson.has("isSubscribed")) {
                        BusProvider.getInstance().post(new IsSubscribedEvent(messageJson.getBoolean("isSubscribed")));
                    }
                    BusProvider.getInstance().post(new ReceivedRoomMembersEvent(users));
                    break;
                case "favorite":
                    Log.d(TAG, "favorite event from socket");
                    user = gson.fromJson(stringJson.getString("user"), User.class);
                    if (mSelfId != user.getUserId()) {
                        favoriteMessage(user, Long.parseLong(message));
                    }
                    break;
                case "removeFavorite":
                    Log.d(TAG, "Received removeFavorite event from socket");
                    user = gson.fromJson(stringJson.getString("user"), User.class);
                    if (mSelfId != user.getUserId()) {
                        removeFavorite(user, Long.parseLong(message));
                    }
                    break;
                case "error":
                    Log.e(TAG, "Error: " + message);
                    Utils.debugToast(getActivity(), "Error: " + message);
                    break;
                default:
                    Utils.debugToast(getActivity(), "Default socket event " + s);
                    Log.w(TAG, "DEFAULT RECEIVED: " + s);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem parsing socket received JSON: " + s);
        }
    }
}
