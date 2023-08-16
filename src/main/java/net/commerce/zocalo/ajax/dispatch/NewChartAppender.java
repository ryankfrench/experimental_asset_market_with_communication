package net.commerce.zocalo.ajax.dispatch;

import org.mortbay.cometd.AbstractBayeux;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import net.commerce.zocalo.ajax.events.NewChart;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** publishes actions announcing {@link net.commerce.zocalo.ajax.events.NewChart
new price charts} to cometd.    */
public class NewChartAppender extends AppenderBase {
    final private NewChartDispatcher newChartDispatcher;

    private NewChartAppender(AbstractBayeux bayeux, String marketName) {
        newChartDispatcher = new NewChartDispatcher(bayeux, marketName);
    }

    static public void registerNewAppender(AbstractBayeux bayeux, String marketName) {
        registerAppender(new NewChartAppender(bayeux, marketName));
    }

    protected Logger getLogger() {
        return NewChart.getNewChartLogger();
    }

    protected void append(LoggingEvent chartEvent) {
        ((NewChart)chartEvent).publishTo(newChartDispatcher);
    }

    public void close() {
        // NOOP
    }

    public boolean requiresLayout() {
        return false;
    }
}
