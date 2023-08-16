package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.freechart.ChartGenerator;
import net.commerce.zocalo.market.MultiMarket;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Transaction;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimePeriodValues;

import java.util.List;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentTradeTest extends PersistentTestHelper {
    private BinaryClaim weather;

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate("data/PersistentTradeTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        tx.commit();
        CashBank bank = new CashBank("money");
        storeObject(bank);
        SecureUser owner = new SecureUser("joe", bank.makeFunds(300), "secure", "someone@example.com");

        storeObject(owner);
        weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
        storeObject(weather);
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        manualTearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testPersistentTrade() throws Exception {
        manualSetUpForUpdate("data/PersistentTradeTest");
        weather = HibernateUtil.getBinaryClaimByName("rain");
        Position yes = weather.getYesPosition();
        storeObject(BookTrade.newBookTrade("joe", Price.dollarPrice(30), q(83), yes));
        storeObject(Ask.newAsk("joe", Price.dollarPrice(37), q(5), yes));
        storeObject(BookTrade.newBookTrade("sue", Price.dollarPrice(40), q(25), yes));
        storeObject(Bid.newBid("joe", Price.dollarPrice(60), q(30), yes));
        storeObject(BookTrade.newBookTrade("ann", Price.dollarPrice(22), q(5), yes));
        storeObject(BookTrade.newBookTrade("ann", Price.dollarPrice(25), q(11), weather.getNoPosition()));

        List trades = HibernateUtil.tradeListForJsp(weather.getName());
        TimePeriodValuesCollection prices = ChartGenerator.getHistoricalPrices(weather.getName(), trades);
        assertEquals(4, prices.getItemCount(0), .01);
        assertEquals(30, prices.getYValue(0,0), .01);
        assertEquals(40, prices.getYValue(0,1), .01);
        assertEquals(22, prices.getYValue(0,2), .01);
        assertEquals(75, prices.getYValue(0,3), .01);

        TimePeriodValuesCollection volumes = ChartGenerator.getHistoricalVolumes(weather.getName(), trades);
        assertEquals(4, volumes.getItemCount(0), .01);
        assertEquals(83, volumes.getYValue(0,0), .01);
        assertEquals(25, volumes.getYValue(0,1), .01);
        assertEquals(5, volumes.getYValue(0,2), .01);
        assertEquals(11, volumes.getYValue(0,3), .01);

        HibernateTestUtil.resetSessionFactory();
    }

    public void testTradeIsBuy() throws Exception {
        manualSetUpForUpdate("data/PersistentTradeTest");
        weather = HibernateUtil.getBinaryClaimByName("rain");
        Position yes = weather.getYesPosition();
        Position no = yes.opposite();
        storeObject(BookTrade.newBookTrade("joe", Price.dollarPrice(75), q(83), yes));
        storeObject(BookTrade.newBookTrade("sue", Price.dollarPrice(40), q(25).negate(), yes));
        storeObject(BookTrade.newBookTrade("joe", Price.dollarPrice(75), q(83), no));
        Thread.sleep(10);
        storeObject(BookTrade.newBookTrade("sue", Price.dollarPrice(40), q(25).negate(), no));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(32.5), q(11), weather.getNoPosition(), Price.dollarPrice(30), Price.dollarPrice(35)));
        Thread.sleep(10);
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(37.5), q(11), weather.getNoPosition(), Price.dollarPrice(40), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(32.5), q(11), weather.getYesPosition(), Price.dollarPrice(30), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(37.5), q(11), weather.getYesPosition(), Price.dollarPrice(40), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(32.5), q(11).negate(), weather.getNoPosition(), Price.dollarPrice(30), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(37.5), q(11).negate(), weather.getNoPosition(), Price.dollarPrice(40), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(32.5), q(11).negate(), weather.getYesPosition(), Price.dollarPrice(30), Price.dollarPrice(35)));
        storeObject(MakerTrade.newMakerTrade("ann", Price.dollarPrice(37.5), q(11).negate(), weather.getYesPosition(), Price.dollarPrice(40), Price.dollarPrice(35)));

        List trades = HibernateUtil.tradeListForJsp(weather.getName());
        Iterator i = trades.iterator();
        Trade trade = (Trade) i.next();
        assertTrue(trade.isBuy());  // yes 83
        trade = (Trade) i.next();
        assertFalse(trade.isBuy()); // yes -25
        trade = (Trade) i.next();
        assertFalse(trade.isBuy());  // no 83
        trade = (Trade) i.next();
        assertTrue(trade.isBuy()); // no -25        TODO This test flips back and forth.  What's going on?
        trade = (Trade) i.next();
        assertFalse(trade.isBuy());  // no 11 rising
        trade = (Trade) i.next();
        assertTrue(trade.isBuy()); // no 11 falling
        trade = (Trade) i.next();
        assertTrue(trade.isBuy());  // yes 11 rising
        trade = (Trade) i.next();
        assertFalse(trade.isBuy()); // yes 11 falling

        HibernateTestUtil.resetSessionFactory();
    }

    public void testMarketTrade() throws Exception {
        String claimName = "rain";
        {
            manualSetUpForUpdate("data/PersistentTradeTest");
            BinaryClaim weather = HibernateTestUtil.getBinaryClaimByName(claimName);
            Position yes = weather.getYesPosition();
            Position no = yes.opposite();
            storeObject(MakerTrade.newMakerTrade("joe", Price.dollarPrice(37), q(24), yes, Price.dollarPrice(30), Price.dollarPrice(45)));
            Thread.sleep(10);
            storeObject(BookTrade.newBookTrade("joe", Price.dollarPrice(47), q(38), yes));
            Thread.sleep(10);
            storeObject(MakerTrade.newMakerTrade("joe", Price.dollarPrice(61), q(55), no, Price.dollarPrice(53), Price.dollarPrice(72)));

            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentTradeTest");
            List trades = HibernateUtil.tradeListForJsp(claimName);
            TimePeriodValuesCollection prices = ChartGenerator.getHistoricalPrices(claimName, trades);
            assertEquals(3, prices.getItemCount(0), .01);
            assertEquals(45, prices.getYValue(0,0), .01);
            assertEquals(47, prices.getYValue(0,1), .01);
            assertEquals(28, prices.getYValue(0,2), .01);

            TimePeriodValuesCollection volumes = ChartGenerator.getHistoricalVolumes(claimName, trades);
            assertEquals(3, volumes.getItemCount(0), .01);
            assertEquals(24, volumes.getYValue(0,0), .01);
            assertEquals(38, volumes.getYValue(0,1), .01);
            assertEquals(55, volumes.getYValue(0,2), .01);

            HibernateTestUtil.resetSessionFactory();
        }
    }

    public void testRetrieveMultiOpenClose() throws Exception {
        {
            manualSetUpForUpdate("data/PersistentTradeTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            String[] fruitNames = new String[]{"peach", "apple", "persimmon", "plum" };
            CashBank bank = HibernateTestUtil.getOrMakePersistentRootBank("money");
            storeObject(bank);
            SecureUser owner = new SecureUser("owner", bank.makeFunds(1000), "fubar", "pancrit@gmail.com");
            storeObject(owner);
            MultiClaim fruit = MultiClaim.makeClaim("fruit", owner, "most productive fruit tree in 2007", fruitNames);
            storeObject(fruit);
            MultiMarket market = MultiMarket.make(owner, fruit, bank.noFunds());
            storeObject(market);
            market.makeMarketMaker(q(500), owner);
            SecureUser buyer = new SecureUser("buyer", bank.makeFunds(1000), "fubar", "pancrit@gmail.com");
            storeObject(buyer);

            Dictionary<String, Position> fruitByName = new Hashtable<String, Position>();
            for (Position position : fruit.getPositions()) {
                fruitByName.put(position.getName(), position);
            }

            Position peach = (Position) fruitByName.get("peach");
            Position apple = (Position) fruitByName.get("apple");
            Position plum = (Position) fruitByName.get("plum");

            market.marketOrder(peach, Price.dollarPrice(30), q(100), buyer);
            market.marketOrder(plum, Price.dollarPrice(30), q(130), buyer);
            market.marketOrder(peach, Price.dollarPrice(35), q(140), buyer);
            market.marketOrder(apple, Price.dollarPrice(30), q(140), buyer);
            market.marketOrder(plum, Price.dollarPrice(35), q(185), buyer);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentTradeTest");
            List trades = HibernateUtil.tradeListForJsp("fruit");
            TimePeriodValuesCollection values = ChartGenerator.getOpenCloseValues(trades, HibernateUtil.getClaimByName("fruit"));
            Dictionary<Comparable, TimePeriodValues> d = new Hashtable<Comparable, TimePeriodValues>();

            for (int i = 0; i < values.getSeriesCount(); i++) {
                TimePeriodValues vals = values.getSeries(i);
                d.put(vals.getKey(), vals);
            }
            TimePeriodValues peach = (TimePeriodValues) d.get("peach");
            TimePeriodValues plum = (TimePeriodValues) d.get("plum");
            TimePeriodValues persimmon = (TimePeriodValues) d.get("persimmon");

            assertEquals(6, peach.getItemCount());
            assertEquals(6, persimmon.getItemCount());
            assertEquals(25, peach.getValue(0).doubleValue(), .01);  // initial value
            assertEquals(30, peach.getValue(1).doubleValue(), .01);  // first trade
            assertTrue(30 > peach.getValue(2).doubleValue());        // three trades raises other prices
            double peach3 = peach.getValue(3).doubleValue();
            double peach4 = peach.getValue(4).doubleValue();
            assertEquals(35, peach3, .01);
            assertTrue(peach3 + "should be greater than" + peach4, peach3 > peach4);

            double persimmon0 = persimmon.getValue(0).doubleValue();
            double persimmon1 = persimmon.getValue(1).doubleValue();
            double persimmon2 = persimmon.getValue(2).doubleValue();
            double persimmon3 = persimmon.getValue(3).doubleValue();
            double persimmon4 = persimmon.getValue(4).doubleValue();
            double persimmon5 = persimmon.getValue(5).doubleValue();
            assertTrue(persimmon0 > persimmon1);
            assertTrue(persimmon1 > persimmon2);
            assertTrue(persimmon2 > persimmon3);
            assertTrue(persimmon3 > persimmon4);
            assertTrue(persimmon4 > persimmon5);

            assertEquals(25, plum.getValue(0).doubleValue(), .01);
            assertEquals(30, plum.getValue(2).doubleValue(), .01);
            assertEquals(35, plum.getValue(5).doubleValue(), .01);

            final SecureUser buyer = HibernateTestUtil.getUserByName("buyer");
            final Market market = HibernateTestUtil.getMarketByName("fruit");
            assertBalancedTrades(buyer, market, 1000);

            HibernateTestUtil.resetSessionFactory();
        }
    }
}
