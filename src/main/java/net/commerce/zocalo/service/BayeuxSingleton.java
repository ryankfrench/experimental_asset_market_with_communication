// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

package net.commerce.zocalo.service;

import org.mortbay.cometd.AbstractBayeux;

import java.util.Set;
import java.util.HashSet;

/**  uses the Singleton pattern to hold onto the AJAX connection. */
public class BayeuxSingleton {
    static private Set<String> serverSubscriptions = new HashSet<String>();
    private static BayeuxSingleton ourInstance = new BayeuxSingleton();
    private AbstractBayeux bayeux;

    public AbstractBayeux getBayeux() {
        return bayeux;
    }

    public void setBayeux(AbstractBayeux bayeux) {
        this.bayeux = bayeux;
    }

    public static BayeuxSingleton getInstance() {
        return ourInstance;
    }

    private BayeuxSingleton() {
    }

    static public boolean isSubscribed(String channel) {
        return serverSubscriptions.contains(channel);
    }

    static public void addSubscription(String channel) {
        serverSubscriptions.add(channel);
    }
}
