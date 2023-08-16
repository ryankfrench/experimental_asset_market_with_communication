package net.commerce.zocalo.ajax.events;

import java.util.Map;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a callback that allows a {@link net.commerce.zocalo.ajax.dispatch.Dispatcher}
 to adding timing info to an Event describing an {@link Action}.  */
public interface TimingUpdater {
    void addTimingInfo(Map e);
}
