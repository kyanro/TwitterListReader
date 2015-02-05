package com.kyanro.twitterlistreader.models;

import java.io.Serializable;

/**
 * Created by ppp on 2015/02/02.
 */
public class TwitterList implements Serializable {
    public String slug;
    public String name;
    public String created_at;
    public String uri;
    public String subscriber_count;
    public String member_count;
}
