package com.kyanro.twitterlistreader.views;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * 最後までスクロールしたら onNextPage イベントを発生させる adapterView 用のリスナ
 * 最初にsetAdapterした時に条件を満たした場合、 #onNextPage が走る
 */
public abstract class NextPageLoader implements OnScrollListener {

    private boolean mLoading = true;
    private final int mInitialPage;
    private int mPageSize;

    private int mPreviousTotalItemCount;

    /**
     * @param initialPage 基本的に 1から。
     * @param pageSize    1回あたりに読み込むアイテム数
     */
    protected NextPageLoader(int initialPage, int pageSize) {
        this.mInitialPage = initialPage;
        this.mPageSize = pageSize;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("mylog", "first:" + firstVisibleItem + " visible:" + visibleItemCount + " total:" + totalItemCount + " mPrevious:" + mPreviousTotalItemCount);
        //if (totalItemCount < mPreviousTotalItemCount) {
        //    mPreviousTotalItemCount = totalItemCount;
        //    // 更新等でリストのアイテムが空になった場合
        //    if (totalItemCount == 0) {
        //        mLoading = true;
        //    }
        //}

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
            onNextPage(totalItemCount / mPageSize + mInitialPage);
        }
    }

    public abstract void onNextPage(int nextPage);

}
