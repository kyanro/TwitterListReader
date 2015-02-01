package com.kyanro.twitterlistreader;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;

import com.kyanro.twitterlistreader.activities.TwitterContentsActivity;
import com.kyanro.twitterlistreader.activities.TwitterLoginActivity;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onResume() {
        super.onResume();
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session == null) {
            startActivityForResult(new Intent(this, TwitterLoginActivity.class), TwitterLoginActivity.REQUEST_CODE);
        } else {
            startActivity(new Intent(this, TwitterContentsActivity.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
