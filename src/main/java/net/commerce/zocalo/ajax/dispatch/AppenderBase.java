package net.commerce.zocalo.ajax.dispatch;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class AppenderBase extends AppenderSkeleton {

    static protected void registerAppender(AppenderBase base) {
        Logger logger = base.getLogger();
        logger.addAppender(base);
    }

    protected abstract Logger getLogger();
}
