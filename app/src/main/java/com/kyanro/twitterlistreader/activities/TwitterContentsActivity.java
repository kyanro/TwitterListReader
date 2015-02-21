package com.kyanro.twitterlistreader.activities;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kyanro.twitterlistreader.MainActivity;
import com.kyanro.twitterlistreader.NavigationDrawerFragment;
import com.kyanro.twitterlistreader.R;
import com.kyanro.twitterlistreader.fragments.TwitterListViewerFragment;
import com.kyanro.twitterlistreader.models.TwitterList;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton;
import com.kyanro.twitterlistreader.network.service.TwitterReaderApiSingleton.TwitterReaderApiService;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;


public class TwitterContentsActivity extends BaseActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @NonNull
    private List<TwitterList> mTwitterLists = new ArrayList<>();
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

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
        TwitterReaderApiService service = TwitterReaderApiSingleton.getTwitterReaderApiService(session);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, TwitterListViewerFragment.newInstanceForTimeline())
                .commit();

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // update drawer list. TODO: cache 機能つける
        bind(service.list(session.getUserId())
                .take(1))
                .subscribe(new Subscriber<List<TwitterList>>() {
                    @Override
                    public void onCompleted() {
                        Log.d("mylog", "list completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("mylog", "service.list error:" + e.getMessage());
                    }

                    @Override
                    public void onNext(List<TwitterList> twitterLists) {
                        mTwitterLists.addAll(twitterLists);
                        mNavigationDrawerFragment.update(mTwitterLists);
                    }
                });
    }

    private static final int MAGNIFICATIONS = 3;
    private float mBaseY = 3;
    private float mDyFromBase;

    @Override
    protected void onResume() {
        super.onResume();
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            startLoginActivity();
            return;
        }

        // 重力センサーを利用するための準備
        BehaviorSubject<SensorEvent> SensorSubject = BehaviorSubject.create();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                SensorSubject.onNext(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.d("mydevlog", "ここくるの？");
            }
        };

        mSensorManager.registerListener(mSensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);

        // 処理
        bind(SensorSubject
                .throttleLast(TwitterListViewerFragment.TICK_MS, TimeUnit.MILLISECONDS))
                .subscribe(event -> {
//                    float dx = event.values[0];
                    float dy = event.values[1];
//                    float dz = event.values[2];
                    mDyFromBase = dy - mBaseY;
                    Log.d("mydevlog", "dy:" + dy);
                });

        bind(Observable.timer(0, TwitterListViewerFragment.TICK_MS, TimeUnit.MILLISECONDS))
                .subscribe(tick -> scrollByAccelerometer());

    }

    private void scrollByAccelerometer() {
        Log.d("mydevlog", "scrollByAccelerometer");
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (!(f instanceof TwitterListViewerFragment)) {
            return;
        }
        Log.d("mydevlog", "mDyFromBase:" + mDyFromBase);
        ((TwitterListViewerFragment) f).moveYBy((int) mDyFromBase * MAGNIFICATIONS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(mSensorListener);
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (mTwitterLists.size() == 0) {
            return;
        }

        TwitterList twitterList = mTwitterLists.get(position);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, TwitterListViewerFragment.newInstanceForMyList(twitterList))
                .commit();
    }

    public void onSectionAttached(int number) {
        if (mTwitterLists.size() == 0) {
            return;
        }
        if (number >= mTwitterLists.size()) {
            return;
        }

        mTitle = mTwitterLists.get(number).name;
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only showTimeline items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to showTimeline in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mNavigationDrawerFragment.getDrawerToggle().onOptionsItemSelected(item)) {
            return true;
        }        // Handle action bar item clicks here. The action bar will
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
}
