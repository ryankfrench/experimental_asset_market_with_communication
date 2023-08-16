package net.commerce.zocalo.logging;

import java.util.concurrent.atomic.AtomicLong;

// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A globally unique sequence number for identifying log entries.  May be used for other
 purposes as well. */
public class GID {
    private static AtomicLong TheSequenceNumber = new AtomicLong(1);

    public static long next() {
        return TheSequenceNumber.incrementAndGet();
    }

    public static String log() {
        return next() + "# ";
    }
}
