package com.kyanro.twitterlistreader.views;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * 最後までスクロールしたら onScrollEnd イベントを発生させる adapterView 用のリスナ
 */
public abstract class NextItemLoader implements OnScrollListener {

    private boolean mLoading = true;

    private int mPreviousTotalItemCount;

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("mylog", "first:" + firstVisibleItem + " visible:" + visibleItemCount + " total:" + totalItemCount);
        if (totalItemCount < mPreviousTotalItemCount) {
            mPreviousTotalItemCount = totalItemCount;
            // 更新等でリストのアイテムが空になった場合
            if (totalItemCount == 0) {
                mLoading = true;
            }
        }

        if (mLoading) {
            // load 完了
            if (totalItemCount > mPreviousTotalItemCount) {
                mLoading = false;
                mPreviousTotalItemCount = totalItemCount;
            }
        }

        if (!mLoading &&
                (firstVisibleItem + visibleItemCount) >= totalItemCount) {
            mLoading = true;
            onScrollEnd();
        }
    }

    public abstract void onScrollEnd();

}
