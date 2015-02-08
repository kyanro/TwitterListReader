package com.kyanro.twitterlistreader.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kyanro.twitterlistreader.R;
import com.kyanro.twitterlistreader.activities.TwitterLoginActivity;
import com.kyanro.twitterlistreader.models.TwitterList;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton.TwitterReaderApiService;
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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TwitterListViewerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TwitterListViewerFragment#newInstanceForTimeline} factory method to
 * create an instance of this fragment.
 */
public class TwitterListViewerFragment extends Fragment {
    public static final int TWEET_COUNT_PER_PAGE = 20;
    public static final String LIST_TYPE = "LIST_TYPE";
    public static final String QUERY_MAP = "QUERY_MAP";

    private OnFragmentInteractionListener mListener;
    private Activity mActivity;
    private TwitterReaderApiService mApiService;

    public enum ListType {
        MY_TIMELINE {
            @Override
            public Observable<List<Tweet>> getTweets(
                    TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap) {
                return service.showTimeline(session.getUserId(), TWEET_COUNT_PER_PAGE);
            }
        },
        MY_LIST {
            @Override
            public Observable<List<Tweet>> getTweets(
                    TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap) {
                String list_id_str = queryMap.get(TwitterReaderApiService.LIST_ID);
                return service.showListTweet(list_id_str, TWEET_COUNT_PER_PAGE);
            }
        };

        public abstract Observable<List<Tweet>> getTweets(
                TwitterSession session, TwitterReaderApiService service, HashMap<String, String> queryMap);

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


        Bundle args = getArguments();
        if (args != null && args.containsKey(LIST_TYPE)) {
            //noinspection unchecked
            ListType listType = (ListType) args.getSerializable(LIST_TYPE);
            HashMap<String, String> queryMap = (HashMap<String, String>) args.getSerializable(QUERY_MAP);

            listType.getTweets(session, mApiService, queryMap)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mTweetAdapter::addAll);
        }
        return view;
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
        public TweetAdapter(Context context, int resource, List<Tweet> tweets) {
            super(context, resource, tweets);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 毎回ビューを作るので、遅いようなら独自実装する・・・？まぁこのままでいいか。
            Tweet tweet = getItem(position);
            Log.d("mylog", "tweet:" + tweet.text);
            return new CompactTweetView(getContext(), tweet);
        }
    }


}
