package net.commerce.zocalo.ajax.dispatch;

import dojox.cometd.Client;
import dojox.cometd.Channel;
import dojox.cometd.DataFilter;

import java.util.*;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MockBayeuxChannel implements Channel {
    Map<String, List<Object>> events;
    private String uri;

    public MockBayeuxChannel(String uri, String clientId) {
        this.uri = uri;
        clearEvents();
    }

    public void publish(Client fromClient, Object data, String msgId) {
        List<Object> priorEvents = events.get(uri.toString());
        if (priorEvents == null) {
            priorEvents = new ArrayList<Object>();
            events.put(uri.toString(), priorEvents);
        }
        priorEvents.add(data);
    }

    public boolean remove() {
        return false;
    }
    public String getId() {
        return null;
    }
    public boolean isPersistent() {
        return false;
    }
    public void setPersistent(boolean persistent) {
    }
    public void subscribe(Client subscriber) {
    }
    public void unsubscribe(Client subscriber) {
    }
    public Collection<Client> getSubscribers() {
        return null;
    }
    public void addDataFilter(DataFilter filter) {
    }
    public DataFilter removeDataFilter(DataFilter filter) {
        return null;
    }
    public Collection<DataFilter> getDataFilters() {
        return null;
    }

    public static MockBayeuxChannel make(String uri, String clientId) {
        return new MockBayeuxChannel(uri, clientId);
    }

    public List getEvents(String uriString) {
        List<Object> result = events.get(uriString);
        if (result == null) {
            return new ArrayList<Object>();
        }
        return result;
    }

    public void clearEvents() {
        events = new HashMap<String, List<Object>>();
    }
}
