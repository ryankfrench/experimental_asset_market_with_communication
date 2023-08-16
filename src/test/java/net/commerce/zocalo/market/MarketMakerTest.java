package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.ajax.events.PriceChange;
import net.commerce.zocalo.ajax.dispatch.MockBayeux;
import net.commerce.zocalo.ajax.dispatch.PriceChangeAppender;
import net.commerce.zocalo.ajax.dispatch.MockBayeuxChannel;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.claim.Position;
//JJDM import net.commerce.zocalo.service.AllMarkets;
import org.apache.log4j.Logger;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class MarketMakerTest extends OrdersTestCase {
    private MarketMaker mm;
    private final int SOME_COUPONS = 100;

    protected void setUp() throws Exception {
        super.setUp();
        mm = ((BinaryMarket)market).makeMarketMaker(owner, q(100000));
        HibernateTestUtil.resetSessionFactory();
    }

    public void testPriceMovementsSymmetric() {
        final Quantity endowment = q(100000);
        User janet = makeUser("janet", endowment);
        User ace = makeUser("ace", endowment);

        buyUpToQuantity(mm, yes, "0.65", SOME_COUPONS, janet);
        assertTrue(endowment.compareTo(janet.cashOnHand()) > 0);
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) < 0);
        assertQIsClose(SOME_COUPONS, janet.couponCount(yes), .1);
        Probability probAfterBuy = mm.currentProbability(yes);
        buyUpToQuantity(mm, no, "0.50", 150, janet);
        assertQEquals(janet.cashOnHand(), ace.cashOnHand());

        buyUpToQuantity(mm, no, "0.65", SOME_COUPONS, ace);
        assertTrue(endowment.compareTo(ace.cashOnHand()) > 0);
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) > 0);
        assertQEquals(SOME_COUPONS, ace.couponCount(no));
        assertQEquals(probAfterBuy, mm.currentProbability(no));
        buyUpToQuantity(mm, yes, "0.50", 150, ace);

        assertQEquals(0.5, market.currentProbability(yes));
        assertQEquals(janet.cashOnHand(), ace.cashOnHand());
    }

    public void testPriceComputation() {
        User janet = makeUser("janet");
        buyUpToQuantity(mm, yes, "0.55", 500, janet);
        assertQEquals(0.55, mm.currentProbability(yes));
    }

    public void testPriceMovesAtConstantRateWithFixedQuantity() {
        User janet = makeUser("Janet");
        buyUpToQuantity(mm, yes, "0.95", 50, janet);
        Probability firstEstimate = market.currentProbability(yes);
        buyUpToQuantity(mm, yes, "0.95", 50, janet);
        Probability secondEstimate = market.currentProbability(yes);
        assertQEquals(odds(firstEstimate).div(odds(Probability.HALF)), odds(secondEstimate).div(odds(firstEstimate)));
        buyUpToQuantity(mm, yes, "0.95", 50, janet);
        Probability thirdEstimate = market.currentProbability(yes);
        assertQEquals(odds(secondEstimate).div(odds(firstEstimate)), odds(thirdEstimate).div(odds(secondEstimate)));
    }

    public void testPriceMovesAtConstantRateWithFixedOddsChange() {
        SecureUser marketOwner = makeSecureUser("chris", 200);
        BinaryMarket market = BinaryMarket.make(owner, claim, rootBank.noFunds());
        MarketMaker maker = market.makeMarketMaker(marketOwner, q(150));

        User bill = makeUser("bill");
        User janet = makeUser("janet");
        User sue = makeUser("sue");
        User jon = makeUser("jon");
        buyUpToQuantity(maker, no, "0.666667", 500, bill);
        Probability oneToTwo = market.currentProbability(yes);
        assertQEquals(.333333, oneToTwo);

        buyUpToQuantity(maker, yes, "0.5", 500, janet);
        Probability even = market.currentProbability(yes);
        assertQEquals(.5, even);

        buyUpToQuantity(maker, yes, "0.666667", 500, sue);
        Probability twoToOne = market.currentProbability(yes);
        assertQEquals(.6666667, twoToOne);

        buyUpToQuantity(maker, yes, "0.8", 500, jon);
        Probability fourToOne = market.currentProbability(yes);
        assertQEquals(.8, fourToOne);
        assertQEquals(odds(even).div(odds(oneToTwo)), odds(twoToOne).div(odds(even)));
        assertQEquals(odds(fourToOne).div(odds(twoToOne)), odds(twoToOne).div(odds(even)));

        assertQEquals(janet.couponCount(yes), bill.couponCount(no));
        assertQEquals(janet.couponCount(yes), sue.couponCount(yes));
        assertQEquals(jon.couponCount(yes), sue.couponCount(yes));
    }

    public void testHowMuchMoneyMovesPriceFrom50to99() {
        SecureUser marketOwner = makeSecureUser("chris");
        BinaryMarket market = BinaryMarket.make(owner, claim, rootBank.noFunds());
        MarketMaker maker = market.makeMarketMaker(marketOwner, q(100000));

        User bill = makeUser("bill");
        buyUpToQuantity(maker, yes, "0.99", 1000, bill);
        assertQIsClose(100000 - 57000, bill.cashOnHand(), 2000);
//        assertEquals(1000 - 570, bill.cashOnHand(), 10);
    }

    private Quantity odds(Probability estimate) {
        return estimate.div(estimate.inverted());
    }

    public void testBuyingFromBookAndMarket() {
        User buyer = makeUser("buyer");
        buyUpToQuantity(mm, yes, "0.55", SOME_COUPONS, buyer);
        Probability pNo = market.currentProbability(no);
        assertTrue("if yes decreases, no should increase: " + pNo, q(0.45).compareTo(pNo) < 0);
        assertTrue("p should be more than .5: " + market.currentProbability(yes), q(0.5).compareTo(market.currentProbability(no)) > 0);
        assertQEquals(SOME_COUPONS, buyer.couponCount(yes));

        buyUpToQuantity(mm, no, "0.55", SOME_COUPONS, buyer);
        assertQEquals(0.5, market.currentProbability(no));
        assertQEquals(100000, buyer.cashOnHand());

        User ace = makeUser("ace", 500000);
        buyUpToQuantity(mm, yes, "0.60", 300, ace);
        assertTrue(q(5000 - 300).compareTo(ace.cashOnHand()) <= 0);
        assertTrue(q(500000).compareTo(ace.cashOnHand()) > 0);
        assertQEquals(300, ace.couponCount(yes));
        assertTrue(q(.6).compareTo(market.currentProbability(yes)) > 0);
        assertTrue(q(.5).compareTo(market.currentProbability(yes)) < 0);

        User nick = makeUser("nick", 500000);
        buyUpToQuantity(mm, no, "0.60", 1000, nick);
        assertQEquals(0.40, market.currentProbability(yes));
        assertTrue(nick.couponCount(no).isPositive());
        assertTrue(q(500000).compareTo(nick.cashOnHand()) > 0);
    }

    public void testBuyingFromMarketMaker() {
        User bookBuyer = makeUser("book buyer");
        buyUpToQuantity(mm, yes, "0.65", 10, bookBuyer);
        assertTrue(q(0.65).compareTo(market.currentProbability(yes)) > 0);
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) <= 0);

        buyUpToQuantity(mm, yes, "0.65", 1000, bookBuyer);
        assertQEquals(0.65, market.currentProbability(yes));

        User ace = makeUser("ace", 500000);
        buyUpToQuantity(mm, yes, "0.70", 30000, ace);
        assertQIsClose(0.70, market.currentProbability(yes), 0.01);
    }

    public void testNotBuyingFromMarketMaker() throws DuplicateOrderException {
        User bookNonBuyer = makeUser("book non-buyer");
        makeLimitOrder(market, yes, 45, 10, bookNonBuyer);
        assertQEquals(0.5, market.currentProbability(yes));
        assertQEquals(45, market.getBook().bestBuyOfferFor(yes));

        User bookNonSeller = makeUser("book non-seller");
        makeLimitOrder(market, no, 45, 10, bookNonSeller);
        assertQEquals(0.5, market.currentProbability(no));
        assertQEquals(45, market.getBook().bestBuyOfferFor(no));
    }

    public void testBuyingBackAndForth() throws DuplicateOrderException {
        final double endowment = 100000;
        User trader = makeUser("trader", endowment);
        makeLimitOrder(market, yes, 65, 30, trader);
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) < 0);
        assertQIsClose(endowment - 1500, trader.cashOnHand(), 100);
        assertFalse(market.getBook().hasOrdersToSell(yes));
        assertFalse(market.getBook().hasOrdersToSell(no));

        makeLimitOrder(market, no, 50, 50, trader);
        assertQEquals(0.5, market.currentProbability(no));
        assertQEquals(endowment, trader.cashOnHand());
        assertQEquals(50, market.getBook().bestBuyOfferFor(no));
        assertTrue(market.getBook().hasOrdersToSell(yes));
        assertFalse(market.getBook().hasOrdersToSell(no));
    }

    public void testSpecifySpendingLimit() {
        final double endowment = 100000;
        final int limit = 10000;
        User janet = makeUser("janet", endowment);
        User ace = makeUser("ace", endowment);

        buyWithCostLimit(mm, yes, 0.65, limit, janet);
        assertQEquals(endowment - limit, janet.cashOnHand());
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) < 0);
        Quantity janetCoupons = janet.couponCount(yes);
        assertTrue(q(150).compareTo(janetCoupons) < 0);

        buyWithCostLimit(mm, no, 0.50, 15000, janet);
        assertQEquals(janet.cashOnHand(), ace.cashOnHand());
        assertTrue(janet.couponCount(no).isZero());

        buyWithCostLimit(mm, no, 0.65, limit, ace);
        assertQEquals(endowment - limit, ace.cashOnHand());
        assertTrue(q(0.5).compareTo(market.currentProbability(no)) < 0);
        assertQEquals(janetCoupons, ace.couponCount(no));

        buyWithCostLimit(mm, yes, 0.50, 15000, ace);
        assertQEquals(0.5, market.currentProbability(yes));
        assertQEquals(janet.cashOnHand(), ace.cashOnHand());
    }

    private void buyWithCostLimit(MarketMaker maker, Position pos, double probability, int limit, User user) {
        maker.buyWithCostLimit(pos, new Probability(probability), q(limit), user);
    }

    public void testLogActions() {
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);
        User bookBuyer = makeUser("book buyer");

        buyUpToQuantity(mm, yes, "0.65", 10, bookBuyer);
        assertEquals(1, appender.messageCount());
    }

    public void testLogSelfDealing() throws DuplicateOrderException {
        MockAppender appender = new MockAppender();
        Logger.getLogger(PriceAction.class).addAppender(appender);
        User bookBuyer = makeUser("self dealer");

        makeLimitOrder(market, yes, 50, 10, bookBuyer);
        assertQEquals(50, market.getBook().getOffers(yes).bestPrice());
        assertEquals(2, appender.messageCount());
        makeLimitOrder(market, no, 50, 10, bookBuyer);
        assertTrue(market.getBook().getOffers(yes).bestPrice().isZero());
        assertEquals(3, appender.messageCount());
    }

    public void testMarketMakerSufficientFunds() {
        final Quantity endowment = q(10000.0);
        SecureUser anotherOwner = makeSecureUser("anOwner", endowment);
        BinaryMarket shortMarket = BinaryMarket.make(owner, claim, rootBank.noFunds());
        MarketMaker shortMaker = ((BinaryMarket) shortMarket).makeMarketMaker(anotherOwner, Quantity.Q100);

        User janet = makeUser("janet", endowment);

        buyUpToQuantity(shortMaker, yes, "0.99", 6, janet);
        assertTrue(endowment.compareTo(janet.cashOnHand()) > 0);
        assertTrue(q(0.5).compareTo(shortMarket.currentProbability(yes)) < 0);
        assertQEquals(6, janet.couponCount(yes));
        buyUpToQuantity(shortMaker, no, "0.50", 150, janet);
        assertQEquals(endowment, janet.cashOnHand());
    }

    public void testSellingInsufficientCoupons() {
        final Quantity endowment = q(1000000.0);
        SecureUser anotherOwner = makeSecureUser("anOwner", endowment);
        BinaryMarket market = BinaryMarket.make(owner, claim, rootBank.noFunds());
        MarketMaker maker = ((BinaryMarket) market).makeMarketMaker(anotherOwner, q(1000));
        User janet = makeUser("janet", endowment);

        buyUpToQuantity(maker, yes, "0.99", 65, janet);
        assertTrue(endowment.compareTo(janet.cashOnHand()) > 0);
        assertTrue(q(0.5).compareTo(market.currentProbability(yes)) < 0);
        assertTrue(q(0.9).compareTo(market.currentProbability(yes)) < 0);
        assertQEquals(65, janet.couponCount(yes));
        buyUpToQuantity(maker, no, "0.60", 150, janet);
        assertQEquals(.6, market.currentProbability(no));
        assertTrue(endowment.compareTo(janet.cashOnHand()) > 0);
    }

    public void testMarketOwnerInsufficientFunds() {
        SecureUser shortOwner = makeSecureUser("shortie", 50);
        BinaryMarket shortMarket = BinaryMarket.make(owner, claim, rootBank.noFunds());
        assertNull(((BinaryMarket) shortMarket).makeMarketMaker(shortOwner, q(150)));
    }

    public void testPriceChangeEvents() {
        MockBayeux mockBayeux = new MockBayeux();
        String uri = "foo"; // JJDM AllMarkets.buildChannelName("foo");
        MockBayeuxChannel channel = (MockBayeuxChannel) mockBayeux.getChannel(uri, true);
        PriceChangeAppender.registerNewAppender(mockBayeux, "foo");
        Hashtable<Position, Probability> update = new Hashtable<Position, Probability>() {{
            put(yes, new Probability(".37"));
            put(no, new Probability(".63"));
        }};
        new PriceChange("foo", update);
        List list = channel.getEvents(uri);
        assertEquals(1, list.size());
        Object event =  list.iterator().next();
        assertTrue(event instanceof Map);
        Map e = (Map) event;
        assertEquals("37", e.get("yes"));
        assertEquals("63", e.get("no"));
    }
}
