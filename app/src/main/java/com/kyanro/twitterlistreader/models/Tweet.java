package com.kyanro.twitterlistreader.models;

import com.twitter.sdk.android.core.models.Coordinates;
import com.twitter.sdk.android.core.models.Place;
import com.twitter.sdk.android.core.models.TweetEntities;
import com.twitter.sdk.android.core.models.User;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ppp on 2015/02/06.
 */
public class Tweet extends com.twitter.sdk.android.core.models.Tweet implements Serializable {

    public Tweet(Coordinates coordinates, String createdAt, Object currentUserRetweet, TweetEntities entities, Integer favoriteCount, boolean favorited, String filterLevel, long id, String idStr, String inReplyToScreenName, long inReplyToStatusId, String inReplyToStatusIdStr, long inReplyToUserId, String inReplyToUserIdStr, String lang, Place place, boolean possiblySensitive, Object scopes, int retweetCount, boolean retweeted, com.twitter.sdk.android.core.models.Tweet retweetedStatus, String source, String text, boolean truncated, User user, boolean withheldCopyright, List<String> withheldInCountries, String withheldScope) {
        super(coordinates, createdAt, currentUserRetweet, entities, favoriteCount, favorited, filterLevel, id, idStr, inReplyToScreenName, inReplyToStatusId, inReplyToStatusIdStr, inReplyToUserId, inReplyToUserIdStr, lang, place, possiblySensitive, scopes, retweetCount, retweeted, retweetedStatus, source, text, truncated, user, withheldCopyright, withheldInCountries, withheldScope);
    }
}
