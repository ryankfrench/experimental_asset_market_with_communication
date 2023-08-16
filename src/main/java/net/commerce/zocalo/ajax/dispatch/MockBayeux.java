package net.commerce.zocalo.ajax.dispatch;

import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.ClientImpl;

import dojox.cometd.Channel;

import java.util.HashMap;
import java.util.Map;
// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

import jjdm.zocalo.ZocaloSetupServlet;

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MockBayeux extends AbstractBayeux {
    private Map<String, MockBayeuxChannel> channels = new HashMap<String, MockBayeuxChannel>();

    /**
     * JJDM Removed dependency on Jetty.
     */
    public MockBayeux() {
//        initialize(ZocaloSetupServlet.SERVLET_CONTEXT);
    }

    public Channel getChannel(String uri, boolean create) {
        Channel channel = channels.get(uri);
        if (channel != null) {
            return channel;
        } else if (create) {
            MockBayeuxChannel newChannel = new MockBayeuxChannel(uri, null);
            channels.put(uri, newChannel);
            return newChannel;
        }
        return null;
    }

    public ClientImpl newRemoteClient() {
        return null;
    }
}
