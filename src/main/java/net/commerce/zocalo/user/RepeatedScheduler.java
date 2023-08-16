package net.commerce.zocalo.user;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Schedule a RepeatableTask repeatedly (at intervals of PERIOD) until it says it's done. */
public class RepeatedScheduler {
    private ExecutorService threads = Executors.newCachedThreadPool();
    private RepeatableTask task;
    final private long delay;

    public RepeatedScheduler(RepeatableTask task, long initialDelay) {
        this.task = task;
        delay = initialDelay;
    }

    public void startScheduling() {
        Runnable scheduler = new Runnable() { public void run() { schedule(); } };
        threads.submit(scheduler);
    }

    private void schedule() {
        waitBeforeStartingTask();
        while (! task.isDone()) {
            task.run();
        }
        task.finalProcessing();
    }

    private void waitBeforeStartingTask() {
        try {
            Thread.sleep((long) delay);
        } catch (InterruptedException e) {
            Logger.getLogger("RepeatedScheduler").error("Unable to wait before running " + task.name());
        }
    }
}
