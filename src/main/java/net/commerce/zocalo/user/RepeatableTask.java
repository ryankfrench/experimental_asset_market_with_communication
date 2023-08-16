package net.commerce.zocalo.user;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**  A task Interface for the Repeated Scheduler.  */
public interface RepeatableTask {
    void run();
    boolean isDone();
    void finalProcessing();
    String name();
}
