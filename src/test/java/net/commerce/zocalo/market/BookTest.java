package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.currency.CouponBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.ajax.events.*;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.math.BigDecimal;

public class BookTest extends OrdersTestCase {
    private CouponBank couponBank;

    static {
        Log4JHelper.getInstance();
    }

    protected void setUp() throws Exception {
        super.setUp();
        couponBank = CouponBank.makeBank(claim, rootBank.noFunds());
        HibernateTestUtil.resetSessionFactory();
    }

    public void testAddBookOrder() throws DuplicateOrderException, IncompatibleOrderException {
        assertTrue(book.bestBuyOfferFor(yes).isZero());
        assertTrue(book.bestBuyOfferFor(no).isZero());
        addOrder(book, yes, "37", 20, makeUser("buyer1"));
        addOrder(book, yes, "40", 10, makeUser("buyer2"));
        assertQEquals(40, book.bestBuyOfferFor(yes));
        addOrder(book, no, "35", 10, makeUser("seller1"));
        addOrder(book, no, "30", 10, makeUser("seller1"));
        assertQEquals(35, book.bestBuyOfferFor(no));
    }

    public void testConsolidateOrders() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "37", 20, makeUser("buyer1"));
        addOrder(book, yes, "40", 15, makeUser("buyer2"));
        addOrder(book, yes, "40", 25, makeUser("buyer3"));
        assertQEquals(40, book.bestBuyOfferFor(yes));
        assertQEquals(40, book.bestQuantity(yes));

        addOrder(book, no, "20", 10, makeUser("seller1"));
        assertQEquals(80, book.bestSellOfferFor(yes));
        assertQEquals(10, book.bestQuantity(no));
        addOrder(book, no, "40", 30, makeUser("seller1"));
        assertQEquals(60, book.bestSellOfferFor(yes));
        assertQEquals(30, book.bestQuantity(no));

        assertQEquals(40, book.bestQuantity(yes));
    }

    public void testRemoveBookOrders() throws DuplicateOrderException, IncompatibleOrderException {
        User buyer1 = makeUser("buyer1");
        User buyer2 = makeUser("buyer2");
        assertEquals(0, buyer1.getOrders().size());
        assertEquals(0, buyer2.getOrders().size());

        addOrder(book, yes, "37", 20, buyer1);
        assertEquals(1, buyer1.getOrders().size());

        Order buyer2Order = addOrder(book, yes, "40", 10, buyer2);
        assertEquals(1, buyer2.getOrders().size());
        assertQEquals(40, book.bestBuyOfferFor(yes));

        book.removeOrder(buyer2Order);
        assertEquals(0, buyer2.getOrders().size());

        assertQEquals(37, book.bestBuyOfferFor(yes));
        book.removeOrder(buyer1.getName(), Price.dollarPrice("37"), yes);
        assertEquals(0, buyer1.getOrders().size());

        assertTrue(book.bestBuyOfferFor(yes).isZero());
        assertEquals(false, book.removeOrder(buyer1.getName(), Price.dollarPrice("30"), yes));
    }

    public void testRemoveBookOrdersLargerPriceRange() throws DuplicateOrderException, IncompatibleOrderException {
        Price largeRange = new Price(new Quantity("400"));
        User buyer1 = makeUser("buyer1");
        User buyer2 = makeUser("buyer2");
        assertEquals(0, buyer1.getOrders().size());
        assertEquals(0, buyer2.getOrders().size());

        BigDecimal p = new BigDecimal(400 - 255);
        book.addOrder(yes, new Price(p, largeRange), q(20), buyer1);    // Double gets the wrong answer
        assertEquals(1, buyer1.getOrders().size());

        Order buyer2Order = book.addOrder(yes, new Price("161", largeRange), q(10), buyer2);
        assertEquals(1, buyer2.getOrders().size());
        assertQEquals(161, book.bestBuyOfferFor(yes));

        book.removeOrder(buyer2Order);
        assertEquals(0, buyer2.getOrders().size());

        BigDecimal p145 = new BigDecimal(145);
        book.removeOrder(buyer1.getName(), new Price(p145, largeRange), yes);
        assertEquals(0, buyer1.getOrders().size());

        assertTrue(book.bestBuyOfferFor(yes).isZero());
        assertEquals(false, book.removeOrder(buyer1.getName(), new Price("300", largeRange), yes));
    }

    public void testAddDuplicateOrders() throws DuplicateOrderException, IncompatibleOrderException {
        User buyer1 = makeUser("buyer1");
        User buyer2 = makeUser("buyer2");
        addOrder(book, yes, "37", 20, buyer1);
        addOrder(book, yes, "37", 20, buyer2);
        assertQEquals(40, book.bestQuantity(yes));

        try {
            addOrder(book, yes, "37", 20, buyer1);
            fail("expected to throw a DuplicateOrderException");
        } catch (DuplicateOrderException e) {
            assertQEquals(40, book.bestQuantity(yes));
        }

        addOrder(book, yes, "32", 20, buyer1);
        assertQEquals(40, book.bestQuantity(yes));
    }

    public void testTrade() throws DuplicateOrderException, IncompatibleOrderException {
        User buyer = makeUser("buyer1");
        addOrder(book, yes, "35", 25, buyer);
        User seller = makeUser("seller1");
        assertQEquals(10, buyFromBookOrders(book, no, "65", 10, seller));
        assertQEquals(15, book.bestQuantity(yes));
    }

    public void testConsumateTrade() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "47", 20, makeUser("buyer1"));
        addOrder(book, yes, "37", 20, makeUser("buyer2"));
        addOrder(book, no, "47", 20, makeUser("seller1"));
        assertQEquals(20, book.bestQuantity(yes));
    }

    public void testReturnValueOfTrade() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "30", 50, makeUser("buyerA"));
        assertQEquals(30, book.bestBuyOfferFor(yes));

        Quantity amountSold = buyFromBookOrders(book, no, "70", 20, makeUser("sellerB"));
        assertQEquals(20, amountSold);
        assertQEquals(30, book.bestQuantity(yes));
        assertTrue(book.bestBuyOfferFor(no).isZero());
        assertQEquals(70, book.bestSellOfferFor(no));
    }

    public void testConsumateMultipleTradesAtOnce() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "30", 40, makeUser("buyerA"));
        addOrder(book, yes, "30", 15, makeUser("buyerB"));
        Quantity amountSold = buyFromBookOrders(book, no, "70", 50, makeUser("sellerC"));
        assertQEquals(50, amountSold);
        assertQEquals(5, book.bestQuantity(yes));
    }

    public void testTradeVsSelf() throws DuplicateOrderException, IncompatibleOrderException {
        double initialCash = 1000.0;
        User buyerA = makeUser("buyerA", initialCash);
        addOrder(book, yes, "30", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "70", 30, buyerA);
        assertQEquals(30, amountSold);
        assertTrue(book.bestQuantity(yes).isZero());
        assertQEquals(initialCash, buyerA.cashOnHand());
    }

    public void testDifferentTradeVsSelf() throws DuplicateOrderException, IncompatibleOrderException {
        double initialCash = 1000.0;
        User buyerA = makeUser("buyerA", initialCash);
        addOrder(book, yes, "60", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "40", 30, buyerA);
        assertQEquals(30, amountSold);
        assertTrue(book.bestQuantity(yes).isZero());
        assertQEquals(initialCash, buyerA.cashOnHand());
    }

    public void testTradeVsSelfAtTwoPrices() throws DuplicateOrderException, IncompatibleOrderException {
        double initialCash = 1000.0;
        User buyerA = makeUser("buyerA", initialCash);
        addOrder(book, yes, "30", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "80", 30, buyerA);
        assertQEquals(30, amountSold);
        assertTrue(book.bestQuantity(yes).isZero());
        assertQEquals(initialCash, buyerA.cashOnHand());
    }

    public void testSecondTradeBiggerThanFirst() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        addOrder(book, yes, "30", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "80", 50, buyerB);
        assertQEquals(40, amountSold);
        assertTrue(book.bestQuantity(yes).isZero());
        assertTrue(book.bestQuantity(no).isZero());
    }

    public void testAcceptAtWorsePrice() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        limitOrder(market, yes, "30", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "80", 50, buyerB);
        assertQEquals(40, amountSold);
        assertQEquals(100000 - (40 * 30), buyerA.cashOnHand());
        assertQEquals(100000 - (40 * 70), buyerB.cashOnHand());
        assertQEquals(0, book.bestQuantity(no));
    }

    public void testTraderCannotAffordOrder() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB", 200);
        limitOrder(market, yes, "30", 40, buyerA);
        Quantity amountSold = buyFromBookOrders(book, no, "80", 50, buyerB);
        assertTrue(amountSold.isPositive());
        assertTrue(book.bestQuantity(yes).compareTo(q(20)) > 0);
        assertTrue(book.bestQuantity(yes).compareTo(q(40)) < 0);
        assertTrue(book.bestQuantity(no).isZero());
    }

    public void testUnableToAddConflictingOrder() throws DuplicateOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB", 10);
        limitOrder(market, yes, "60", 40, buyerA);
        assertQEquals(40, book.bestQuantity(yes));

        try {
            book.addOrder(no, Price.dollarPrice(50), q(30), buyerB);
            fail("shouldn't be able to add the order");
        } catch (IncompatibleOrderException e) {
            // NOOP
        }
        assertQEquals(40, book.bestQuantity(yes));
        assertQEquals(0, book.bestQuantity(no));
    }

    public void testTradesOnlyAgainstBestPrice() throws DuplicateOrderException {
        Market m = BinaryMarket.make(owner, claim, rootBank.noFunds());
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User buyerC = makeUser("buyerC");
        User seller = makeUser("seller");

        limitOrder(m, yes, "45", 50, buyerA);
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));

        limitOrder(m, yes, "55", 20, buyerB);
        limitOrder(m, yes, "55", 30, buyerC);
        assertQEquals(50, m.getBook().bestQuantity(yes));
        assertQEquals(55, m.getBook().bestBuyOfferFor(yes));

        limitOrder(m, no, "50", 200, seller);

        assertQEquals(55, m.getBook().bestSellOfferFor(no));
        assertQEquals(50, m.getBook().bestQuantity(yes));
        assertQEquals(150, m.getBook().bestQuantity(no));
    }

    public void testTradesAgainstAllOrders() throws DuplicateOrderException {
        Market m = BinaryMarket.make(owner, claim, rootBank.noFunds());
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User buyerC = makeUser("buyerC");
        User seller = makeUser("seller");

        limitOrder(m, yes, "45", 50, buyerA);
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));

        limitOrder(m, yes, "55", 20, buyerB);
        limitOrder(m, yes, "55", 30, buyerC);
        assertQEquals(50, m.getBook().bestQuantity(yes));
        assertQEquals(55, m.getBook().bestBuyOfferFor(yes));

        limitOrder(m, no, "60", 200, seller);

        assertQEquals(Quantity.Q100, m.getBook().bestSellOfferFor(no));
        assertTrue(m.getBook().bestQuantity(yes).isZero());
        assertQEquals(40, m.getBook().bestSellOfferFor(yes));
        assertQEquals(Quantity.Q100, m.getBook().bestQuantity(no));
    }

    public void testLogActions() throws DuplicateOrderException {
        Market m = BinaryMarket.make(owner, claim, rootBank.noFunds());
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);

        assertEquals(0, appender.messageCount());
        limitOrder(m, yes, "35", 50, makeUser("buyer"));
        assertEquals(2, appender.messageCount());

        List<Class> prices0 = new ArrayList<Class>();
        for (Iterator iterator = appender.getEvents().iterator(); iterator.hasNext();) {
            PriceAction event = (PriceAction) iterator.next();
            prices0.add(event.getClass());
        }
        ArrayList<Class> expected0 = new ArrayList<Class>();
        expected0.add(Bid.class);
        expected0.add(BestBid.class);
        assertTrue(prices0.containsAll(expected0));

        limitOrder(m, no, "60", 200, makeUser("seller"));
        assertEquals(4, appender.messageCount());

        List<Class> prices1 = new ArrayList<Class>();
        for (Iterator iterator = appender.getEvents().iterator(); iterator.hasNext();) {
            PriceAction event = (PriceAction) iterator.next();
            prices1.add(event.getClass());
        }
        ArrayList<Class> expected1 = new ArrayList<Class>();
        expected1.add(Bid.class);
        expected1.add(BestBid.class);
        expected1.add(Ask.class);
        expected1.add(BestAsk.class);
        assertTrue(prices1.containsAll(expected1));

        limitOrder(m, no, "70", 200, makeUser("seller"));
        assertEquals(8, appender.messageCount());

        List<Class> prices2 = new ArrayList<Class>();
        for (Iterator iterator = appender.getEvents().iterator(); iterator.hasNext();) {
            PriceAction event = (PriceAction) iterator.next();
            prices2.add(event.getClass());
        }
        ArrayList<Class> expected2 = new ArrayList<Class>();
        expected2.add(Bid.class);
        expected2.add(BestBid.class);
        expected2.add(Ask.class);
        expected2.add(BestAsk.class);
        expected2.add(BookTrade.class);
        expected2.add(Ask.class);
        expected2.add(BestAsk.class);
        assertTrue(prices2.containsAll(expected2));
    }

    public void testThreeTraders() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User buyerC = makeUser("buyerC");
        User placeHolder = makeUser("placeHolder");

        assignCouponsTo(q(20), buyerA, buyerC);
        assignCouponsTo(q(20), buyerB, placeHolder);

        addOrder(book, yes, "50", 1, buyerA);
        buyFromBookOrders(book, no, "50", 1, buyerB);

        assertQEquals(100000 - 50, buyerA.cashOnHand());
        assertQEquals(100000 + 50, buyerB.cashOnHand());
        assertQEquals(19, buyerB.couponCount(claim));
        assertQEquals(21, buyerA.couponCount(claim));

        addOrder(book, no, "50", 1, buyerA);
        buyFromBookOrders(book, yes, "50", 1, buyerB);

        assertQEquals(100000, buyerA.cashOnHand());
        assertQEquals(100000, buyerB.cashOnHand());
        assertQEquals(20, buyerA.couponCount(claim));
        assertQEquals(20, buyerB.couponCount(claim));

        assertQEquals(-20, buyerC.couponCount(claim));
        assertQEquals(100000, buyerC.cashOnHand());
        addOrder(book, yes, "30", 1, buyerA);
        buyFromBookOrders(book, no, "70", 1, buyerC);

        assertQEquals(100000 - 30, buyerA.cashOnHand());
        assertQEquals(100000 - 70, buyerC.cashOnHand());

        assertQEquals(21, buyerA.couponCount(claim));
        assertQEquals(-21, buyerC.couponCount(claim));

        addOrder(book, yes, "30", 1, buyerC);
        buyFromBookOrders(book, no, "70", 1, buyerA);

        assertQEquals(100000, buyerA.cashOnHand());
        assertQEquals(100000, buyerC.cashOnHand());

        assertQEquals(20, buyerA.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        assertQEquals(100000, buyerB.cashOnHand());
        assertQEquals(20, buyerB.couponCount(claim));
    }

    public void testThreeTrades() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User buyerC = makeUser("buyerC");
        Quantity q20 = q(20);
        buyerA.endow(couponBank.issueUnpairedCoupons(q20, yes));
        buyerB.endow(couponBank.issueUnpairedCoupons(q20, yes));
        buyerC.endow(couponBank.issueUnpairedCoupons(q20, no));

        assertQEquals(20, buyerA.couponCount(claim));
        assertQEquals(20, buyerB.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        addOrder(book, yes, "20", 1, buyerA);
        addOrder(book, yes, "30", 1, buyerA);
        addOrder(book, yes, "40", 1, buyerA);
        addOrder(book, no, "10", 1, buyerB);
        addOrder(book, no, "20", 1, buyerB);
        buyFromBookOrders(book, no, "60", (double)1, buyerB);
        buyFromBookOrders(book, no, "70", 1, buyerB);
        addOrder(book, yes, "40", 1, buyerC);
        buyFromBookOrders(book, yes, "80", 1, buyerC);

        assertQEquals(22, buyerA.couponCount(claim));
        assertQEquals(17, buyerB.couponCount(claim));
        assertQEquals(-19, buyerC.couponCount(claim));
    }

    public void testBuyingFromTwoOrders() throws DuplicateOrderException, IncompatibleOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        Quantity q100 = Quantity.Q100;
        buyerA.endow(couponBank.issueUnpairedCoupons(q100, yes));
        buyerB.endow(couponBank.issueUnpairedCoupons(q100, yes));

        assertQEquals(100000, buyerA.cashOnHand());
        assertQEquals(q100, buyerA.couponCount(claim));
        assertQEquals(100000, buyerB.cashOnHand());
        assertQEquals(q100, buyerB.couponCount(claim));

        addOrder(book, yes, "45", 51, buyerA);
        addOrder(book, yes, "46", 51, buyerA);
        addOrder(book, yes, "50", 10, buyerA);
        assertQEquals(10, book.bestQuantity(yes));
        assertQEquals(50, book.bestBuyOfferFor(yes));

        buyFromBookOrders(book, no, "65", 11, buyerB);

        assertQEquals(100000 - (10 * 50) - 46, buyerA.cashOnHand());
        assertQEquals(100 + 11, buyerA.couponCount(claim));
        assertQEquals(100000 + (10 * 50) + 46, buyerB.cashOnHand());
        assertQEquals(100 - 11, buyerB.couponCount(claim));

        assertQEquals(50, book.bestQuantity(yes));
        assertQEquals(46, book.bestBuyOfferFor(yes));
    }

    public void testLimitOrders() throws DuplicateOrderException {
        Market m = BinaryMarket.make(owner, claim, rootBank.noFunds());
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User seller = makeUser("seller");

        limitOrder(m, yes, "25", 50, buyerA);
        limitOrder(m, yes, "35", 50, buyerA);
        limitOrder(m, yes, "45", 50, buyerA);
        limitOrder(m, no, "20", 50, seller);
        limitOrder(m, no, "30", 50, seller);
        limitOrder(m, no, "40", 50, seller);
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(40, m.getBook().bestBuyOfferFor(no));

        marketOrder(m, yes, "60", 50, buyerB);
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));

        marketOrder(m, yes, "65", 50, buyerB);
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));

        marketOrder(m, no, "50", 50, seller);
        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));

        marketOrder(m, no, "60", 50, seller);
        assertQEquals(35, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));
    }
}
