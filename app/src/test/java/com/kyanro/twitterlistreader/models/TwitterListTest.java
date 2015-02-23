package com.kyanro.twitterlistreader.models;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

public class TwitterListTest {

    TwitterList tl;
    
    @Before
    public void setup() {
        tl = new TwitterList();
    }
    
    @Test
    public void testGetName() {
        tl.name = "test";
        assertThat(tl.getName(), is("test"));
    }
}