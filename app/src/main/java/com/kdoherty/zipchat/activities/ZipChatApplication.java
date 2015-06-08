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

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        initImageLoader(sContext);
        FacebookSdk.sdkInitialize(sContext);
    }

    public static void initImageLoader(Context context) {
        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = ImageLoaderConfiguration.createDefault(context);
            ImageLoader.getInstance().init(config);
        }
    }

    public static Context getAppContext() {
        return sContext;
    }
}
