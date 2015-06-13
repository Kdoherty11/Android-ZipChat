package com.kdoherty.zipchat.activities;

import android.app.Application;
import android.content.Context;

import com.facebook.FacebookSdk;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created by kevin on 5/19/15.
 */
public class ZipChatApplication extends Application {

    private static final String TAG = ZipChatApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader(this);
        FacebookSdk.sdkInitialize(this);
    }

    public static void initImageLoader(Context context) {
        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = ImageLoaderConfiguration.createDefault(context);
            ImageLoader.getInstance().init(config);
        }
    }

}
