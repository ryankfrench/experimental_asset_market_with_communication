package net.commerce.zocalo.ajax.events;

import org.apache.log4j.Logger;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.ajax.dispatch.TransitionDispatcher;

import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Action representing changes from one Session state to the next.  (market open,
    computing scores, etc.)  */
public class Transition extends Action {
    final private String name;
    final private String displayText;
    final private int round;
    final private String timeRemaining;
    final private String logGID = GID.log();

    public Transition(String transition, String displayText, int round, String timeRemaining) {
        super(getActionLogger());
        this.name = transition;
        this.displayText = displayText;
        this.round = round;
        this.timeRemaining = timeRemaining;
        getActionLogger().callAppenders(this);
    }

    public Transition(String transition, String marketName) {
        super(getActionLogger());
        name = transition;
        displayText = marketName;
        round = 0;
        timeRemaining = null;
        getActionLogger().callAppenders(this);
    }

    public void publishTo(TransitionDispatcher dispatcher) {
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("marketPublisher", new java.util.Date());
        message.put("transition", name);
        String encoded = null;
        if (timeRemaining != null) {
            try {
                encoded = URLEncoder.encode(displayText, "utf-8");
            } catch (UnsupportedEncodingException e) {
                encoded = displayText;
            }
            message.put("encodedUrl", encoded);
            message.put("round", Integer.toString(round));
            message.put("timeRemaining", timeRemaining);
        }
        message.put("displayText", displayText);

        dispatcher.publishTransition(message);
    }

    public String toLogString() {
        return logGID + "State transition: " + name + ", round: " + round + ".";
    }

    static public Logger getActionLogger() {
        return Logger.getLogger(Transition.class);
    }
}
