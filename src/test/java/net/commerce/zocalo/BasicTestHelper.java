package net.commerce.zocalo;

import junit.framework.TestCase;
import net.commerce.zocalo.service.BayeuxSingleton;
import net.commerce.zocalo.ajax.dispatch.*;
import java.util.regex.Pattern;
import java.io.*;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class BasicTestHelper extends JunitHelper {
    protected MockBayeuxChannel historyChannel;
    protected MockBayeuxChannel livingChannel;
    protected MockBayeuxChannel changeChannel;
    protected MockBayeux mockBayeux;

    protected void setupBayeux() {
        mockBayeux = new MockBayeux();
        BayeuxSingleton.getInstance().setBayeux(mockBayeux);
        historyChannel = (MockBayeuxChannel)mockBayeux.getChannel(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX, true);
        livingChannel = (MockBayeuxChannel)mockBayeux.getChannel(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI, true);
        changeChannel = (MockBayeuxChannel)mockBayeux.getChannel(TransitionDispatcher.TRANSITION_TOPIC_URI, true);
    }

    /** modelled on example from http://javaalmanac.com/, which says
      "All the code examples from the book are made available here for you to copy and paste into your programs." */
    static public void copyFile(String src, String dest) throws IOException {
        if (! new File(src).exists()) {
            return;
        }
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    static public void assertREMatches(String expected, String result) {
        Pattern exp = Pattern.compile(expected, Pattern.DOTALL);
        if (! exp.matcher(result).matches()) {
            assertEquals("expected string doesn't match result", expected, result);
        }
    }

    static public void assertRENoMatch(String expected, String result) {
        Pattern exp = Pattern.compile(expected, Pattern.DOTALL);
        assertFalse("'" + expected + "'" + " shouldn't match: '" + result + "'", exp.matcher(result).matches());
    }

    static public void assertMatches(String pattern, String string) {
        if (string.indexOf(pattern) < 0) {
            assertEquals("'" + pattern + "' doesn't match '" + string + "'", pattern, string);
        }
    }
}