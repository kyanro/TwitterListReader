package com.kyanro.twitterlistreader.network.service;

import com.kyanro.twitterlistreader.models.Tweet;
import com.kyanro.twitterlistreader.models.TwitterList;
import com.twitter.sdk.android.core.Session;
import com.twitter.sdk.android.core.TwitterApiClient;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * このアプリでよく使うApi用のサービス.
 * TODO: セッションが内部でどう扱われているのか確認。 セッション切れた時どうなる？ 普通にシングルトンにしていい？
 */
public class TwitterReaderApiSingleton extends TwitterApiClient {
    private static TwitterReaderApiSingleton mTwitterReaderApiSingleton;
    private static TwitterReaderApiService mTwitterReaderApiService;

    public TwitterReaderApiSingleton(Session session) {
        super(session);
    }

    public static TwitterReaderApiService getTwitterReaderApiService(Session session) {
        if (mTwitterReaderApiSingleton == null) {
            mTwitterReaderApiSingleton = new TwitterReaderApiSingleton(session);
            mTwitterReaderApiService = mTwitterReaderApiSingleton.getService(TwitterReaderApiService.class);
        }
        return mTwitterReaderApiService;
    }

    public interface TwitterReaderApiService {
        @GET("/1.1/statuses/user_timeline.json")
        public Observable<List<Tweet>> show(@Query("user_id") Long user_id, @Query("count") Integer count);

        @GET("/1.1/lists/list.json")
        public Observable<List<TwitterList>> list(@Query("user_id") Long user_id);
    }
}
