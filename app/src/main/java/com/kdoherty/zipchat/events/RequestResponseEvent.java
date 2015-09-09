package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Request;

/**
 * Created by kevin on 5/3/15.
 */
public class RequestResponseEvent {

    private Request.Status mResponse;

    public RequestResponseEvent(Request.Status response) {
        this.mResponse = response;
    }

    public Request.Status getResponse() {
        return mResponse;
    }

}
