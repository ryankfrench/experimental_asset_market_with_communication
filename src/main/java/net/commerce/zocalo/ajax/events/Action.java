package net.commerce.zocalo.ajax.events;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;

/** the base of the hierarchy of actions to be disseminated via Log4J and comet. */
public abstract class Action extends LoggingEvent {
    final String ACTION_KEY = "action";

    protected Action(Logger logger) {
        super(Category.class.getName(), logger, Level.INFO, null, null);
    }

    abstract public String toLogString();

    public String toString() {
        return toLogString();
    }

    public String getRenderedMessage() {
        return toLogString();
    }

    public void callBackPublish(PriceActionAppender appender) {
        appender.publishBidUpdate(this);
    }
}
