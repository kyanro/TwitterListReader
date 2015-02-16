package com.kyanro.twitterlistreader.network.service;

import com.kyanro.twitterlistreader.models.TwitterList;
import com.twitter.sdk.android.core.Session;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.models.Tweet;

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

        public static final String LIST_ID = "list_id";

        // TODO: 追加読み込み処理用に、max_id を受け取るインターフェースも作らないとダメな気がする

        /**
         * タイムラインの新しいつぶやき読み込み
         */
        @GET("/1.1/statuses/user_timeline.json")
        public Observable<List<Tweet>> showNewerTimeline(@Query("user_id") Long user_id, @Query("count") Integer count, @Query("since_id") String since_id);

        /**
         * タイムラインの古いつぶやき追加読み込み
         */
        @GET("/1.1/statuses/user_timeline.json")
        public Observable<List<Tweet>> showOlderTimeline(@Query("user_id") Long user_id, @Query("count") Integer count, @Query("max_id") String max_id);

        /**
         * リストの新しいつぶやき読み込み
         */
        @GET("/1.1/lists/statuses.json")
        public Observable<List<Tweet>> showNewerListTweet(@Query(LIST_ID) String list_id, @Query("count") Integer count, @Query("since_id") String since_id);

        /**
         * リストの古いつぶやき追加読み込み
         */
        @GET("/1.1/lists/statuses.json")
        public Observable<List<Tweet>> showOlderListTweet(@Query(LIST_ID) String list_id, @Query("count") Integer count, @Query("max_id") String since_id);

        @GET("/1.1/lists/list.json")
        public Observable<List<TwitterList>> list(@Query("user_id") Long user_id);
    }
}
