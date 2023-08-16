package net.commerce.zocalo.freechart;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**  Schedule a task like generating a price history graph.  Multiple requests may come
 in sporadically.  We want to ensure that only one is being processed at a time.  If we're
 busy processing when a request comes in, we'll remember to start another when this one is
 done.  Multiple requests that come in while processing will spur a single restart. */
public class ChartScheduler {
    static private Logger Log = Logger.getLogger(ChartScheduler.class);
    static private Map<String, ChartScheduler> Schedulers = new HashMap<String, ChartScheduler>();
    private AtomicBoolean generating = new AtomicBoolean(false);
    private AtomicBoolean requested = new AtomicBoolean(false);
    private ExecutorService threads = Executors.newCachedThreadPool();
    private Callable<Boolean> callable;
    private int runs = 0;
    private String name;
    private Date lastFinishTime;

    private ChartScheduler(String name, final Callable<Boolean> worker) {
        this.name = name;
        callable = worker;
    }

    public static ChartScheduler create(String name, Callable<Boolean> worker) {
        ChartScheduler sched = find(name);
        if (sched == null) {
            sched = new ChartScheduler(name, worker);
            Schedulers.put(name, sched);
        }
        return sched;
    }

    public static ChartScheduler find(String name) {
        return Schedulers.get(name);
    }

    public boolean generateNewChart() {
        Log.debug("    setting REQUESTED");
        requested.set(true);
        if (generating.compareAndSet(false, true)) {
            Log.debug("starting a thread for " + name);
            startNewThread();
            return true;
        } else {
            Log.debug("someone else seems to be generating, so I'm done: ");
            return false;
        }
    }

    private void startNewThread() {
        Log.debug("    setting ! REQUESTED");
        requested.set(false);
        Log.debug("scheduling a thread for " + name);

        Future<Boolean> eventualResult = threads.submit(callable);
        Boolean result;
        try {
            result = eventualResult.get();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            result = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            result = false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            result = false;
        }
        Log.debug("    RETURNED from worker thread: " + result);
        runs++;
        lastFinishTime = new Date();
        restartIfNeeded();
    }

    private void restartIfNeeded() {
        if (requested.get()) {
            Log.debug("restarting a thread for " + name);
            startNewThread();
        } else {
            Log.debug("    resetting generating");
            generating.set(false);
            Log.debug("regenerate not needed");
        }
    }

    public boolean isBusy() {
        return generating.get();
    }

    public Date lastFinish() {
        return lastFinishTime;
    }

    public int runs() {
        return runs;
    }
}
