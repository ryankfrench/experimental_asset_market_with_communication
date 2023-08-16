package net.commerce.zocalo.ajax.dispatch;

import org.mortbay.cometd.AbstractBayeux;

import java.net.URI;
import java.util.Map;

import jjdm.zocalo.ZocaloSetupServlet;
import dojox.cometd.Client;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** marshalls private events and distributes them to a particular user via comet. */
public class PrivateEventDispatcher extends Dispatcher {
    public static final String PRIVATE_EVENT_TOPIC_SUFFIX = "/service/privateEvent";

    public PrivateEventDispatcher(AbstractBayeux bayeux) {
        super(bayeux, PRIVATE_EVENT_TOPIC_SUFFIX);
    }

    public void publishEvent(Map<String, Object> message) {
        String topic = PRIVATE_EVENT_TOPIC_SUFFIX;
        String name = (String) message.get("name");
        String clientId = ZocaloSetupServlet.ExperimentBayeuxService.getClientId(name);
        Client client = getBayeux().getClient(clientId);
        if (client != null) {
            client.deliver(getClient(topic), topic, message, null);
        }
    }
}
