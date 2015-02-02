package com.kyanro.twitterlistreader.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kyanro.twitterlistreader.BuildConfig;
import com.kyanro.twitterlistreader.MainActivity;
import com.kyanro.twitterlistreader.NavigationDrawerFragment;
import com.kyanro.twitterlistreader.R;
import com.kyanro.twitterlistreader.models.TwitterList;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton.TwitterReaderApiService;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.CompactTweetView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;


public class TwitterContentsActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    private static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @NonNull
    List<Tweet> mTweets = new ArrayList<>();
    TweetAdapter mTweetAdapter;

    // butter knife
    @InjectView(R.id.tweet_listview)
    ListView mTweetListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_contents);
        ButterKnife.inject(this);

        // ログインしていなかったらログイン画面へ飛ばす
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            startLoginActivity();
            return;
        }

        mTweetAdapter = new TweetAdapter(this, 0, mTweets);
        mTweetListView.setAdapter(mTweetAdapter);

        TwitterReaderApiService service = TwitterReaderApiSingleton.getTwitterReaderApiService(session);
        service.show(session.getUserId(), 3)
                .flatMap(Observable::from)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Tweet>() {
                    @Override
                    public void onCompleted() {
                        Log.d("mylog", "complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("mylog", "error:" + e.getMessage());
                    }

                    @Override
                    public void onNext(Tweet tweet) {
                        mTweetAdapter.add(tweet);
                        Log.d("mylog", "tweet" + tweet.text);
                    }
                });

        service.list(session.getUserId())
                .flatMap(Observable::from)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TwitterList>() {
                    @Override
                    public void onCompleted() {
                        Log.d("mylog", "list completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("mylog", "list error:" + e.getMessage());
                    }

                    @Override
                    public void onNext(TwitterList twitterList) {
                        Log.d("mylog", "list name:" + twitterList.name);
                        Log.d("mylog", "list name:" + twitterList.member_count);
                    }
                });

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            startLoginActivity();
            return;
        }
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((TwitterContentsActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
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
