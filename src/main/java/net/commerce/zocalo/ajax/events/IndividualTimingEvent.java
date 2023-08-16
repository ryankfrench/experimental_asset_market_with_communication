package net.commerce.zocalo.ajax.events;

import org.apache.log4j.Logger;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.ajax.dispatch.PrivateEventDispatcher;

import java.util.HashMap;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** An event relevant to one subject in an experiment.  Will be distributed via a
  private channel in cometd.  */
public class IndividualTimingEvent extends Action {
    private String name;
    private String logString;
    private String eventKey;

    public IndividualTimingEvent(AbstractSubject player, Logger logger, String eventKey, String logString) {
        super(logger);
        name = player.getName();
        this.logString = logString;
        this.eventKey = eventKey;
        logger.callAppenders(this);
    }

    public String toLogString() {
        return GID.log() + logString;
    }

    public void sendTo(PrivateEventDispatcher dispatcher) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put(eventKey, "true");
        dispatcher.publishEvent(map);
    }

    static public Logger getActionLogger() {
        return Logger.getLogger(IndividualTimingEvent.class);
    }
}
