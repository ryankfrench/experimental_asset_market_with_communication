package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.*;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public final class MultiMarketMakerTest extends OrdersTestCase {
    private MultiMarketMaker mm;
    private MultiClaim mClaim;
    private MultiMarket m;
    private Position rain;
    private Position snow;
    private Position sun;
    private Probability qOneThird = new Probability(Quantity.ONE.div(new Quantity(3.0)));
    private Quantity q1000 = new Quantity("1000");
    private Quantity q100 = Quantity.Q100;

    protected void setUp() throws Exception {
        super.setUp();
        mClaim = MultiClaim.makeClaim("weather", owner, "tomorrow's weather", new String[]{"rain", "snow", "sun"});
        m = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        rain = mClaim.lookupPosition("rain");
        snow = mClaim.lookupPosition("snow");
        sun = mClaim.lookupPosition("sun");
        mm = m.makeMarketMaker(q(100000), owner);
        market = m;
        HibernateTestUtil.resetSessionFactory();
    }

    public void testPriceMovementsSymmetric() {
        final Quantity endowment = q(100000);
        User janet = makeUser("janet", endowment);
        User ace = makeUser("ace", endowment);
        Probability oneThird = market.currentProbability(rain);

        // janet buy rain upward from 1/3
        buyUpToProbQ(mm, janet, "0.65", 100, rain);
        Probability probAfterBuy = mm.currentProbability(rain);
        assertTrue(probAfterBuy + " should be greater than 1/3", qOneThird.compareTo(probAfterBuy) < 0);

        // Ace buy rain back down to 1/3
        mm.buyOrSellUpToQuantity(rain, new Price(30, Price.ONE_DOLLAR), q100, ace);
        assertQApproaches(qOneThird, market.currentProbability(rain));

        //janet and ace have similar amounts of cash and holdings
        assertEquals(0, janet.couponCount(rain).compareTo(ace.couponCount(sun)));
        assertTrue(endowment.compareTo(janet.cashOnHand()) > 0);
        assertTrue(endowment.compareTo(ace.cashOnHand()) > 0);
//        Quantity expected = new Quantity("2").times(endowment).minus(new Quantity("100"));
//        assertQApproaches(expected, janet.cashOnHand().plus(ace.cashOnHand()));
        assert(probAfterBuy.equals(mm.currentProbability(rain)));
    }

    private void buyOrSellUpToPriceQ(MultiMarketMaker maker, User user, Position pos, String price, double quant) {
        maker.buyOrSellUpToQuantity(pos, new Price(price, Price.ONE_DOLLAR), q(quant), user);
    }

    private void buyUpToProbQ(MultiMarketMaker maker, User u, String prob, int quant, Position pos) {
        maker.buyUpToQuantity(pos, new Probability(new BigDecimal(prob)), new Quantity(quant), u);
    }

    public void testPriceComputation() {
        User janet = makeUser("janet");
        buyUpToProbQ(mm, janet, "0.55", 2000, rain);
        assertQEquals(0.55, mm.currentProbability(rain));
    }

    public void testPriceMovesAtConstantRateWithFixedQuantity() {
        User janet = makeUser("Janet");
        buyUpToProbQ(mm, janet, "0.95", 50, rain);
        Probability firstEstimate = market.currentProbability(rain);
        buyUpToProbQ(mm, janet, "0.95", 50, rain);
        Probability secondEstimate = market.currentProbability(rain);
        assertEquals(0, oddsRatio(firstEstimate, qOneThird).compareTo(oddsRatio(secondEstimate, firstEstimate)));
        buyUpToProbQ(mm, janet, "0.95", 50, rain);
        Probability thirdEstimate = market.currentProbability(rain);
        assertEquals(0, oddsRatio(secondEstimate, firstEstimate).compareTo(oddsRatio(thirdEstimate, secondEstimate)));
    }

    private Quantity oddsRatio(Probability p1, Probability p2) {
        return odds(p1).div(odds(p2));
    }

    public void testInsufficientFunds() throws DuplicateOrderException {
        User shortOwner = rootBank.makeEndowedUser("shortie", new Quantity("10"));
        MultiClaim claim = MultiClaim.makeClaim("hockey", shortOwner, "Stanley Cup Winner", new String[]{"sharks", "oilers", "redwings"});
        MultiMarket mkt = MultiMarket.make(owner, claim, rootBank.noFunds());
        MultiMarketMaker maker = mkt.makeMarketMaker(q1000, shortOwner);

        assertNull(maker);
    }

    public void testPriceMovesAtConstantRateWithFixedOddsChange() {
        User marketOwner = makeUser("chris");
        MultiMarket market = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        MarketMaker maker = market.makeMarketMaker(q100, marketOwner);

        User janet = makeUser("janet");
        User sue = makeUser("sue");
        User jon = makeUser("jon");
        Probability oneToTwo = market.currentProbability(rain);
        assertEquals(0, qOneThird.compareTo(oneToTwo));

        Probability even = new Probability(".5");
        maker.buyUpToQuantity(rain, even, q1000, janet);
        assertEquals(0, even.compareTo(market.currentProbability(rain)));

        Probability twoToOne = new Probability(qOneThird.times(q(2))) ;
        maker.buyUpToQuantity(rain, twoToOne, q1000, sue);
        assertEquals(0, twoToOne.compareTo(market.currentProbability(rain)));

        Probability fourToOne = new Probability(".8");
        maker.buyUpToQuantity(rain, fourToOne, q1000, jon);
        assertQEquals(fourToOne, market.currentProbability(rain));
        assertQEquals(oddsRatio(even, oneToTwo), (oddsRatio(twoToOne, even)));
        assertQEquals(oddsRatio(fourToOne, twoToOne), oddsRatio(twoToOne, even));

        assertQEquals(janet.couponCount(rain), sue.couponCount(rain));
        assertQEquals(jon.couponCount(rain), sue.couponCount(rain));
    }

    public void testOpposingPricesMoveTogether() {
        User chris = makeUser("chris");
        MultiMarket market = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        MarketMaker maker = market.makeMarketMaker(q100, chris);
        assertEquals(0, qOneThird.compareTo(market.currentProbability(snow)));
        assertEquals(0, qOneThird.compareTo(market.currentProbability(rain)));
        assertEquals(0, qOneThird.compareTo(market.currentProbability(sun)));

        User bill = makeUser("bill");
        Probability fortyPercent = new Probability("0.4");
        maker.buyUpToQuantity(rain, fortyPercent, q1000, bill);
        assertEquals(0, maker.currentProbability(sun).compareTo(maker.currentProbability(snow)));
        assertEquals(0, fortyPercent.compareTo(maker.currentProbability(rain)));

        Probability prevProb = new Probability(".3");  // (1.0 - .4) / 2  == .3
        User sara = makeUser("sara");
        Probability half = new Probability(".5");
        maker.buyWithCostLimit(snow, half, q1000, sara);
        assertEquals(0, half.compareTo(maker.currentProbability(snow)));
        assertEquals(0, maker.currentProbability(rain).div(fortyPercent).compareTo(maker.currentProbability(sun).div(prevProb)));
    }

    public void testMovePriceTo99() {
        User marketOwner = makeUser("chris");
        MultiMarket market = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        MarketMaker maker = market.makeMarketMaker(q100, marketOwner);
        assertEquals(0, qOneThird.compareTo(market.currentProbability(snow)));
        assertEquals(0, qOneThird.compareTo(market.currentProbability(rain)));
        assertEquals(0, qOneThird.compareTo(market.currentProbability(sun)));

        User bill = makeUser("bill");
        Probability ninetyNine = new Probability("0.99");
        maker.buyUpToQuantity(rain, ninetyNine, q1000, bill);
        assertEquals(0, ninetyNine.compareTo(market.currentProbability(rain)));
    }

    private Quantity odds(Probability estimate) {
        return estimate.odds();
    }

    public void testBuyingFromMarketMaker() {
        User buyer = makeUser("buyer");
        buyUpToProbQ(mm, buyer, "0.45", 10, rain);
        assertTrue(new Probability("0.45").compareTo(mm.currentProbability(rain)) > 0);
        assertTrue(new Probability("0.3334").compareTo(mm.currentProbability(rain)) < 0);

        buyUpToProbQ(mm, buyer, "0.45", 100000, rain);
        assertQEquals(0.45, mm.currentProbability(rain));

        User ace = makeUser("ace", 50000);
        buyUpToProbQ(mm, ace, "0.50", 30000, rain);
        assertQEquals(0.5, mm.currentProbability(rain));
    }

    public void testSpecifySpendingLimit() {
        final double endowment = 1000;
        final Quantity limit = q100;
        User janet = makeUser("janet", endowment);
        User ace = makeUser("ace", endowment);
        User nick = makeUser("nick", endowment);

        Probability prob = Probability.HALF;
        mm.buyWithCostLimit(sun, prob, limit, janet);
        assertQEquals(q(endowment).minus(limit), janet.cashOnHand());
        assertTrue(qOneThird.compareTo(market.currentProbability(sun)) < 0);

        mm.buyWithCostLimit(rain, prob, limit, ace);
        assertQEquals(q(endowment).minus(limit), ace.cashOnHand());
        assertTrue(qOneThird.compareTo(market.currentProbability(rain)) < 0);
        assertEquals(0, janet.cashOnHand().compareTo(ace.cashOnHand()));

        mm.buyWithCostLimit(snow, prob, limit, nick);
        assertTrue(qOneThird.compareTo(market.currentProbability(snow)) < 0);
        assertEquals(0, nick.cashOnHand().compareTo(ace.cashOnHand()));
    }

    public void testLogActions() {
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);
        User ace = makeUser("ace", q1000);

        buyUpToProbQ(mm, ace, "0.65", 10, snow);
        assertEquals(3, appender.messageCount());
    }

    public void testMultiMarket() throws DuplicateOrderException {
        double endowment = 1000;
        User ace = makeUser("ace", endowment);

        limitOrder(m, sun, "40", 20, ace);
        assertQEquals(20, ace.getAccounts().couponCount(sun));
    }

    public void testBuyingWithCostLimit() {
        User buyer = makeUser("buyer");
        int iota = 10;
        mm.buyWithCostLimit(rain, new Probability(".45"), q(iota), buyer);
        assertTrue(buyer.cashOnHand().compareTo(q(100000 - iota)) >= 0);

        mm.buyWithCostLimit(rain, new Probability(".95"), q(100000), buyer);
        Quantity cash = buyer.cashOnHand();
        assertQIsClose(0, cash, 0.001);
    }

    public void testThinnerMarketsMoveFaster() {
        SecureUser banker = new SecureUser("banker", rootBank.makeFunds(100000), "banker's pwd", "bank@example.com");
        String thinMktName = "svelte";
        MultiClaim thinClaim = MultiClaim.makeClaim("thin", banker, "the skinny", new String[]{thinMktName, "waif", "rail"});
        String thickMktName = "fat";
        MultiClaim thickClaim = MultiClaim.makeClaim("thick", banker, "the whole story", new String[]{thickMktName, "hefty", "chunky"});
        MultiMarket thinMkt = MultiMarket.make(banker, thinClaim, rootBank.noFunds());
        MultiMarket thickMkt = MultiMarket.make(banker, thickClaim, rootBank.noFunds());
        thinMkt.makeMarketMaker(q1000, banker);
        thickMkt.makeMarketMaker(q(10000), banker);

        User buyer1 = makeUser("buyer1");
        User buyer2 = makeUser("buyer2");
        Position thinPos = thinClaim.lookupPosition(thinMktName);
        Position thickPos = thickClaim.lookupPosition(thickMktName);
        thinMkt.buyWithCostLimit(thinPos, Price.dollarPrice("45"), q(200), buyer1, true);
        thickMkt.buyWithCostLimit(thickPos, Price.dollarPrice("45"), q(200), buyer2, true);
        assertTrue(buyer1.cashOnHand().compareTo(buyer2.cashOnHand()) > 0);
        Quantity thinCoupons = buyer1.couponCount(thinPos);
        Quantity thickCoupons = buyer2.couponCount(thickPos);
        assertTrue("buyer should have purchased some coupons", thickCoupons.isPositive());
        assertTrue("buyer should have purchased some coupons", thinCoupons.isPositive());
        Quantity thinPrice = thinCoupons.div(q(100000).minus(buyer1.cashOnHand()));
        Quantity thickPrice = thickCoupons.div((q(100000).minus(buyer2.cashOnHand())));
        assertTrue("thin markets should be cheaper to move", thinPrice.compareTo(thickPrice) < 0);
    }

    public void testSymmetricMovementWithCouponSales() {
        SecureUser banker = new SecureUser("banker", rootBank.makeFunds(100000), "banker's pwd", "bank@example.com");
        String thinMktName = "svelte";
        MultiClaim thinClaim = MultiClaim.makeClaim("thin", banker, "the skinny", new String[]{thinMktName, "waif", "rail"});
        MultiMarket thinMkt = MultiMarket.make(banker, thinClaim, rootBank.noFunds());
        thinMkt.makeMarketMaker(q(10000), banker);

        User buyer1 = makeUser("buyer1");
        Position thinPos = thinClaim.lookupPosition(thinMktName);
        thinMkt.buyWithCostLimit(thinPos, Price.dollarPrice("45"), q(200), buyer1, true);
        Quantity thinCoupons = buyer1.couponCount(thinPos);
        assertTrue("buyer should have purchased some coupons", thinCoupons.isPositive());
        assertTrue(q(100000).compareTo(buyer1.cashOnHand()) > 0);
        assertTrue(buyer1.couponCount(thinPos).isPositive());

        // should be symmetrical even with coupons to sell
        thinMkt.buyWithCostLimit(thinPos, new Price(Quantity.Q100.div(q(3)), Price.ONE_DOLLAR), q(400), buyer1, true);  /// had to spend more money!
        assertEquals(0, q(100000).compareTo(buyer1.cashOnHand()));
        assertTrue(buyer1.couponCount(thinPos).isZero());
    }

    public void testMoneyPump() {
        Price price = Price.dollarPrice("30");
        SecureUser creator = new SecureUser("jane", rootBank.makeFunds(q1000), "jane's pwd", "jane@example.com");

        MultiClaim footballClaim = MultiClaim.makeClaim("football", creator, "this play", new String[]{"pass", "run", "kick"});
        MultiMarket m = MultiMarket.make(creator, footballClaim, rootBank.noFunds());
        Position pass = footballClaim.lookupPosition("pass");
        MarketMaker maker = m.makeMarketMaker(q100, creator);
        final double endowment = 10000.0;
        User jason = makeUser("jason", endowment);

        // buy rain upward from 1/3
        buyUpToQuantity(pass, maker, "0.9", 1000, jason);
        assertTrue(qOneThird.compareTo(m.currentProbability(pass)) < 0);

        // buy rain back down to .30
        maker.buyOrSellUpToQuantity(pass, price, q1000, jason);
        Quantity firstCash = jason.cashOnHand();
        Quantity firstRain = jason.couponCount(pass);

        // buy rain upward from .30
        buyUpToQuantity(pass, maker, "0.9", 1000, jason);
        assertTrue(qOneThird.compareTo(m.currentProbability(pass)) < 0);

        maker.buyOrSellUpToQuantity(pass, price, q1000, jason);
        assertQEquals(firstRain, jason.couponCount(pass));
        buyUpToQuantity(pass, maker, "0.9", 1000, jason);
        maker.buyOrSellUpToQuantity(pass, price, q1000, jason);
        assertQEquals(firstRain, jason.couponCount(pass));
        buyUpToQuantity(pass, maker, "0.9", 1000, jason);
        maker.buyOrSellUpToQuantity(pass, price, q1000, jason);

        assertTrue("jason should have spent some money (since the prob isn't 1/3", q(endowment).compareTo(jason.cashOnHand()) > 0);
        assertQEquals(firstCash, jason.cashOnHand());
        assertQEquals(firstRain, jason.couponCount(pass));
    }

    private void buyUpToQuantity(Position pass, MarketMaker maker, String p, double q, User u) {
        maker.buyUpToQuantity(pass, new Probability(p), q(q), u);
    }

    public void testSellHoldings() {
        User bill = makeUser("bill");
        User janet = makeUser("janet");
        User sue = makeUser("sue");
        User jon = makeUser("jon");
        buyUpToProbQ(mm, bill, "0.42", 500, rain);
        buyUpToProbQ(mm, janet, "0.35", 500, snow);
        buyOrSellUpToPriceQ(mm, sue, snow, "29", 500);
        buyUpToProbQ(mm, jon, "0.3", 500, sun);
        Quantity jonHoldings = jon.couponCount(sun);
        Quantity sueHoldings = sue.couponCount(sun);

        assertTrue(bill.couponCount(rain).isPositive());
        assertTrue(janet.couponCount(snow).isPositive());
        assertQEquals(sueHoldings, sue.couponCount(rain));
        assertQEquals(sueHoldings, sue.couponCount(sun));
        assertTrue(sueHoldings.isPositive());
        assertTrue(jonHoldings.isPositive());

        mm.sellHoldings(bill, rain);
        mm.sellHoldings(janet, snow);
        mm.sellHoldings(jon, rain);  // no holdings
        mm.sellHoldings(sue, rain);  // has rain and sun

        assertQIsClose(0, janet.couponCount(snow), 0.001);
        assertQIsClose(0, bill.couponCount(rain), 0.001);
        assertQIsClose(0, sue.couponCount(rain), 0.001);
        assertQIsClose(0, sue.couponCount(snow), 0.001);
        assertQEquals(sueHoldings, sue.couponCount(sun));
        assertQIsClose(0, jon.couponCount(rain), 0.001);
        assertQIsClose(0, jon.couponCount(snow), 0.001);
        assertQEquals(jonHoldings, jon.couponCount(sun));
    }

    public void testQuantLimitSelling() {
        User buyer = makeUser("buyer");
        buyUpToProbQ(mm, buyer, "0.45", 1000, rain);
        assertTrue(q(0.45).compareTo(market.currentProbability(rain)) >= 0);
        assertTrue(q(1.0/3.0).compareTo(market.currentProbability(rain)) < 0);

        buyUpToProbQ(mm, buyer, "0.5", 10000, rain);
        assertQEquals(0.5, market.currentProbability(rain));

        User ace = makeUser("ace", 50000);
        buyUpToProbQ(mm, ace, "0.6", 40000, rain);
        assertQEquals(0.6, market.currentProbability(rain));

        User seller = makeUser("seller");
        buyOrSellUpToPriceQ(mm, seller, rain, "45", 1000);
        assertTrue(q(0.45).compareTo(market.currentProbability(rain)) <= 0);
        assertTrue(q(0.6).compareTo(market.currentProbability(rain)) > 0);

        buyOrSellUpToPriceQ(mm, seller, rain, "45", 10000);
        assertQEquals(0.45, market.currentProbability(rain));

        User nick = makeUser("nick", 50000);
        buyOrSellUpToPriceQ(mm, nick, rain, "40", 3000);
        assertQEquals(0.40, market.currentProbability(rain));
    }
}
