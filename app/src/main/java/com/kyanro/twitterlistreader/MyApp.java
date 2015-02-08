package com.kyanro.twitterlistreader;

import android.app.Application;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Fabric.Builder;

public class MyApp extends Application {
    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    private static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET;

    @Override
    public void onCreate() {
        super.onCreate();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);

        if (BuildConfig.DEBUG) {
            Fabric.with(new Builder(this).debuggable(true).kits(new Twitter(authConfig)).build());
        } else {
            Fabric.with(this, new Twitter(authConfig));
        }
    }
}
