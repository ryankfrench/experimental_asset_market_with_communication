package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.currency.*;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Properties;
import java.util.Iterator;
import java.util.HashMap;
import java.util.regex.Pattern;

public class MarketTest extends OrdersTestCase {
    final private double SOME_COUPONS = 100;
    final private double SOME_CASH = 10000;
    final Price ninetyNineCents = Price.dollarPrice("99");
    private User janet;
    private BinaryMarket m;
    private final double endowment = 100000.0;
    private CouponBank couponBank;

    protected void setUp() throws Exception {
        super.setUp();
        janet = makeUser("Janet", endowment);
        Properties props = new Properties();
        props.put(UNARY_ASSETS, "false");
        couponBank = CouponBank.makeBank(claim, rootBank.noFunds());
        m = BinaryMarket.make(claim, couponBank, owner, props, null, 0);
        HibernateTestUtil.resetSessionFactory();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        m = null;
        janet = null;
        couponBank = null;
    }

    public void testMarketCreation() {
        assertEquals("silly", m.getClaim().getName());
    }

    public void testEnteringBookOrdersViaMarket() throws DuplicateOrderException {
        limitOrder(m, yes, "40", SOME_COUPONS, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(SOME_COUPONS, m.getBook().bestQuantity(yes));
    }

    private void limitOrder(BinaryMarket market, Position pos, String price, double quantity, User user) throws DuplicateOrderException {
        market.limitOrder(pos, Price.dollarPrice(price), q(quantity), user);
    }

    public void testTransaction() throws DuplicateOrderException {
        final int purchaseAmount = 30;
        limitOrder(m, yes, "40", purchaseAmount, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(endowment, janet.cashOnHand());

        User ace = makeUser("Ace", endowment);
        limitOrder(m, no, "65", SOME_COUPONS, ace);
        assertTrue(ace.cashOnHand().compareTo(q(endowment)) < 0);
        assertTrue(janet.cashOnHand().compareTo(q(endowment)) < 0);
        assertQEquals(purchaseAmount, janet.couponCount(yes));
        assertQEquals(0, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(65, m.getBook().bestBuyOfferFor(no));
        assertQEquals(SOME_COUPONS - purchaseAmount, m.getBook().bestQuantity(no));
    }

    public void testPriceOutOfBounds() throws DuplicateOrderException {
        final int purchaseAmount = 30;
        limitOrder(m, yes, "85", purchaseAmount, janet);
        assertQEquals(endowment, janet.cashOnHand());

        User ace = makeUser("Ace", endowment);
        limitOrder(m, no, "0.0", endowment, ace);
        assertNotSame("", ace.getWarningsHTML());
        assertQEquals(endowment, ace.cashOnHand());
        assertQEquals(endowment, janet.cashOnHand());
        assertQEquals(0, m.getBook().bestBuyOfferFor(no));
    }

    public void testBuyerIsShortMoney() throws DuplicateOrderException {
        SecureUser marketOwner = makeSecureUser("Chris", endowment);
        m.makeMarketMaker(marketOwner, q(500));

        limitOrder(m, yes, "40", 30, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        limitOrder(m, no, "40", 30, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(no));
        assertQEquals(endowment, janet.cashOnHand());
        assertQEquals(.50, m.currentProbability(yes));

        User ace = makeUser("Ace", 20);
        limitOrder(m, no, "75", 200, ace);
        assertQEquals(endowment, janet.cashOnHand());
        assertFalse(ace.couponCount(no).isZero());
        assertQIsClose(0, ace.cashOnHand(), .001);
        assertTrue(m.currentProbability(yes).compareTo(Probability.HALF) < 0);

        User nick = makeUser("Nick", 20);
        limitOrder(m, yes, "75", 200, nick);
        assertQEquals(endowment, janet.cashOnHand());
        assertFalse(nick.couponCount(yes).isZero());
        assertQIsClose(0, nick.cashOnHand(), .001);
        assertQIsClose(.50, m.currentProbability(yes), .01);
    }

    public void testBuyingFromBookAtMarketPrice() throws DuplicateOrderException {
        final int janetPurchaseAmount = 30;
        final int aceOfferAmount = 200;

        SecureUser marketOwner = makeSecureUser("Chris", endowment);
        m.makeMarketMaker(marketOwner, q(500));

        limitOrder(m, yes, "50", janetPurchaseAmount, janet);
        assertQEquals(50, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(endowment, janet.cashOnHand());

        User ace = makeUser("Ace", 2000);
        assertQEquals(0.0, ace.couponCount(yes));

        limitOrder(m, no, "75", aceOfferAmount, ace);

        assertQEquals(janetPurchaseAmount, janet.couponCount(yes));
        assertQEquals(q(endowment).minus(q(15).times(m.maxPrice())), janet.cashOnHand());
        assertQIsClose(0, ace.cashOnHand(), 0.0001);
        assertTrue(ace.couponCount(no).isPositive());
        assertQEquals(0, m.getBook().bestQuantity(no));
    }

    public void testSellingFromMakerWhenBookIsWorse() throws DuplicateOrderException {
        SecureUser marketOwner = makeSecureUser("Chris");
        m.makeMarketMaker(marketOwner, q(50000));

        limitOrder(m, yes, "40", SOME_COUPONS, janet);
        limitOrder(m, no, "35", SOME_COUPONS, janet);

        assertQEquals(35, m.getBook().bestBuyOfferFor(no));
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(.50, m.currentProbability(yes));

        User ace = makeUser("Ace");
        limitOrder(m, no, "45", 300, ace);

        assertQEquals(45, m.getBook().bestBuyOfferFor(no));
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        assertQIsClose(.50, m.currentProbability(yes), .0001);

        limitOrder(m, no, "57", 300, ace);

        assertQEquals(57, m.getBook().bestBuyOfferFor(no));
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        assertQIsClose(.57, m.currentProbability(no), .0001);
    }

    public void testBuyingFromMakerWhenBookIsWorse() throws DuplicateOrderException {
        limitOrder(m, no, "40", SOME_COUPONS, janet);
        limitOrder(m, yes, "35", SOME_COUPONS, janet);

        SecureUser marketOwner = makeSecureUser("Chris");
        m.makeMarketMaker(marketOwner, q(500));

        assertQEquals(35, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(40, m.getBook().bestBuyOfferFor(no));
        assertQEquals(.50, m.currentProbability(yes));

        User ace = makeUser("Ace");
        limitOrder(m, yes, "45", 300, ace);

        assertQEquals(45, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(40, m.getBook().bestBuyOfferFor(no));
        assertQEquals(.50, m.currentProbability(yes));

        limitOrder(m, yes, "57", 300, ace);

        assertQEquals(57, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(40, m.getBook().bestBuyOfferFor(no));
        assertQEquals(.57, m.currentProbability(yes));
    }

    public void testBuyingFromBookThenMaker() throws DuplicateOrderException {
        int manyCoupons = 900;
        SecureUser marketOwner = makeSecureUser("Chris", endowment);
        m.makeMarketMaker(marketOwner, q(500));

        limitOrder(m, no, "30", SOME_COUPONS, janet);
        limitOrder(m, yes, "35", SOME_COUPONS, janet);

        assertQEquals(35, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));
        assertQEquals(.50, m.currentProbability(yes));

        User ace = makeUser("Ace", endowment);
        limitOrder(m, yes, "75", manyCoupons, ace);

        assertQEquals(75, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(.25, m.currentProbability(no));
        assertQEquals(0, m.getBook().bestBuyOfferFor(no));
        assertQEquals(SOME_COUPONS, janet.couponCount(no));
        assertTrue(ace.cashSameOrGreaterThan(q(400)));
        assertQEquals(manyCoupons, ace.couponCount(yes).plus(m.getBook().bestQuantity(yes)));
    }

    public void testSellingToBookThenMaker() throws DuplicateOrderException {
        SecureUser marketOwner = makeSecureUser("Chris", endowment);
        m.makeMarketMaker(marketOwner, q(50000), new Probability(".6"));

        limitOrder(m, no, "10", SOME_COUPONS, janet);
        limitOrder(m, no, "30", SOME_COUPONS, janet);
        limitOrder(m, yes, "15", SOME_COUPONS, janet);
        limitOrder(m, yes, "35", SOME_COUPONS, janet);

        assertQEquals(35, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(30, m.getBook().bestBuyOfferFor(no));
        assertQEquals(.60, m.currentProbability(yes));

        User ace = makeUser("Ace", endowment);
        limitOrder(m, no, "80", 900, ace);

        assertQEquals(30, m.getBook().bestBuyOfferFor(no));
        assertTrue(q(.20).compareTo(m.currentProbability(yes)) < 0);
        assertTrue(q(.30).compareTo(m.currentProbability(yes)) > 0);
        assertQEquals(15, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(SOME_COUPONS, janet.couponCount(yes));
        assertTrue(ace.cashSameOrGreaterThan(q(40000)));
        assertQEquals(1000, ace.couponCount(no).plus(m.getBook().bestQuantity(no)));
    }

    public void testLogEvents() throws DuplicateOrderException {
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);
        limitOrder(m, yes, "40", 30, janet);
        assertEquals(2, appender.messageCount());
    }

    public void testCloseMarket() throws DuplicateOrderException {
        limitOrder(m, yes, "40", SOME_COUPONS, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        m.close();
        limitOrder(m, yes, "50", SOME_COUPONS, janet);
        assertQEquals(40, m.getBook().bestBuyOfferFor(yes));
        m.open();
        limitOrder(m, yes, "50", SOME_COUPONS, janet);
        assertQEquals(50, m.getBook().bestBuyOfferFor(yes));
    }

    public void testMultipleBookTransactions() throws DuplicateOrderException {
        User buyerA = makeUser("buyerA");
        User buyerB = makeUser("buyerB");
        User buyerC = makeUser("buyerC");
        buyerA.endow(couponBank.issueUnpairedCoupons(q(20), yes));
        buyerB.endow(couponBank.issueUnpairedCoupons(q(20), yes));
        buyerC.endow(couponBank.issueUnpairedCoupons(q(20), no));

        assertQEquals(20, buyerA.couponCount(claim));
        assertQEquals(20, buyerB.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        limitOrder(m, yes, "20", 1, buyerA);
        limitOrder(m, yes, "30", 1, buyerA);
        limitOrder(m, yes, "40", 1, buyerA);
        limitOrder(m, no, "10", 1, buyerB);
        limitOrder(m, no, "20", 1, buyerB);
        assertQEquals(20, buyerA.couponCount(claim));
        assertQEquals(20, buyerB.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        limitOrder(m, no, "60", 1, buyerB);
        assertQEquals(21, buyerA.couponCount(claim));
        assertQEquals(19, buyerB.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        limitOrder(m, no, "70", 1, buyerB);
        assertQEquals(22, buyerA.couponCount(claim));
        assertQEquals(18, buyerB.couponCount(claim));
        assertQEquals(-20, buyerC.couponCount(claim));

        limitOrder(m, yes, "40", 1, buyerC);
        limitOrder(m, yes, "80", 1, buyerC);

        assertQEquals(22, buyerA.couponCount(claim));
        assertQEquals(17, buyerB.couponCount(claim));
        assertQEquals(-19, buyerC.couponCount(claim));
    }

    public void testTradeAssetsForCash() throws DuplicateOrderException {
        User buyer = rootBank.makeEndowedUser("buyer", q(10000));
        User seller = rootBank.makeEndowedUser("seller", Quantity.ZERO);
        User bystander = rootBank.makeEndowedUser("bystander", Quantity.ZERO);
        assignCouponsTo(q(20), seller, bystander);

        limitOrder(m, yes, "60", 5, buyer);
        assertQEquals(q(10000), buyer.cashOnHand());
        assertQEquals(0, seller.cashOnHand());
        assertQEquals(20, seller.couponCount(claim));
        assertQEquals(0, buyer.couponCount(claim));

        limitOrder(m, no, "40", 5, seller);
        assertQEquals(15, seller.couponCount(claim));
        assertQEquals(5, buyer.couponCount(claim));
        assertQEquals(q(10000 - 5 * 60), buyer.cashOnHand());
        assertQEquals(5 *  60, seller.cashOnHand());

        limitOrder(m, no, "40", 5, seller);
        assertQEquals(15, seller.couponCount(claim));
        assertQEquals(5, buyer.couponCount(claim));
        assertQEquals(q(10000 - 5 * 60), buyer.cashOnHand());
        assertQEquals(5 * 60, seller.cashOnHand());

    }

    public void testTradeCashForAssets() throws DuplicateOrderException {
        User buyer = rootBank.makeEndowedUser("buyer", q(SOME_CASH));
        User seller = rootBank.makeEndowedUser("seller", Quantity.ZERO);
        User bystander = rootBank.makeEndowedUser("bystander", Quantity.ZERO);
        assignCouponsTo(q(20), seller, bystander);

        limitOrder(m, no, "40", 5, seller);
        assertQEquals(SOME_CASH, buyer.cashOnHand());
        assertQEquals(0, seller.cashOnHand());
        assertQEquals(20, seller.couponCount(claim));
        assertQEquals(0, buyer.couponCount(claim));

        limitOrder(m, yes, "60", 5, buyer);
        assertQEquals(15, seller.couponCount(claim));
        assertQEquals(5, buyer.couponCount(claim));
        assertQEquals(SOME_CASH - 5 * 60, buyer.cashOnHand());
        assertQEquals(5 * 60, seller.cashOnHand());

        limitOrder(m, yes, "60", 5, buyer);
        assertQEquals(15, seller.couponCount(claim));
        assertQEquals(5, buyer.couponCount(claim));
        assertQEquals(SOME_CASH - 5 * 60, buyer.cashOnHand());
        assertQEquals(5 * 60, seller.cashOnHand());
    }

    public void testFractionalTrading() throws DuplicateOrderException {
        User A1 = rootBank.makeEndowedUser("A1", q(750));
        User B4 = rootBank.makeEndowedUser("B4", q(500));

        Coupons[] couponses = m.printNewCouponSets(q(15), rootBank.makeFunds(15));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(5)));
                B4.endow(coupons.provide(q(10)));
            }
        }

        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));

        addOrder(yes, "56", A1);
        takeOrder(no, "44", B4);

        addOrder(no, "30", B4);
        takeOrder(yes, "70", A1);
        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));

        addOrder(yes, "87", B4);
        takeOrder(no, "13", A1);

        addOrder(no, "30", B4);
        takeOrder(yes, "70", A1);

        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);

        addOrder(no, "30", B4);
        takeOrder(yes, "70", A1);

        addOrder(no, "20", B4);
        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));
        takeOrder(yes, "80", A1);

        addOrder(no, "12", B4);
        takeOrder(yes, "88", A1);

        addOrder(no, "02", B4);
        takeOrder(yes, "98", A1);

        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);

        addOrder(no, "13", B4);
        takeOrder(yes, "87", A1);

        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);

        assertQEquals(15.0, A1.couponCount(claim).plus(B4.couponCount(claim)));
        assertTrue(A1.cashOnHand().compareTo(Quantity.Q100) < 0);
        assertTrue(A1.cashOnHand().compareTo(Quantity.ZERO) > 0);
    }

    public void testWholeShareTradingOnly() throws DuplicateOrderException {
        User A1 = rootBank.makeEndowedUser("A1", q(750));
        User B4 = rootBank.makeEndowedUser("B4", q(50));

        Coupons[] couponses = m.printNewCouponSets(q(15), rootBank.makeFunds(1500));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(5)));
                B4.endow(coupons.provide(q(10)));
            }
        }
        m.setWholeShareTradingOnly();

        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));

        addOrder(yes, "56", A1);
        takeOrder(no, "44", B4);

        addOrder(no, "3", B4);
        takeOrder(yes, "70", A1);
        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));

        addOrder(yes, "87", B4);
        takeOrder(no, "13", A1);

        addOrder(no, "30", B4);
        takeOrder(yes, "70", A1);
        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);

        addOrder(no, "30", B4);
        takeOrder(yes, "70", A1);
        addOrder(no, "20", B4);
        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));
        takeOrder(yes, "80", A1);

        addOrder(no, "12", B4);
        takeOrder(yes, "88", A1);
        addOrder(no, "02", B4);
        takeOrder(yes, "98", A1);

        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);
        addOrder(no, "13", B4);
        takeOrder(yes, "87", A1);

        addOrder(no, "33", B4);
        takeOrder(yes, "67", A1);
        addOrder(no, "33", B4);
        assertQEquals(A1.couponCount(claim), A1.couponCount(claim).newScale(0));
        assertQEquals(15, A1.couponCount(claim).plus(B4.couponCount(claim)));
        takeOrder(yes, "67", A1);

        assertQEquals(15.0, A1.couponCount(claim).plus(B4.couponCount(claim)));
        assertTrue(A1.cashOnHand().compareTo(Quantity.Q100) < 0);
        assertQEquals(A1.couponCount(claim), A1.couponCount(claim).newScale(0));
    }

    public void testOfferCantAffordTrade() throws DuplicateOrderException {
        MockAppender appender = new MockAppender();
        Logger.getLogger(PriceAction.class).addAppender(appender);

        User A1 = rootBank.makeEndowedUser("A1", Quantity.Q100);
        User B4 = rootBank.makeEndowedUser("B4", Quantity.Q100);

        Coupons[] couponses = m.printNewCouponSets(q(2), rootBank.makeFunds(200));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(1)));
                B4.endow(coupons.provide(q(1)));
            }
        }

        assertBalanced(A1, B4, q(200), 2);
        addOrder(yes, "87", A1);    // A1 can afford the order

        Funds sequestered = A1.getAccounts().provideCash(q(50));   // but now he can't

        takeOrder(no, "13", B4);
        assertBalanced(A1, B4, q(150), 2);
        assertQEquals(0.0, A1.cashOnHand());
        assertQEquals(150, B4.cashOnHand());
        assertQIsClose(85.0 / 200, B4.couponCount(claim), 1);
        assertQIsClose(2.0 - (.85 / 2), A1.couponCount(claim), .01);

        Iterator evIter = appender.getEvents().iterator();
        boolean foundSell = false;
        Pattern exp = Pattern.compile(".*575( of .*)? at 87.*", Pattern.DOTALL);
        for (; evIter.hasNext();) {
            LoggingEvent e = (LoggingEvent) evIter.next();
            if (exp.matcher(e.getRenderedMessage()).matches()) {
                foundSell = true;
            }
        }
        assertTrue("should find a matching sell", foundSell);
        assertQEquals(50, sequestered.getBalance());
    }

    public void testBuyerCantAffordTrade() throws DuplicateOrderException {
        User A1 = rootBank.makeEndowedUser("A1", Quantity.Q100);
        User B4 = rootBank.makeEndowedUser("B4", q(300));

        Coupons[] couponses = market.printNewCouponSets(q(2), rootBank.makeFunds(200));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(1)));
                B4.endow(coupons.provide(q(1)));
            }
        }
        assertBalanced(A1, B4, q(400), 2);

        market.limitOrder(yes, Price.dollarPrice("70"), Quantity.ONE, A1);
        market.marketOrder(no, Price.dollarPrice("30"), Quantity.ONE, B4);
        assertBalanced(A1, B4, q(400), 2);
        assertQEquals(100 - 70, A1.cashOnHand());
        assertQEquals(300 + 70, B4.cashOnHand());
        market.limitOrder(no, Price.dollarPrice("13"), Quantity.ONE, B4);
        market.marketOrder(yes, Price.dollarPrice("87"), Quantity.ONE, A1);
        assertQEquals(0, A1.cashOnHand());
        assertTrue(B4.cashOnHand().isPositive());
        assertQEquals(6, A1.couponCount(claim).plus(B4.cashOnHand().div(Quantity.Q100)));
        assertQEquals(2.0, A1.couponCount(claim).plus(B4.couponCount(claim)));
    }

    public void testOrderCantAffordTrade_WholeShares() throws DuplicateOrderException {
        market.setWholeShareTradingOnly();
        User A1 = rootBank.makeEndowedUser("A1", q(50));
        User B4 = rootBank.makeEndowedUser("B4", q(50));
        User G8 = rootBank.makeEndowedUser("G8", q(20));

        addOrder(yes, "70", A1);       // A1 can't afford
        assertEquals(0, A1.getOrders().size());
        takeOrder(no, "30", B4);   // Nothing for B4 to accept
        assertEquals(0, B4.getOrders().size());

        assertQEquals(50, A1.cashOnHand());
        assertQEquals(50, B4.cashOnHand());
        assertQEquals(0, A1.couponCount(claim));
        assertQEquals(0, B4.couponCount(claim));

        Coupons[] couponses = market.printNewCouponSets(q(1), rootBank.makeFunds(100));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(1)));
            }
        }

        addOrder(no, "30", A1);       // A1 CAN afford
        assertEquals(1, A1.getOrders().size());
        takeOrder(yes, "70", B4);   // but B4 can only partially afford
        assertEquals(0, B4.getOrders().size());

        assertQEquals(50, A1.cashOnHand());
        assertQEquals(50, B4.cashOnHand());
        assertQEquals(1, A1.couponCount(claim));
        assertQEquals(0, B4.couponCount(claim));

        addOrder(yes, "70", G8);
        assertEquals(0, G8.getOrders().size());
        takeOrder(no, "30", A1);
        assertEquals(1, A1.getOrders().size());

        assertQEquals(50, A1.cashOnHand());
        assertQEquals(20, G8.cashOnHand());
        assertQEquals(1, A1.couponCount(claim));
        assertQEquals(0, G8.couponCount(claim));
    }

    public void testOrder2CantAffordTrade_WholeShares() throws DuplicateOrderException {
        m.setWholeShareTradingOnly();
        User A1 = rootBank.makeEndowedUser("A1", q(50));
        User G8 = rootBank.makeEndowedUser("G8", q(20));

        Coupons[] couponses = m.printNewCouponSets(Quantity.ONE, rootBank.makeFunds(1));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(Quantity.ONE));
            }
        }

        addOrder(yes, "70", G8);
        takeOrder(no, "30", A1);
        assertQEquals(50, A1.cashOnHand());
        assertQEquals(20, G8.cashOnHand());
        assertQEquals(1, A1.couponCount(claim));
        assertQEquals(0, G8.couponCount(claim));
    }

    public void testOrderCantAffordTradeHighMax() throws DuplicateOrderException {
        Properties props = new Properties();
        props.put(UNARY_ASSETS, "false");
        props.put(MAX_TRADING_PRICE, "900");
        props.put(WHOLE_SHARE_TRADING_ONLY, "true");
        BinaryMarket mkt = BinaryMarket.make(claim, couponBank, owner, props, null, 0);

        User A1 = rootBank.makeEndowedUser("A1", q(50));
        User G8 = rootBank.makeEndowedUser("G8", q(500));

        Coupons[] couponses = mkt.printNewCouponSets(Quantity.ONE, rootBank.makeFunds(900));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(Quantity.ONE));
            }
        }

        assertTrue(mkt.getBook().bestBuyOfferFor(yes).isZero());
        assertQEquals(1, A1.couponCount(claim));
        assertQEquals(0, G8.couponCount(claim));
        mkt.limitOrder(yes, new Price("297", mkt.maxPrice()), Quantity.ONE, G8);
        assertQEquals(297, mkt.getBook().bestBuyOfferFor(yes));
        assertQEquals(1, A1.couponCount(claim));
        assertQEquals(0, G8.couponCount(claim));
        mkt.marketOrder(yes, new Price("315", mkt.maxPrice()), Quantity.ONE, G8);
        assertQEquals(297, mkt.getBook().bestBuyOfferFor(yes));
    }

    public void testFractionalTradeHighMax() throws DuplicateOrderException {
        Properties props = new Properties();
        props.put(UNARY_ASSETS, "false");
        props.put(MAX_TRADING_PRICE, "900");
        couponBank = CouponBank.makeBank(claim, rootBank.noFunds());
        m = BinaryMarket.make(claim, couponBank, owner, props, null, 0);
        m.setWholeShareTradingOnly();
        Price priceLimit = m.maxPrice();

        User AA = rootBank.makeEndowedUser("AA", q(21200));
        User BA = rootBank.makeEndowedUser("BA", q(95300));
        User CC = rootBank.makeEndowedUser("CC", q(52700));

        Coupons[] couponses = m.printNewCouponSets(q(9), rootBank.makeFunds(priceLimit.times(q(10))));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                AA.endow(coupons.provide(q(4)));
                CC.endow(coupons.provide(q(1)));
                BA.endow(coupons.provide(q(4)));
            }
        }

        assertTrue(m.getBook().bestBuyOfferFor(yes).isZero());
        assertQEquals(4, AA.couponCount(claim));
        assertQEquals(4, BA.couponCount(claim));
        assertQEquals(1, CC.couponCount(claim));
        assertQEquals(21200, AA.cashOnHand());
        assertQEquals(95300, BA.cashOnHand());
        assertQEquals(52700, CC.cashOnHand());

        m.limitOrder(no, new Price(900 - 200, priceLimit), Quantity.ONE, CC);
        m.limitOrder(no, new Price(900 - 199, priceLimit), Quantity.ONE, CC);
        assertQEquals(701, m.getBook().bestBuyOfferFor(no));

        m.marketOrder(yes, new Price(200, priceLimit), Quantity.ONE, AA);

        assertQEquals(5, AA.couponCount(claim));
        assertQEquals(4, BA.couponCount(claim));
        assertQEquals(0, CC.couponCount(claim));

        m.limitOrder(yes, new Price(12, priceLimit), Quantity.ONE, AA);
        m.limitOrder(no, new Price(900 - 155, priceLimit), Quantity.ONE, AA);
        m.marketOrder(yes, new Price(155, priceLimit), Quantity.ONE, BA);
        assertQEquals(4, AA.couponCount(claim));
        assertQEquals(5, BA.couponCount(claim));
        assertQEquals(0, CC.couponCount(claim));

        m.marketOrder(no, new Price(900 - 12, priceLimit), Quantity.ONE, BA);
        assertQEquals(5, AA.couponCount(claim));
        assertQEquals(4, BA.couponCount(claim));
        assertQEquals(0, CC.couponCount(claim));
    }

    public void testOrderCantAffordTrade() throws DuplicateOrderException {
        User A1 = rootBank.makeEndowedUser("A1", Quantity.Q100);
        User B4 = rootBank.makeEndowedUser("B4", Quantity.Q100);

        Coupons[] couponses = market.printNewCouponSets(q(2), rootBank.makeFunds(200));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(q(1)));
                B4.endow(coupons.provide(q(1)));
            }
        }
        assertBalanced(A1, B4, q(200), 2);

        addOrder(yes, "70", A1);
        takeOrder(no, "30", B4);
        assertBalanced(A1, B4, q(200), 2);
        addOrder(no, "13", B4);
        takeOrder(yes, "87", A1);
        assertTrue(A1.cashOnHand().isNonNegative());
        assertTrue(B4.cashOnHand().isNonNegative());
        assertTrue(A1.cashOnHand().isZero());
        assertQEquals(q(200).minus(B4.cashOnHand()).div(Quantity.Q100), A1.couponCount(claim).minus(q(2)));
        assertQEquals(q(2), A1.couponCount(claim).plus(B4.couponCount(claim)));
    }

    public void testBuyerHasNoCash() throws DuplicateOrderException {
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);

        User A1 = rootBank.makeEndowedUser("A1", Quantity.Q100);
        User B4 = rootBank.makeEndowedUser("B4", Quantity.Q100);

        Coupons[] couponses = m.printNewCouponSets(q(2), rootBank.makeFunds(200));
        for (int i = 0; i < couponses.length; i++) {
            Coupons coupons = couponses[i];
            if (coupons.getPosition().equals(yes)) {
                A1.endow(coupons.provide(Quantity.ONE));
                B4.endow(coupons.provide(Quantity.ONE));
            }
        }
        assertBalanced(A1, B4, q(200), 2);

        m.limitOrder(yes, Price.dollarPrice("70"), Quantity.ONE, A1);
        m.marketOrder(no, Price.dollarPrice("30"), Quantity.ONE, B4);
        assertBalanced(A1, B4, q(200), 2);
        m.limitOrder(no, Price.dollarPrice("13"), Quantity.ONE, B4);
        m.marketOrder(yes, Price.dollarPrice("87"), Quantity.ONE, A1);
        assertTrue(A1.cashOnHand().isNonNegative());
        assertTrue(B4.cashOnHand().isNonNegative());
        assertBalanced(A1, B4, q(200), 2);
        m.limitOrder(no, Price.dollarPrice("25"), Quantity.ONE, B4);

        int oldCount = appender.messageCount();
        m.marketOrder(yes, Price.dollarPrice("75"), Quantity.ONE, A1);
        assertEquals(oldCount, appender.messageCount());
    }

    private void assertBalanced(User first, User second, Quantity cash, int coupons) {
        assertQEquals(coupons, first.couponCount(claim).plus(second.couponCount(claim)));
        assertQEquals(cash, first.cashOnHand().plus(second.cashOnHand()));
    }

    private void addOrder(Position pos, String price, User user) throws DuplicateOrderException {
        market.limitOrder(pos, Price.dollarPrice(price), Quantity.ONE, user);
    }

    private void takeOrder(Position pos, String price, User u) throws DuplicateOrderException {
        market.marketOrder(pos, Price.dollarPrice(price), Quantity.ONE, u);
    }

    public void testBuyWithCostLimit() throws DuplicateOrderException {
        SecureUser marketOwner = makeSecureUser("Chris", endowment);
        m.makeMarketMaker(marketOwner, q(500));
        final Quantity purchaseAmount = q(30);
        Quantity janetQuant = m.buyWithCostLimit(yes, ninetyNineCents, purchaseAmount, janet);
        assertTrue("Shouldn't have negative balance.", janetQuant.isNonNegative());
        assertTrue("Should have been able to purchase some shares", janet.getAccounts().couponCount(yes).isPositive());
        assertQEquals(q(endowment).minus(purchaseAmount), janet.cashOnHand());
        assertEquals("", janet.getWarningsHTML());
        assertTrue(m.currentProbability(yes).compareTo(Probability.HALF) > 0);

        User ace = makeUser("Ace", endowment);
        Quantity aceQuant = m.buyWithCostLimit(no, ninetyNineCents, purchaseAmount, ace);
        assertTrue(aceQuant.isPositive());
        assertQEquals(q(endowment).minus(purchaseAmount), ace.cashOnHand());
        assertQEquals(q(endowment).minus(purchaseAmount), janet.cashOnHand());

        assertQIsClose(.50, m.currentProbability(yes), .001);

        SecureUser poorRichard = makeSecureUser("poorRichard", 200);
        Quantity richQuant = m.buyWithCostLimit(yes, ninetyNineCents, q(300), poorRichard); // more than he can afford
        assertTrue(poorRichard.cashOnHand().isZero());   // but he doesn't spend more than he has
        assertTrue(richQuant.isPositive());

        Quantity aceQuant2 = m.buyWithCostLimit(no, ninetyNineCents, q(200), ace);
        assertTrue(m.currentProbability(yes).compareTo(Probability.HALF) < 0);
        assertTrue(aceQuant2.isPositive());
        assertQEquals(q(endowment - 200).minus(purchaseAmount), ace.cashOnHand());  // and it only costs 200 to move the price back
    }

    public void testBuyFromBookWithCostLimit() throws DuplicateOrderException {
        final Quantity purchaseAmount = q(5);

        SecureUser chris = makeSecureUser("Chris", endowment);
        m.limitOrder(yes, Price.dollarPrice("30"), q(20), chris);
        m.limitOrder(no, Price.dollarPrice("60"), q(30), chris);

        Quantity janetQuant = m.buyWithCostLimit(yes, ninetyNineCents, purchaseAmount, janet);
        assertTrue("Should buy some coupons when a dollar limit is specified.", janetQuant.isPositive());
        assertTrue(janet.getAccounts().couponCount(yes).isPositive());
        Quantity remainingFunds = q(endowment).minus(purchaseAmount);
        assertQEquals(remainingFunds, janet.cashOnHand());
        assertEquals("", janet.getWarningsHTML());
        assertQEquals(30, m.getBook().bestBuyOfferFor(yes));
        assertQEquals(40, m.getBook().bestSellOfferFor(yes));

        User ace = makeUser("Ace", endowment);
        Quantity aceQuant = m.buyWithCostLimit(no, ninetyNineCents, purchaseAmount, ace);
        assertTrue(aceQuant.isPositive());
        assertQEquals(remainingFunds, ace.cashOnHand());
        assertQEquals(remainingFunds, janet.cashOnHand());
    }

    public void testComboBuyFromBookWithCostLimit() throws DuplicateOrderException {
        final int purchaseAmount = 500;

        SecureUser chris = makeSecureUser("Chris", endowment);
        Quantity offerQuantity = q(50);
        m.limitOrder(yes, Price.dollarPrice(20), offerQuantity, chris);
        m.marketOrder(yes, Price.dollarPrice(30), Quantity.ONE, chris);

        Quantity janetQuant = m.buyWithCostLimit(no, ninetyNineCents, q(purchaseAmount), janet);
        assertTrue("Should buy some coupons when a dollar limit is specified.", janetQuant.isPositive());
        assertTrue(janet.getAccounts().couponCount(no).isPositive());
        assertTrue(janet.cashOnHand().compareTo(q(endowment - purchaseAmount)) >= 0);
        assertEquals("", janet.getWarningsHTML());
        assertQEquals(20, m.getBook().bestBuyOfferFor(yes));
        Quantity q = m.getBook().bestQuantity(yes);
        assertTrue("Should partially satisfy second order: " + q +" < 50", q.compareTo(offerQuantity) < 0);
    }

    public void testMarketOutcomes() {
        Outcome open = Outcome.newOpen(true);
        assertTrue(open.isContinuous());
        assertTrue(open.isOpen());
        try {
            open.outcome(yes);
            fail();
        } catch (RuntimeException r) {
            // ignore;
        }

        Outcome determined = Outcome.newSingle(yes);
        assertFalse(determined.isContinuous());
        assertFalse(determined.isOpen());
        assertQEquals(1.0, determined.outcome(yes));
        assertQEquals(0.0, determined.outcome(no));
        try {
            determined.outcome(null);
            fail();
        } catch (RuntimeException r) {
            // ignore;
        }

        HashMap<Position, Probability> outcomes = new HashMap<Position, Probability>();
        outcomes.put(yes, new Probability(.54));
        outcomes.put(no, new Probability(.46));

        MultiClaim multiClaim = MultiClaim.makeClaim("multi", owner, "a multi-outcome claim", new String[] { "one", "two", "three" } );
        Position one = multiClaim.lookupPosition("one");
        Position two = multiClaim.lookupPosition("two");
        Position three = multiClaim.lookupPosition("three");

        Outcome multi = Outcome.newSingle(two);
        assertFalse(multi.isContinuous());
        assertFalse(multi.isOpen());
        assertQEquals(1.0, multi.outcome(two));
        assertQEquals(0.0, multi.outcome(one));
        assertQEquals(0.0, multi.outcome(three));
    }
}
