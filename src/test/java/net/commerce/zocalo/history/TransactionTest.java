package net.commerce.zocalo.history;

// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimePeriodValues;

import java.util.Date;

public class TransactionTest extends PersistentTestHelper {
    private CashBank bank;
    private final String password = "secure";
    private BinaryClaim weather;
    private SecureUser owner;

    protected void setUp() throws Exception {
        bank = new CashBank("money");
        owner = new SecureUser("owner", bank.makeFunds(300), password, "someone@example.com");
        weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
    }

    public void testSimpleBookTransactions() {
        Date baseDate = new Date();
        SecureUser taker = new SecureUser("priceTaker", bank.makeFunds(200), "insecure", "j@b.com");

        // order creation may cause price changes
        Position pos = weather.getYesPosition();
        double price = 30;
        double quant = 25;
        SecureUser user = owner;
        Order order0 = makeNewOrder(pos, price, quant, user);
        TransactionRec record0 = new TransactionRec(new Date(baseDate.getTime()));
        record0.addBidPriceChange(weather, 30);

        // book trade don't always create new prices
        TransactionRec record1 = new TransactionRec(new Date(baseDate.getTime() + 1200));
        record1.addBookTrade(taker, 15, order0);

        // A new book order or an order withdrawal can change the price without a trade
        Order order2 = makeNewOrder(weather.getYesPosition(), 40, 45, owner);
        TransactionRec record2 = new TransactionRec(new Date(baseDate.getTime() + 2100));
        record2.addBidPriceChange(weather, 40);

        // A sell order changes the ask price
        Order order3 = makeNewOrder(weather.getNoPosition(), 80, 32, owner);
        TransactionRec record3 = new TransactionRec(new Date(baseDate.getTime() + 3800));
        record3.addAskPriceChange(weather, 80);

        // a book trade sometimes changes prices
        TransactionRec record4 = new TransactionRec(new Date(baseDate.getTime() + 4400));
        record4.addBookTrade(taker, 45, order2);
        record4.addBidPriceChange(weather, 30);

        TimePeriodValuesCollection vals = buildValuesCollection( new TransactionRec[] { record0, record1, record2, record3, record4 } );
        TimePeriodValues openSeries = vals.getSeries(TransactionRec.OPEN);
        TimePeriodValues closeSeries = vals.getSeries(TransactionRec.CLOSE);
        TimePeriodValues bidSeries = vals.getSeries(TransactionRec.BEST_BID);
        TimePeriodValues askSeries = vals.getSeries(TransactionRec.BEST_ASK);

        // Zeroth transaction
        assertEquals(30, getItemValue(bidSeries, 0), .01);
        assertEquals(record0.time(), bidSeries.getDataItem(0).getPeriod());

        // first transaction
        assertEquals(30, getItemValue(openSeries, 0), .01);
        assertEquals(30, getItemValue(closeSeries, 0), .01);
        assertEquals(record1.time(), closeSeries.getDataItem(0).getPeriod());

        // second transaction
        assertEquals(40, getItemValue(bidSeries, 1), .01);
        assertEquals(record2.time(), bidSeries.getDataItem(1).getPeriod());

        // third transaction
        assertEquals(80, getItemValue(askSeries, 0), .01);
        assertEquals(record3.time(), askSeries.getDataItem(0).getPeriod());

        // fourth transaction
        assertQEquals(getItemValue(openSeries, 1), order2.price());
        assertQEquals(getItemValue(closeSeries, 1), order2.price());
        assertEquals(30, getItemValue(bidSeries, 2), .01);
        assertEquals(record4.time(), closeSeries.getDataItem(1).getPeriod());
    }

    private Order makeNewOrder(Position pos, double price, double quant, SecureUser user) {
        return new Order(pos, Price.dollarPrice(price), q(quant), user);
    }

    public void testMoreBookTransactions() {
        Date baseDate = new Date();
        SecureUser taker = new SecureUser("priceTaker", bank.makeFunds(200), "insecure", "j@b.com");

        // 0: add a large buy order .3
        // 1: consume part of it
        // 2: add two small sell orders  .6, .7
        // 3: execute a buy that consumes the lower sell and estalishes a new book order above that price
        // 4: execute a large sell order that consumes both buy orders
        Order order0 = makeNewOrder(weather.getYesPosition(), 30, 25, owner);
        TransactionRec record0 = new TransactionRec(new Date(baseDate.getTime()));
        record0.addBidPriceChange(weather, 30);

        TransactionRec record1 = new TransactionRec();
        record1.addBookTrade(taker, 15, order0);

        Order order2a = makeNewOrder(weather.getNoPosition(), 40, 18, owner);
        Order order2b = makeNewOrder(weather.getNoPosition(), 30, 28, owner);
        TransactionRec record2 = new TransactionRec(new Date(baseDate.getTime() + 2400));
        record2.addAskPriceChange(weather, 60);

        TransactionRec record3 = new TransactionRec(new Date(baseDate.getTime() + 3500));
        record3.addBookTrade(taker, 18, order2a);
        record3.addBidPriceChange(weather, 65);
        record3.addAskPriceChange(weather, 70);
        Order order3 = makeNewOrder(weather.getYesPosition(), 65, 20, owner);

        TransactionRec record4 = new TransactionRec(new Date(baseDate.getTime() + 5300));
        record4.addBookTrade(taker, 20, order3);
        record4.addBookTrade(taker, 10, order0);
        record4.addBidPriceChange(weather, 0);
        record4.addAskPriceChange(weather, 25);

        TimePeriodValuesCollection vals = buildValuesCollection( new TransactionRec[] { record0, record1, record2, record3, record4 } );
        TimePeriodValues openSeries = vals.getSeries(TransactionRec.OPEN);
        TimePeriodValues closeSeries = vals.getSeries(TransactionRec.CLOSE);
        TimePeriodValues bidSeries = vals.getSeries(TransactionRec.BEST_BID);
        TimePeriodValues askSeries = vals.getSeries(TransactionRec.BEST_ASK);
        TimePeriodValues quantSeries = vals.getSeries(TransactionRec.QUANTITY);

        assertEquals(30, getItemValue(bidSeries, 0), .01);
        assertEquals(record0.time(), bidSeries.getDataItem(0).getPeriod());

        assertEquals(record1.time(), openSeries.getDataItem(0).getPeriod());
        assertEquals(30, getItemValue(openSeries, 0), .01);
        assertEquals(record1.time(), closeSeries.getDataItem(0).getPeriod());
        assertEquals(30, getItemValue(closeSeries, 0), .01);
        assertEquals(record1.time(), quantSeries.getDataItem(0).getPeriod());
        assertEquals(15, getItemValue(quantSeries, 0), .01);

        assertEquals(record2.time(), askSeries.getDataItem(0).getPeriod());
        assertEquals(60, getItemValue(askSeries, 0), .01);

        assertEquals(record3.time(), askSeries.getDataItem(1).getPeriod());
        assertEquals(70, getItemValue(askSeries, 1), .01);
        assertEquals(record3.time(), bidSeries.getDataItem(1).getPeriod());
        assertEquals(65, getItemValue(bidSeries, 1), .01);
        assertEquals(record3.time(), openSeries.getDataItem(1).getPeriod());
        assertEquals(60, getItemValue(openSeries, 1), .01);
        assertEquals(record3.time(), closeSeries.getDataItem(1).getPeriod());
        assertEquals(60, getItemValue(closeSeries, 1), .01);
        assertEquals(record3.time(), quantSeries.getDataItem(1).getPeriod());
        assertEquals(18, getItemValue(quantSeries, 1), .01);

        assertEquals(record4.time(), openSeries.getDataItem(2).getPeriod());
        assertQEquals(getItemValue(openSeries, 2), order3.price());
        assertEquals(record4.time(), closeSeries.getDataItem(2).getPeriod());
        assertQEquals(getItemValue(closeSeries, 2), order0.price());
        assertEquals(record4.time(), bidSeries.getDataItem(2).getPeriod());
        assertEquals(0, getItemValue(bidSeries, 2), .01);
        assertEquals(record4.time(), askSeries.getDataItem(2).getPeriod());
        assertEquals(25, getItemValue(askSeries, 2), .01);
        assertEquals(record4.time(), quantSeries.getDataItem(2).getPeriod());
        assertEquals(30, getItemValue(quantSeries, 2), .01);
    }

    private double getItemValue(TimePeriodValues series, int item) {
        return series.getDataItem(item).getValue().doubleValue();
    }

    public void testSimpleMarketMakerTransactions() {
        Order order = makeNewOrder(weather.getYesPosition(), 30, 20, owner);
        SecureUser taker = new SecureUser("priceTaker", bank.makeFunds(200), "insecure", "j@b.com");

        TransactionRec zerothRec = new TransactionRec();
        zerothRec.addMarketTrade(weather.getYesPosition(), taker, 50, 55, 2);

        TransactionRec firstRec = new TransactionRec();
        zerothRec.addMarketTrade(weather.getYesPosition(), taker, 55, 58, 10);

        TransactionRec secondRec = new TransactionRec();
        secondRec.addMarketTrade(weather.getYesPosition(), taker, 58, 48, 50);

        TransactionRec thirdRec = new TransactionRec();
        secondRec.addMarketTrade(weather.getYesPosition(), taker, 48, 53, 25);

        TimePeriodValuesCollection vals = buildValuesCollection( new TransactionRec[] { zerothRec, firstRec, secondRec, thirdRec} );

        TimePeriodValues openSeries = vals.getSeries(TransactionRec.OPEN);
        TimePeriodValues closeSeries = vals.getSeries(TransactionRec.CLOSE);

        assertEquals(50, getItemValue(openSeries, 0), .01);
        assertEquals(55, getItemValue(openSeries, 1), .01);
        assertEquals(58, getItemValue(openSeries, 2), .01);
        assertEquals(48, getItemValue(openSeries, 3), .01);

        assertEquals(55, getItemValue(closeSeries, 0), .01);
        assertEquals(58, getItemValue(closeSeries, 1), .01);
        assertEquals(48, getItemValue(closeSeries, 2), .01);
        assertEquals(53, getItemValue(closeSeries, 3), .01);

        assertEquals(zerothRec.time(), openSeries.getDataItem(0).getPeriod());
        assertEquals(secondRec.time(), closeSeries.getDataItem(2).getPeriod());
    }

    private TimePeriodValuesCollection buildValuesCollection(TransactionRec[] transactionRecs) {
        TimePeriodValuesCollection c = new TimePeriodValuesCollection();
        c.addSeries(new TimePeriodValues("Open"));
        c.addSeries(new TimePeriodValues("Close"));
        c.addSeries(new TimePeriodValues("Best Bid"));
        c.addSeries(new TimePeriodValues("Best Ask"));
        c.addSeries(new TimePeriodValues("Quantity"));
        for (int i = 0; i < transactionRecs.length; i++) {
            TransactionRec rec = transactionRecs[i];
            rec.addTransactionsTo(c);
        }
        return c;
    }
}
