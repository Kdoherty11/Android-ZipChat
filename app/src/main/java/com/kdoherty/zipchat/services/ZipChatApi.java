package com.kdoherty.zipchat.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.Request;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by kdoherty on 12/19/14.
 */
public interface ZipChatApi {

    String ENDPOINT = "http://zipchatapp.herokuapp.com/";

    Gson GSON = new GsonBuilder().create();

    RestAdapter ADAPTER = new RestAdapter.Builder()
            .setEndpoint(ENDPOINT)
            .setConverter(new GsonConverter(GSON))
            .build();

    ZipChatApi INSTANCE = ADAPTER.create(ZipChatApi.class);

    String AUTH_TOKEN_KEY = "X-AUTH-TOKEN";


    // *************** Public Rooms ***************

    @FormUrlEncoded
    @POST("/publicRooms")
    void createPublicRoom(@Header(AUTH_TOKEN_KEY) String authToken, @Field("name") String roomName, @Field("radius") int radius,
                          @Field("latitude") double latitude, @Field("longitude") double longitude, @Field("creatorId") long creatorId,
                          Callback<Response> response);

    @GET("/test")
    void getPublicRooms(@Header(AUTH_TOKEN_KEY) String authToken, @Query("lat") double latitude,
                        @Query("lon") double longitude,
                        Callback<List<PublicRoom>> response);

    @GET("/publicRooms/{roomId}/messages")
    void getPublicRoomMessages(@Header(AUTH_TOKEN_KEY) String authToken, @Path("roomId") long roomId,
                               @Query("limit") int limit,
                               @Query("offset") int offset,
                               Callback<List<Message>> response);

    @FormUrlEncoded
    @POST("/publicRooms/{roomId}/subscriptions")
    void subscribe(@Header(AUTH_TOKEN_KEY) String authToken, @Path("roomId") long roomId, @Field("userId") long userId, Callback<Response> response);

    @DELETE("/publicRooms/{roomId}/subscriptions/{userId}")
    void removeSubscription(@Header(AUTH_TOKEN_KEY) String authToken, @Path("roomId") long roomId, @Path("userId") long userId, Callback<Response> response);

    // *************** Private Rooms ***************

    @GET("/privateRooms")
    void getPrivateRooms(@Header(AUTH_TOKEN_KEY) String authToken, @Query("userId") long userId, Callback<List<PrivateRoom>> response);

    @GET("/privateRooms/{roomId}/messages")
    void getPrivateRoomMessages(@Header(AUTH_TOKEN_KEY) String authToken, @Path("roomId") long roomId,
                                @Query("limit") int limit,
                                @Query("offset") int offset,
                                Callback<List<Message>> response);

    @PUT("/privateRooms/{roomId}/leave")
    void leaveRoom(@Header(AUTH_TOKEN_KEY) String authToken, @Path("roomId") long roomId, @Query("userId") long userId, Callback<Response> response);

    // *************** Messages ***************

    @PUT("/messages/{messageId}/favorite")
    void favoriteMessage(@Header(AUTH_TOKEN_KEY) String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    @DELETE("/messages/{messageId}/favorite")
    void removeFavorite(@Header(AUTH_TOKEN_KEY) String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    @PUT("/messages/{messageId}/flag")
    void flagMessage(@Header(AUTH_TOKEN_KEY) String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    // *************** Requests ***************

    @GET("/requests")
    void getRequests(@Header(AUTH_TOKEN_KEY) String authToken, @Query("userId") long receiverId, Callback<List<Request>> response);

    @FormUrlEncoded
    @POST("/requests")
    void sendChatRequest(@Header(AUTH_TOKEN_KEY) String authToken, @Field("sender") long senderId, @Field("receiver") long receiverId, Callback<Response> response);

    @FormUrlEncoded
    @PUT("/requests/{requestId}")
    void respondToRequest(@Header(AUTH_TOKEN_KEY) String authToken, @Path("requestId") long requestId, @Field("status") String status, Callback<Response> response);

    @GET("/requests/status")
    void getStatus(@Header(AUTH_TOKEN_KEY) String authToken, @Query("senderId") long senderId, @Query("receiverId") long receiverId, Callback<Response> response);

    // *************** Users ***************

    @FormUrlEncoded
    @PUT("/users")
    void createUser(@Field("fbAccessToken") String fbAccessToken, @Field("registrationId") String regId,
                    @Field("platform") String platform, Callback<Response> response);

    @GET("/auth")
    void auth(@Query("fbAccessToken") String fbAccessToken, Callback<Response> response);

    // *************** Devices ***************

    @FormUrlEncoded
    @POST("/devices")
    void registerDevice(@Header(AUTH_TOKEN_KEY) String authToken, @Field("userId") long userId, @Field("regId") String regId, @Field("platform") String platform, Callback<Response> response);

    @FormUrlEncoded
    @PUT("devices/{deviceId}")
    void replaceRegId(@Header(AUTH_TOKEN_KEY) String authToken, @Field("deviceId") long deviceId, @Field("regId") String regId, Callback<Response> response);
}
