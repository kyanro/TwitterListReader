package com.kyanro.twitterlistreader.views;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * 最後までスクロールしたら onScrollEnd イベントを発生させる adapterView 用のリスナ
 */
public abstract class NextItemLoader implements OnScrollListener {

    private boolean mLoaderEnabled = true;

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("mylog", "first:" + firstVisibleItem + " visible:" + visibleItemCount + " total:" + totalItemCount);
        if (totalItemCount == 0) {
            return;
        }
        if (!mLoaderEnabled) {
            return;
        }

        if (firstVisibleItem + visibleItemCount >= totalItemCount) {
            mLoaderEnabled = false;
            onScrollEnd();
        }
    }

    public void loaderEnabled(boolean enabled) {
        mLoaderEnabled = enabled;
    }

    public abstract void onScrollEnd();

}
