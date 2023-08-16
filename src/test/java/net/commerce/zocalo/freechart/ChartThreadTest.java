package net.commerce.zocalo.freechart;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.JspSupport.ClaimPurchase;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.user.SecureUser;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Date;

import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class ChartThreadTest extends PersistentTestHelper {
    protected void setUp() throws Exception {
        super.setUp();
        Log4JHelper.getInstance();
    }

    public void testSimpleThreadedTask() throws InterruptedException {
        MockScheduler sched = new MockScheduler();
        assertFalse(sched.isBusy());
        assertEquals(0, sched.tasksRun());
        sched.generateNewChart(10);
        assertTrue(sched.isBusy());
        Thread.sleep(8);
        assertEquals(1, sched.tasksRun());
        assertTrue(sched.isBusy());
        Thread.sleep(8);
        assertFalse("timer problem", sched.isBusy());

        sched.generateNewChart(9);
        assertTrue(sched.isBusy());
        Thread.sleep(4);
        assertEquals(2, sched.tasksRun());
        sched.generateNewChart(40);
        assertTrue(sched.isBusy());
        Thread.sleep(30);
        assertFalse(sched.isBusy());
        assertEquals(3, sched.tasksRun());
    }

    private class MockScheduler {
        private AtomicBoolean generating = new AtomicBoolean(false);
        private AtomicBoolean requested = new AtomicBoolean(false);
        private ExecutorService threads = Executors.newCachedThreadPool();
        private int taskCount = 0;
        private Logger log = Logger.getLogger("Trading");

        public void generateNewChart(long msecs) {
            requested.set(true);
            if (generating.compareAndSet(false, true)) {
                log.info("starting a thread for " + msecs);
                startNewThread(msecs);
            } else {
                log.info("someone else seems to be generating, so I'm done: " + msecs);
            }
        }

        private void startNewThread(final long msecs) {
            generating.set(true);
            requested.set(false);
            log.info("scheduling a thread for " + msecs);

            threads.submit(new Callable<Long>() {
                public Long call() throws InterruptedException {
                    taskCount ++;
                    log.info("thread sleeping for " + msecs);
                    Thread.sleep(msecs);
                    log.info("thread woke from " + msecs);
                    restartIfNeeded(msecs);
                    return msecs;
                }
            });
        }

        private void restartIfNeeded(long msecs) {
            generating.set(false);
            if (requested.get()) {
                log.info("restarting a thread for " + msecs);
                generateNewChart(msecs);
            } else {
                log.info("regenerate not needed");
            }
        }

        public boolean isBusy() {
            return generating.get();
        }

        public int tasksRun() {
            return taskCount;
        }
    }

    public void testChartGenerator() throws Exception {
        CashBank bank = new CashBank("money");
        HibernateTestUtil.save(bank);
        SecureUser owner = new SecureUser("joe", bank.makeFunds(300), "secure", "someone@example.com");
        HibernateTestUtil.save(owner);
        BinaryClaim weather = BinaryClaim.makeClaim("mockRain", owner, "will it rain tomorrow?");
        HibernateTestUtil.save(weather);
        BinaryMarket market = BinaryMarket.make(owner, weather, bank.noFunds());
        HibernateTestUtil.save(weather);
        Callable<Boolean> worker = new Callable<Boolean>() {
            public Boolean call() throws Exception { return true; } };
        ChartScheduler sched = ChartScheduler.create(market.getName(), worker);
        assertEquals(0, sched.runs());

        sched.generateNewChart();
        Thread.sleep(1);
        assertEquals(1, sched.runs());

        sched.generateNewChart();
        Thread.sleep(1);
        assertEquals(2, sched.runs());

        sched.generateNewChart();
        Thread.sleep(1);
        assertEquals(3, sched.runs());

//        ChartScheduler.create(market.getName(), null);
    }

    public void testChartGeneration() throws Exception {
        Logger logger = Logger.getLogger("info");
        manualSetUpForCreate("data/ChartThreadTest");
        HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = new CashBank("money");
        storeObject(bank);
        SecureUser owner = new SecureUser("joe", bank.makeFunds(30000), "secure", "someone@example.com");
        storeObject(owner);
        BinaryClaim weather = BinaryClaim.makeClaim("privateRain", owner, "will it rain tomorrow?");
        storeObject(weather);
        BinaryMarket market = BinaryMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        SecureUser buyer = new SecureUser("buyer", bank.makeFunds(30000), "bpwd", "buyer@example.com");
        storeObject(buyer);
        SecureUser seller = new SecureUser("seller", bank.makeFunds(30000), "spwd", "seller@example.com");
        storeObject(seller);
        Date noTrade = market.getLastTrade();

        market.marketOrder(weather.getNoPosition(), Price.dollarPrice(70), q(5), seller);
        Date firstTrade = market.getLastTrade();

        ClaimPurchase page = new ClaimPurchase();
        page.setClaimName(weather.getName());

        page.historyChartNameForJsp();                                    // GENERATE CHART
        ChartScheduler sched = ChartScheduler.find(market.getName());
        int i;
        for (i = 0; i < 30 ; i++) {
            Thread.sleep(100);
            if (! sched.isBusy()) {                                      // WAIT FOR GENERATE TO FINISH
                break;
            }
        }
        logger.warn("slept for " + ((i + 1) / 10.0) + " secs.");
        assertTrue(i < 22);

        assertTrue("timing problem", noTrade.before(sched.lastFinish()));
        assertEquals(1, sched.runs());
        Date firstFinish = sched.lastFinish();
        assertTrue(firstTrade.before(firstFinish));

        market.limitOrder(weather.getYesPosition(), Price.dollarPrice(40), q(6), buyer);
        market.limitOrder(weather.getNoPosition(), Price.dollarPrice(70), q(6), seller);         // another trade
        Date secondTrade = market.getLastTrade();
//        page.historyChartNameForJsp();                                     // GENERATE CHART AGAIN
        assertTrue(firstFinish.before(secondTrade));
        assertEquals(2, sched.runs());

        market.limitOrder(weather.getYesPosition(), Price.dollarPrice(40), q(7), buyer);
        market.limitOrder(weather.getNoPosition(), Price.dollarPrice(70), q(7), seller);         // and another trade
        Date thirdTrade = market.getLastTrade();

//        page.historyChartNameForJsp();                                     // GENERATE CHART YET AGAIN
        assertFalse(thirdTrade.before(sched.lastFinish()));
        int j;
        for (j = 0; j < 20 ; j++) {
            Thread.sleep(100);
            if (! sched.isBusy()) {
                break;
            }
        }
        logger.warn("slept for " + ((j + 1) / 10.0) + " secs.");
        assertTrue(j < 15);
        assertEquals(3, sched.runs());                                   // should stay busy until chart generated twice more.
        assertTrue("timer problem", secondTrade.before(sched.lastFinish()));

        HibernateTestUtil.resetSessionFactory();
    }
}
