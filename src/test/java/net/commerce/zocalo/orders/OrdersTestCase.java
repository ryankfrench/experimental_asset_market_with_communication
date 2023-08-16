package net.commerce.zocalo.orders;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Coupons;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;
import net.commerce.zocalo.ajax.events.TimingUpdater;

import java.util.Map;

public abstract class OrdersTestCase extends PersistentTestHelper {
    protected BinaryClaim claim;
    protected final CashBank rootBank = new CashBank("cash");
    protected Position yes;
    protected Position no;
    protected SortedOrders yesOrders;
    protected SortedOrders noOrders;
    protected Book book;
    protected Market market;
    protected SecureUser owner;
    protected PriceActionAppender chart;
    private Quantity q100000 = new Quantity("100000");

    protected void setUp() throws Exception {
        super.setUp();
        Log4JHelper.getInstance();
        HibernateTestUtil.resetSessionFactory();

        setupBayeux();
        owner = new SecureUser("joe", rootBank.makeFunds(q100000), "joe's pwd", "joe@example.com");
        claim = BinaryClaim.makeClaim("silly", owner, "a silly claim");
        market = BinaryMarket.make(owner, claim, rootBank.noFunds());
        book = market.getBook();
        chart = PriceActionAppender.make(mockBayeux, book, makeUpdaterCallback());
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        yesOrders = book.getOffers(yes);
        noOrders = book.getOffers(no);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        claim = null;
        market = null;
        yesOrders = null;
        noOrders = null;
        yes = null;
        no = null;
        book = null;
        owner = null;
        chart = null;
    }

    protected User makeUser(String name) {
        return rootBank.makeEndowedUser(name, q100000);
    }

    protected User makeUser(String name, Quantity initialCash) {
        return rootBank.makeEndowedUser(name, initialCash);
    }

    protected User makeUser(String name, double initialCash) {
        return rootBank.makeEndowedUser(name, new Quantity(initialCash));
    }

    protected SecureUser makeSecureUser(String name) {
        return makeSecureUser(name, q100000);
    }

    protected SecureUser makeSecureUser(String name, Quantity initialCash) {
        return new SecureUser(name, rootBank.makeFunds(initialCash), "", "");
    }

    protected SecureUser makeSecureUser(String name, double initialCash) {
        return new SecureUser(name, rootBank.makeFunds(new Quantity(initialCash)), "", "");
    }

    // TODO: This method is almost identical to Book:useFundsToPurchaseNewCoupons()
    protected void assignCouponsTo(Quantity amount, User yesRecipient, User noRecipient) {
        Coupons[] couponArray = market.printNewCouponSets(amount, rootBank.makeFunds(amount));
        for (int i = 0; i < couponArray.length; i++) {
            Coupons coupons = couponArray[i];
            if (coupons.getPosition() == yes) {
                yesRecipient.endow(coupons);
            } else {
                noRecipient.endow(coupons);
            }
        }
    }

    static public TimingUpdater makeUpdaterCallback() {
        final net.commerce.zocalo.experiment.Session session = SessionSingleton.getSession();
        return new TimingUpdater() {
            public void addTimingInfo(Map e) {
                if (session == null) {
                    e.put("round", "0");
                } else {
                    e.put("round", Integer.toString(session.getCurrentRound()));
                    e.put("timeRemaining", session.timeRemaining());
                }
            }
        };
    }

    static public void makeLimitOrder(Market market, Position pos, double price, int quantity, User user) throws DuplicateOrderException {
        market.limitOrder(pos, Price.dollarPrice(price), q(quantity), user);
    }
}
