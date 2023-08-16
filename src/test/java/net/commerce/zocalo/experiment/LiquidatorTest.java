package net.commerce.zocalo.experiment;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.user.RepeatedScheduler;
import net.commerce.zocalo.user.RepeatableTask;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;

public class LiquidatorTest extends SessionTestCase {
    public void testMockLiquidater() throws InterruptedException {
        final AtomicInteger diminuend = new AtomicInteger(5);
        final RepeatableTask task = new RepeatableTask() {
            public boolean done = false;

            public void run() {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Logger.getLogger("LiquidatorTest").warn("Sleep interrupted for " + name(), e);
                }

                int current = diminuend.get();
                if (current > 0) {
                    diminuend.compareAndSet(current, current - 1);
                    Logger.getLogger("LiquidatorTest").warn("reduced diminuend to " + diminuend);
                }
                if (current <= 0) {
                    done = true;
                }
            }
            public boolean isDone() { return done; }
            public void finalProcessing() { assertTrue(done); assertEquals(0, diminuend.get()); }
            public String name() { return "mock"; }
        };


        assertEquals(5, diminuend.get());

        Runnable runUntilDone = new Runnable() {
            public void run() {
                RepeatedScheduler mock = new RepeatedScheduler(task, 2);
                mock.startScheduling();
            }
        };

        Executors.newCachedThreadPool().submit(runUntilDone);

        Thread.sleep(25);
        assertEquals(4, diminuend.get());
        Thread.sleep(16);
        assertEquals(3, diminuend.get());
        Thread.sleep(16);
        assertEquals(2, diminuend.get());
        Thread.sleep(16);
        assertEquals(1, diminuend.get());
        Thread.sleep(16);
        assertEquals(0, diminuend.get());
    }
}
