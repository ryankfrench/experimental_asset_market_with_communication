package net.commerce.zocalo.ajax.dispatch;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import jjdm.zocalo.WebSocketConnector;
import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.currency.Quantity;
import org.mortbay.cometd.AbstractBayeux;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.io.UnsupportedEncodingException;

import dojox.cometd.Channel;
import dojox.cometd.Client;

/** Dispatchers marshall events and publish them via cometd. */
public abstract class Dispatcher {
    private AbstractBayeux bayeux;
    private URI topicURI;
    private String marketName;

    protected Dispatcher(AbstractBayeux bayeux, String topic) {
        this.bayeux = bayeux;
        topicURI = URI.create(topic);
        marketName = topic;
    }

    protected Dispatcher(AbstractBayeux bayeux, String marketName, String topic) {
        this.bayeux = bayeux;
        topicURI = URI.create(buildChannelName(marketName, topic));
        this.marketName = marketName;
    }

    protected void publishEvent(Map<String, Object> message) {
        String topic = topicURI.toString();
	    message.put("topic", topic);
	    String jsonMessage = WebSocketConnector.toJson(message);
	    WebSocketConnector.sendMessageToAll(jsonMessage);
    }

    boolean matchesTopic(String name) {
        return marketName.indexOf(name) >= 0;
    }

    protected Client getClient(String topic) {
        return bayeux.getClient(topic);
    }

    AbstractBayeux getBayeux() {
        return bayeux;
    }

    public static String buildChannelName(String marketName, String topicUri) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(marketName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            encoded = marketName.replaceAll("[^a-zA-Z1-90/]+", "-");
        }

        return topicUri + encoded;
    }
}
