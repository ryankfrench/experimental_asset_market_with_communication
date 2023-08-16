package net.commerce.zocalo.ajax.events;

import org.apache.log4j.Logger;
import net.commerce.zocalo.ajax.dispatch.NewChartDispatcher;

import java.util.Map;
import java.util.HashMap;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Event used to notify web pages that a delayed price chart is ready. */
public class NewChart extends Action {
    private String claimName;

    public NewChart(String claimName) {
        super(getNewChartLogger());

        this.claimName = claimName;
        getNewChartLogger().callAppenders(this);
    }

    public String toLogString() {
        return "The chart for " + claimName + " has been updated.";
    }

    static public Logger getNewChartLogger() {
        return Logger.getLogger(NewChart.class);
    }

    public void publishTo(NewChartDispatcher newChartDispatcher) {
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("newChart", new java.util.Date());

        newChartDispatcher.publishTransition(message);
    }
}
