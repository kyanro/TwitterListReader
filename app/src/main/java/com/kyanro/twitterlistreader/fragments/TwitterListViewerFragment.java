package com.kyanro.twitterlistreader.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kyanro.twitterlistreader.R;
import com.kyanro.twitterlistreader.activities.TwitterLoginActivity;
import com.kyanro.twitterlistreader.models.TwitterList;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton.TwitterReaderApiService;
import com.kyanro.twitterlistreader.views.NextPageLoader;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.CompactTweetView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TwitterListViewerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TwitterListViewerFragment#newInstanceForTimeline} factory method to
 * create an instance of this fragment.
 */
public class TwitterListViewerFragment extends BaseFragment {
    public static final int TWEET_COUNT_PER_PAGE = 20;
    public static final String LIST_TYPE = "LIST_TYPE";
    public static final String QUERY_MAP = "QUERY_MAP";

    public static final int TICK_MS = 200;

    private OnFragmentInteractionListener mListener;
    private Activity mActivity;
    private TwitterReaderApiService mApiService;

    private int mPosition;
    public void moveYBy(int dy) {
        if (dy == 0) { return; }

        if (dy > 0 && mPosition < mTweetAdapter.getCount()) {
            mPosition++;
        }
        if (dy < 0 && mPosition > 0) {
            mPosition--;
        }

        mTweetListView.smoothScrollToPosition(mPosition);
    }

    public enum ListType {
        MY_TIMELINE {
            @Override
            public Observable<List<Tweet>> getNewerTweets(
                    TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String since_id) {
                return service.showNewerTimeline(session.getUserId(), TWEET_COUNT_PER_PAGE, since_id);
            }

            @Override
            public Observable<List<Tweet>> getOlderTweets(TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String max_id) {
                return service.showOlderTimeline(session.getUserId(), TWEET_COUNT_PER_PAGE, max_id);
            }
        },
        MY_LIST {
            @Override
            public Observable<List<Tweet>> getNewerTweets(
                    TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String since_id) {
                String list_id_str = queryMap.get(TwitterReaderApiService.LIST_ID);
                return service.showNewerListTweet(list_id_str, TWEET_COUNT_PER_PAGE, since_id);
            }

            @Override
            public Observable<List<Tweet>> getOlderTweets(TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String max_id) {
                String list_id_str = queryMap.get(TwitterReaderApiService.LIST_ID);
                return service.showOlderListTweet(list_id_str, TWEET_COUNT_PER_PAGE, max_id);
            }
        };

        /**
         * 新しいつぶやき読み込み
         */
        public abstract Observable<List<Tweet>> getNewerTweets(
                TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String since_id);

        /**
         * 古いつぶやき追加読み込み
         */
        public abstract Observable<List<Tweet>> getOlderTweets(
                TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap, String max_id);

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TwitterListViewerFragment for my timeline.
     */
    public static TwitterListViewerFragment newInstanceForTimeline() {
        TwitterListViewerFragment fragment = new TwitterListViewerFragment();
        Bundle b = new Bundle();
        b.putSerializable(LIST_TYPE, ListType.MY_TIMELINE);
        fragment.setArguments(b);
        return fragment;
    }

    /**
     * @return A new instance of fragment TwitterListViewerFragment for my list
     */
    public static TwitterListViewerFragment newInstanceForMyList(TwitterList twitterList) {
        TwitterListViewerFragment fragment = new TwitterListViewerFragment();
        HashMap<String, String> queryMap = new HashMap<>();
        queryMap.put(TwitterReaderApiService.LIST_ID, twitterList.id_str);
        Bundle b = new Bundle();
        b.putSerializable(LIST_TYPE, ListType.MY_LIST);
        b.putSerializable(QUERY_MAP, queryMap);

        fragment.setArguments(b);
        return fragment;
    }

    public TwitterListViewerFragment() {
        // Required empty public constructor
    }

    @NonNull
    List<Tweet> mTweets = new ArrayList<>();
    TweetAdapter mTweetAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @InjectView(R.id.tweet_listview)
    ListView mTweetListView;

    @InjectView(R.id.container_sl)
    SwipeRefreshLayout mContainerSwipeRefresh;

    @InjectView(R.id.load_older_tweets_progressbar)
    ProgressBar mLoadOlderTweetsProgress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_twitter_list_viewer, container, false);
        ButterKnife.inject(this, view);

        mTweetAdapter = new TweetAdapter(mActivity, 0, mTweets);
        mTweetListView.setAdapter(mTweetAdapter);


        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            mActivity.startActivity(new Intent(mActivity, TwitterLoginActivity.class));
            return null;
        }

        mApiService = TwitterReaderApiSingleton.getTwitterReaderApiService(session);

        setupStreams(session);

        return view;
    }

    private void setupStreams(TwitterSession session) {
        Observable<Object> refreshStream = Observable.create(
                subscriber -> mContainerSwipeRefresh.setOnRefreshListener(() -> subscriber.onNext(1)));

        Bundle args = getArguments();
        if (args == null || !args.containsKey(LIST_TYPE)) {
            return;
        }

        //noinspection unchecked
        ListType listType = (ListType) args.getSerializable(LIST_TYPE);

        /**
         * @see #newInstanceForMyList(TwitterList)
         * @see #newInstanceForTimeline()
         */
        @SuppressWarnings("unchecked")
        HashMap<String, String> queryMap = (HashMap<String, String>) args.getSerializable(QUERY_MAP);

        // 更新処理を設定
        bind(refreshStream.startWith(0)
                .flatMap(trigger -> {
                    String sinceId = null;
                    if (!mTweets.isEmpty()) {
                        sinceId = mTweets.get(0).idStr;
                    }
                    Log.d("mylog", "sinceId:" + sinceId);
                    return listType.getNewerTweets(session, mApiService, queryMap, sinceId)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnEach(o -> mContainerSwipeRefresh.setRefreshing(false));
                })
                .onErrorResumeNext(throwable -> {
                    Toast.makeText(mActivity, throwable.getMessage(), Toast.LENGTH_LONG).show();
                    return Observable.never();
                }))
                .subscribe(
                        (tweets) -> mTweetAdapter.addAll(0, tweets),
                        throwable -> {
                            Log.d("mylog", "error:" + throwable.getMessage());
                            Toast.makeText(mActivity, throwable.getMessage(), Toast.LENGTH_LONG).show();
                        },
                        () -> Log.d("mylog", "refresh stream compleat. maybe not called")
                );

        BehaviorSubject<Integer> loaderSubject = BehaviorSubject.create();
        // 追加読み込み処理を設定
        mTweetListView.setOnScrollListener(new NextPageLoader(1, TWEET_COUNT_PER_PAGE) {
            @Override
            public void onNextPage(int nextPage) {
                Log.d("mylog", "nexPage:" + nextPage);
                loaderSubject.onNext(nextPage);
            }
        });

        bind(loaderSubject
                .flatMap(integer -> {
                    mLoadOlderTweetsProgress.setVisibility(View.VISIBLE);
                    String maxId = "0";
                    if (!mTweets.isEmpty()) {
                        long maxIdInt = mTweets.get(mTweets.size() - 1).id - 1;
                        maxId = String.valueOf(maxIdInt);
                    }
                    Log.d("mylog", "maxId:" + maxId);
                    return listType.getOlderTweets(session, mApiService, queryMap, maxId);
                })
                .onErrorResumeNext(throwable -> {
                    Toast.makeText(mActivity, throwable.getMessage(), Toast.LENGTH_LONG).show();
                    return Observable.never();
                }))
                .subscribe(
                        (tweets) -> {
                            mLoadOlderTweetsProgress.setVisibility(View.GONE);
                            mTweetAdapter.addAll(tweets);
                        },
                        throwable -> {
                            Log.d("mylog", "error:" + throwable.getMessage());
                            Toast.makeText(mActivity, throwable.getMessage(), Toast.LENGTH_LONG).show();
                        },
                        () -> Log.d("mylog", "loaderSubject compleat. maybe not called")
                );
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // ログインしていなかったらとりあえず路銀画面に飛ばす
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            mActivity.startActivity(new Intent(mActivity, TwitterLoginActivity.class));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            // TODO: 今のところコールバック予定なしなので外しておく
            //throw new ClassCastException(activity.toString()
            //        + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    private static class TweetAdapter extends ArrayAdapter<Tweet> {
        private final List<Tweet> tweets;

        public TweetAdapter(Context context, int resource, List<Tweet> tweets) {
            super(context, resource, tweets);
            this.tweets = tweets;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 毎回ビューを作るので、遅いようなら独自実装する・・・？まぁこのままでいいか。
            Tweet tweet = getItem(position);
            Log.d("mylog", "tweet:" + tweet.text);
            return new CompactTweetView(getContext(), tweet);
        }

        public void addAll(int index, List<Tweet> tweets) {
            this.tweets.addAll(index, tweets);
            notifyDataSetChanged();
        }
    }


}
